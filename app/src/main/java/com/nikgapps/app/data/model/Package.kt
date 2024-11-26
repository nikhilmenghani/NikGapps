package com.nikgapps.app.data.model

data class Package(
    val packageName: String = "",
    val partition: String = "",
    val title: String = "",
    val packageTitle: String = "",
    val appType: String = "",
    val fileList: List<String> = emptyList(),
    val overlayList: List<String> = emptyList(),
    val otherFilesList: List<String> = emptyList(),
    val removeAospAppsList: List<String> = emptyList(),
) {
    fun addFile(fileName: String): Package {
        val updatedFileList = fileList.toMutableList().apply { add(fileName) }
        return copy(fileList = updatedFileList)
    }
}