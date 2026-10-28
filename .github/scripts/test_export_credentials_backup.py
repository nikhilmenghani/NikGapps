import tempfile
from pathlib import Path
import unittest
from unittest.mock import patch

import export_credentials_backup as backup


class ExportBackupTest(unittest.TestCase):
    def test_text_contains_readable_names_and_exact_multiline_values(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            values = {name: "fixture-value-" + name + "\nsecond line\n" for name in backup.NAMES}
            captured = []
            def fake_run(args, **kwargs):
                captured.append((directory / "credentials.txt").read_bytes())
                (directory / "credentials.zip").write_bytes(b"fixture")
                return type("Result", (), {"returncode": 0})()
            with patch.object(backup.subprocess, "run", side_effect=fake_run):
                backup.encrypt_zip(values, "fixture password", directory, "7z")
            expected = "".join(f"===== {name} =====\n{value}\n===== END {name} =====\n\n" for name, value in values.items())
            self.assertEqual(expected.encode(), captured[0])
            self.assertFalse((directory / "credentials.txt").exists())

    def repository(self, **changes):
        return dict(name=backup.REPOSITORY, owner={"login": backup.OWNER},
                    private=True, fork=False, archived=False, default_branch="main", **changes)

    def test_existing_public_repo_is_never_accepted(self):
        repo = self.repository()
        repo["private"] = False
        with patch.object(backup, "api", side_effect=[{"login": backup.OWNER}, repo]) as api:
            with self.assertRaises(RuntimeError):
                backup.ensure_private_repository("fixture")
            self.assertFalse(any(call.args[2:] for call in api.call_args_list))

    def test_creates_only_private_repository(self):
        with patch.object(backup, "api", side_effect=[{"login": backup.OWNER}, None, self.repository()]) as api:
            backup.ensure_private_repository("fixture")
            self.assertTrue(api.call_args.args[3]["private"])
            self.assertEqual("/user/repos", api.call_args.args[1])

    def test_wrong_authenticated_owner_cannot_create_repository(self):
        with patch.object(backup, "api", return_value={"login": "someone-else"}) as api:
            with self.assertRaises(RuntimeError):
                backup.ensure_private_repository("fixture")
            self.assertEqual(1, api.call_count)

    def test_plaintext_is_cleaned_on_failed_encryption(self):
        result = type("Result", (), {"returncode": 1})()
        with tempfile.TemporaryDirectory() as temporary:
            with patch.object(backup.subprocess, "run", return_value=result):
                with self.assertRaises(RuntimeError):
                    backup.encrypt_zip({"AUTH": "fixture"}, "password", Path(temporary), "7z")
            self.assertFalse((Path(temporary) / "credentials.txt").exists())

    def test_zip_uses_aes_and_arguments_not_a_shell(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            calls = []
            def fake_run(args, **kwargs):
                calls.append((args, kwargs))
                (directory / "credentials.zip").write_bytes(b"encrypted-fixture")
                return type("Result", (), {"returncode": 0})()
            with patch.object(backup.subprocess, "run", side_effect=fake_run):
                self.assertEqual(b"encrypted-fixture", backup.encrypt_zip({"AUTH": "fixture"}, "spaces ' $()", directory, "7z"))
            self.assertIn("-mem=AES256", calls[0][0])
            self.assertEqual("credentials.txt", calls[0][0][-1])
            self.assertEqual(7, len(calls[0][0]))
            self.assertIn("-pspaces ' $()", calls[0][0])
            self.assertTrue(all(not kwargs.get("shell") for _, kwargs in calls))
            self.assertFalse((directory / "credentials.txt").exists())


if __name__ == "__main__":
    unittest.main()
