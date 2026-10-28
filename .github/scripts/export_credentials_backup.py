"""Explicit manual export of owner credentials into an encrypted ZIP in a private repo."""
import base64
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
from urllib.error import HTTPError
from urllib.request import Request, urlopen

OWNER = "nikhilmenghani"
REPOSITORY = "NikGappsCredentialsBackup"
NAMES = ("AUTH", "KEYSTORE", "KEYSTORE_PASSWORD", "NIKGAPPS_CLIENT_ID",
         "NIKGAPPS_CLIENT_SECRET", "POSTHOG_API_KEY", "POSTHOG_HOST")


def api(token, path, method="GET", body=None, allow_missing=False):
    request = Request("https://api.github.com" + path, method=method,
                      data=None if body is None else json.dumps(body).encode(),
                      headers={"Authorization": "Bearer " + token,
                               "Accept": "application/vnd.github+json",
                               "X-GitHub-Api-Version": "2022-11-28",
                               "Content-Type": "application/json"})
    try:
        with urlopen(request, timeout=60) as response:
            return json.load(response)
    except HTTPError as error:
        if allow_missing and error.code == 404:
            return None
        raise RuntimeError(f"GitHub request failed (HTTP {error.code}); response omitted") from None
    except OSError:
        raise RuntimeError("GitHub request failed; sensitive diagnostics omitted") from None


def ensure_private_repository(token):
    user = api(token, "/user")
    if user.get("login", "").lower() != OWNER.lower():
        raise RuntimeError("AUTH must authenticate as the expected repository owner")
    path = f"/repos/{OWNER}/{REPOSITORY}"
    repository = api(token, path, allow_missing=True)
    if repository is None:
        repository = api(token, "/user/repos", "POST", {
            "name": REPOSITORY, "private": True, "auto_init": True,
            "description": "Encrypted personal credential backups"})
    if (repository.get("private") is not True or repository.get("fork") is True or
            repository.get("archived") is True or
            repository.get("owner", {}).get("login", "").lower() != OWNER.lower() or
            repository.get("name") != REPOSITORY):
        raise RuntimeError("Refusing to upload: destination must be the owner's private, active, non-fork repository")
    return repository


def encrypt_zip(values, password, directory, executable):
    source = directory / "credentials.txt"
    archive = directory / "credentials.zip"
    try:
        with source.open("x", encoding="utf-8", newline="\n") as stream:
            os.chmod(source, 0o600)
            for name, value in values.items():
                stream.write(f"===== {name} =====\n")
                stream.write(value)
                stream.write(f"\n===== END {name} =====\n\n")
        # Pass arguments structurally. Suppress 7-Zip output and never enable shell tracing.
        child_env = {key: value for key, value in os.environ.items()
                     if key not in NAMES and key != "CREDENTIAL_BACKUP_PASSWORD"}
        args = [executable, "a", "-tzip", "-mem=AES256", "-p" + password,
                str(archive), source.name]
        result = subprocess.run(args, cwd=directory, env=child_env,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode or not archive.is_file():
            raise RuntimeError("ZIP encryption failed; sensitive diagnostics omitted")
        result = subprocess.run([executable, "t", "-p" + password, str(archive)],
                                cwd=directory, env=child_env,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode:
            raise RuntimeError("Encrypted ZIP validation failed")
        return archive.read_bytes()
    finally:
        source.unlink(missing_ok=True)


def main():
    values = {name: os.environ.get(name, "") for name in NAMES}
    missing = [name for name in NAMES if not values[name]]
    if missing:
        raise ValueError("Missing required secrets: " + ", ".join(missing))
    run_id, attempt = os.environ["GITHUB_RUN_ID"], os.environ["GITHUB_RUN_ATTEMPT"]
    if not re.fullmatch(r"[0-9]+", run_id) or not re.fullmatch(r"[0-9]+", attempt):
        raise ValueError("Invalid workflow run identity")
    password = os.environ.get("CREDENTIAL_BACKUP_PASSWORD", "")
    if len(password) < 24 or password.lower() == OWNER.lower():
        raise ValueError("Set CREDENTIAL_BACKUP_PASSWORD to the generated strong password (at least 24 characters)")
    executable = shutil.which("7z") or shutil.which("7zz")
    if not executable:
        raise RuntimeError("7-Zip is required on this runner")
    with tempfile.TemporaryDirectory(prefix="nikgapps-backup-", dir=os.environ.get("RUNNER_TEMP")) as temporary:
        directory = Path(temporary)
        os.chmod(directory, 0o700)
        archive = encrypt_zip(values, password, directory, executable)
        token = values["AUTH"]
        repository = ensure_private_repository(token)
        path = f"/repos/{OWNER}/{REPOSITORY}/contents/backups/credentials-{run_id}-{attempt}.zip"
        result = api(token, path, "PUT", {
            "message": f"Add encrypted backup from run {run_id}, attempt {attempt}",
            "branch": repository["default_branch"],
            "content": base64.b64encode(archive).decode()})
        if not result.get("commit", {}).get("sha"):
            raise RuntimeError("GitHub did not confirm the backup commit")
        print(f"Encrypted backup committed to {OWNER}/{REPOSITORY}; no plaintext was uploaded.")


if __name__ == "__main__":
    main()
