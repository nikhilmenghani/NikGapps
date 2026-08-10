package com.nikgapps.app.registry

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class RegistryCacheTest {
    @Test fun clearsOnlyRegistryBuildCache() {
        val root = Files.createTempDirectory("registry-clear").toFile()
        val cached = listOf("nikgapps/metadata/catalog.json", "nikgapps/packages/a.zip",
            "nikgapps/builder-assets/hash", "zip-builds/output.part")
        cached.forEach { path -> root.resolve(path).apply { requireNotNull(parentFile).mkdirs(); writeText("cached") } }
        val preserved = root.resolve("unrelated/keep.txt").apply { requireNotNull(parentFile).mkdirs(); writeText("keep") }

        val result = RegistryCache.clear(root)

        assertEquals(cached.size, result.files)
        cached.forEach { assertFalse(root.resolve(it).exists()) }
        assertEquals("keep", preserved.readText())
    }
}
