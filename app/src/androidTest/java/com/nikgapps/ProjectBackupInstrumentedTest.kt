package com.nikgapps

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** All preferences are redirected to unique test-only names; personal projects are never written. */
@RunWith(AndroidJUnit4::class)
class ProjectBackupInstrumentedTest {
    @Test fun transferReplacesDestinationProjectsAndProtectsAgainstStaleConfirmation() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "backup-test-${UUID.randomUUID()}"
        fun isolated(suffix: String) = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) =
                base.getSharedPreferences("$prefix-$suffix-$name", mode)
        }
        val debug = isolated("debug")
        val release = isolated("release")
        val original = BuildProject(name = "Backup test", androidVersion = AndroidVersion.ANDROID_17,
            architecture = Architecture.ARM64, selectedAppIds = setOf("gms_core"),
            selectedPackageAppSets = mapOf("gms_core" to "core"), debloatTargets = listOf("YouTube"),
            installerSettings = mapOf("InstallPartition" to "product"), configVersion = "41")
        try {
            val source = BuildProjectRepository(debug)
            source.addProject(original)
            val file = ProjectBackups.export(source.getProjects(), BackupSource("0.80.22-debug", 80022, "debug"))
            val parsed = ProjectBackups.read(file)
            val destination = BuildProjectRepository(release)
            val existing = original.copy(id = UUID.randomUUID().toString(), name = "Keep me")
            destination.addProject(existing)
            assertEquals(1, destination.replaceFromBackup(parsed.projects, listOf(existing), false))
            assertEquals(listOf(original), BuildProjectRepository(release).getProjects())
            assertEquals(1, destination.replaceFromBackup(parsed.projects, listOf(original), false))
            assertThrows(IllegalStateException::class.java) {
                destination.replaceFromBackup(listOf(existing), emptyList(), false)
            }
            assertThrows(IllegalArgumentException::class.java) {
                destination.replaceFromBackup(emptyList(), listOf(original), false)
            }
            assertEquals(listOf(original), BuildProjectRepository(release).getProjects())
            // Explicit Elite selection must durably retain outdated definitions too.
            val outdated = original.copy(id = UUID.randomUUID().toString(), name = "Outdated backup",
                packageDefinitions = mapOf("gms_core" to "retired-definition"), configVersion = "40")
            val all = ProjectBackups.read(ProjectBackups.export(listOf(original, outdated),
                BackupSource("0.80.22-debug", 80022, "debug"))).projects
            assertEquals(2, destination.replaceFromBackup(all, listOf(original), elite = true,
                selectedIds = all.mapTo(hashSetOf()) { it.id }))
            assertEquals(all, BuildProjectRepository(release).getProjects())
            assertEquals(listOf(original), source.getProjects())
        } finally {
            for (suffix in listOf("debug", "release")) {
                base.getSharedPreferences("$prefix-$suffix-build_projects", Context.MODE_PRIVATE).edit().clear().commit()
                base.deleteSharedPreferences("$prefix-$suffix-build_projects")
            }
        }
    }
}
