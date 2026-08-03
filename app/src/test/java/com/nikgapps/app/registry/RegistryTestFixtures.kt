package com.nikgapps.app.registry

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object RegistryTestFixtures {
    fun catalog(dependency: String = "", cycle: Boolean = false) = """{
      "schemaVersion":1,"updatedAt":"2026-08-02T00:00:00Z","androidVersion":"16","platformApi":36,
      "architecture":"arm64-v8a","packages":[
      ${pkg("gms_core", false, true, if (cycle) "{\"id\":\"support_common\"}" else dependency,
        "{\"stable\":\"s\",\"beta\":\"b\",\"canary\":\"c\"}")},
      ${pkg("support_common", true, false, if (cycle) "{\"id\":\"gms_core\"}" else "", "{\"stable\":\"s\"}")},
      ${pkg("support_standard", true, false, "", "{\"stable\":\"s\"}")},
      ${pkg("support_go", true, false, "", "{\"stable\":\"s\"}")}
      ]} """
    private fun pkg(id: String, internal: Boolean, selectable: Boolean, deps: String, channels: String) = """
      {"id":"$id","name":"$id","selectable":$selectable,"internal":$internal,"dependencies":[${deps}],
       "channels":$channels,"versions":{
       "s":${version("stable")},"b":${version("beta")},"c":${version("canary")}}}
    """.trimIndent()
    private fun version(name: String) = """{"versionName":"$name","versionCode":1,"packageName":"test.$name",
      "android":{"minApi":29,"targetApi":36,"maxApi":36},"architectures":["arm64-v8a"],"defaultPartition":"product",
      "apk":{"path":"priv-app/Test/Test.apk","replaceable":true},"artifact":{"url":"https://example.test/$name.zip",
      "sha256":"${"a".repeat(64)}","size":1},"contentSha256":"x"}"""
    fun appSets() = """{"schemaVersion":1,"appSets":[
      {"id":"core","name":"Core","packages":["gms_core"],"resolvedPackages":["gms_core"],"legacyPackageNames":{"gms_core":"GmsCore"}},
      {"id":"core_go","name":"CoreGo","packages":["gms_core"],"resolvedPackages":["gms_core"],"legacyPackageNames":{"gms_core":"GmsCore"}}]}"""
    fun artifact(directory: File, id: String = "gms_core"): Pair<File, String> {
        val payload = "apk".toByteArray(); val payloadHash = hash(payload)
        val metadata = """{"schemaVersion":1,"id":"$id","packageName":"test.app","defaultPartition":"product",
          "apk":{"path":"priv-app/Test/Test.apk","replaceable":true},"files":[{"path":"priv-app/Test/Test.apk",
          "archivePath":"___priv-app___Test/Test.apk","installPath":"product/priv-app/Test/Test.apk",
          "type":"primaryApk","sha256":"$payloadHash","size":3}],"install":{"format":"nikgapps-package-v1",
          "title":"Test","packageTitle":"GmsCore","payloadSize":3,"removeFiles":[],"removeOverlays":[],
          "privilegedPermissions":[],"cleanFlashOnly":false,"addonIndex":"09"}}"""
        val file = File(directory, "$id.zip")
        ZipOutputStream(file.outputStream()).use { z ->
            z.putNextEntry(ZipEntry("___priv-app___Test/Test.apk")); z.write(payload); z.closeEntry()
            z.putNextEntry(ZipEntry("installer.sh")); z.write("find_install_mode\n".toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("uninstaller.sh")); z.write("uninstall_package\n".toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("package.json")); z.write(metadata.toByteArray()); z.closeEntry()
        }
        return file to hash(file.readBytes())
    }
    fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
