package com.nikgapps.app.registry

import org.junit.Assert.*
import org.junit.Test

class ConfigTemplateMetadataTest {
    @Test fun templateSelectionUsesTheRequestedAndroidVersion() {
        val index = """{"schemaVersion":1,"templates":{
          "16":{"androidVersion":"16","version":"38","template":{"url":"https://example.test/16","sha256":"a","size":20}},
          "17":{"androidVersion":"17","version":"40","template":{"url":"https://example.test/17","sha256":"b","size":30}}}}"""
        assertEquals("38", configTemplateAsset(index, "16").configVersion)
        assertEquals("https://example.test/17", configTemplateAsset(index, "17").url)
        assertEquals("40", configTemplateAsset(index, "17").configVersion)
        assertThrows(MetadataException::class.java) { configTemplateAsset(index, "15") }
    }
}
