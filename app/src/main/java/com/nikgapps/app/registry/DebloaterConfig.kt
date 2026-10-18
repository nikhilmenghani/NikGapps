package com.nikgapps.app.registry

/** Exact app directories or files inside them; never arbitrary system files or partition roots. */
object DebloaterConfig {
    const val MAX_BYTES = 16384
    const val MAX_TARGETS = 64
    const val CONFIG_MARKER = "# NikGapps debloater starts here"
    private val name = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")
    private val path = Regex("/(?:system(?:/system|/product|/system_ext)?|product|system_ext|system_root/system(?:/product|/system_ext)?)/(?:app|priv-app)/(.+)")
    private const val HEADER = "# Removed when this ZIP is flashed, not while building.\n"
    private val protected = setOf("android", "system", "product", "system_ext", "app", "priv-app", "etc",
        "bin", "lib", "lib64", "framework", "systemui", "settings", "settingsprovider",
        "permissioncontroller", "googlepermissioncontroller", "packageinstaller", "googlepackageinstaller",
        "packageinstallerlegacy", "phonesky", "gmscore", "prebuiltgmscore", "google_services_framework",
        "googleservicesframework", "telecom", "telephonyprovider", "providers", "vold", "init")

    fun add(targets: List<String>, input: String, conflicts: Set<String> = emptySet()): List<String> {
        require(input.isNotBlank()) { "Enter a removal target" }
        require('\n' !in input && '\r' !in input) { "Add one entry at a time" }
        return validate(targets + input.trim(), conflicts)
    }

    fun parse(text: String): List<String> {
        require(text.toByteArray().size <= MAX_BYTES) { "Debloater config is too large (maximum 16 KiB)" }
        val targets = text.removePrefix("\uFEFF").lineSequence().map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith('#') }.toList()
        return validate(targets)
    }

    fun validate(targets: List<String>, conflicts: Set<String> = emptySet()): List<String> {
        require(targets.size <= MAX_TARGETS) { "Use at most $MAX_TARGETS removal targets" }
        require(HEADER.toByteArray().size + targets.sumOf { it.toByteArray().size + 1 } <= MAX_BYTES) {
            "Debloater config exceeds 16 KiB"
        }
        val seen = mutableSetOf<String>()
        targets.forEach { target ->
            require(target.length <= 256 && !target.contains("..")) { "Unsafe removal target: $target" }
            val relative = if (target.startsWith('/')) path.matchEntire(target)?.groupValues?.get(1)
                ?: throw IllegalArgumentException("Use a supported app directory or a file inside it: $target") else target
            val segments = relative.split('/')
            require((target.startsWith('/') || segments.size == 1) && segments.all { name.matches(it) }) {
                "Use an exact name; spaces, wildcards and shell syntax are not allowed"
            }
            val folder = segments.first()
            val key = folder.lowercase(java.util.Locale.ROOT).removeSuffix(".apk")
            require(key !in protected && !key.startsWith("framework") &&
                !key.startsWith("com.android.") && !key.startsWith("com.google.android.permissioncontroller")) {
                "Protected system component: $folder"
            }
            require(seen.add(target)) { "Duplicate removal target: $target" }
            require(conflicts.none { it.equals(folder, ignoreCase = true) || it.lowercase(java.util.Locale.ROOT).removeSuffix(".apk") == key }) {
                "$folder conflicts with a package being installed or a stock app marked to keep"
            }
        }
        return targets.toList()
    }

    fun render(targets: List<String>): String = HEADER +
        validate(targets).joinToString("\n", postfix = if (targets.isEmpty()) "" else "\n")

    fun configWithoutTargets(config: String): String = config.lineSequence()
        .takeWhile { !it.trimStart().startsWith(CONFIG_MARKER) }.joinToString("\n").trimEnd() + "\n"

    fun embeddedTargets(config: String): List<String> = parse(config.lineSequence()
        .dropWhile { !it.trimStart().startsWith(CONFIG_MARKER) }.drop(1).joinToString("\n"))

    /** Same trailing section understood by the Python NikGappsConfig reader. */
    fun withEmbeddedTargets(config: String, targets: List<String>): String = configWithoutTargets(config) +
        if (targets.isEmpty()) "" else "\n$CONFIG_MARKER\n" + validate(targets).joinToString("\n", postfix = "\n")

    fun conflicts(packages: List<CatalogPackage>, kept: Set<String> = emptySet()): Set<String> = buildSet {
        packages.forEach { pkg ->
            add(pkg.name); add(pkg.id)
            pkg.versions.values.forEach { version ->
                version.packageName?.let(::add)
                version.apk?.path?.substringBeforeLast('/')?.substringAfterLast('/')?.let(::add)
            }
            if (pkg.id in kept) {
                val stock = mapOf("google_clock" to "DeskClock", "google_contacts" to "Contacts",
                    "google_dialer" to "Dialer", "google_calendar" to "Calendar", "gboard" to "LatinIME",
                    "google_messages" to "Messaging", "google_camera" to "Camera", "google_chrome" to "Browser")
                stock[pkg.id]?.let(::add)
            }
        }
    }
}
