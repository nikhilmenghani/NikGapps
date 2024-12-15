package com.nikgapps.app.utils.managers

import android.content.Context
import com.nikgapps.R
import java.lang.reflect.Field

class ResourceManager(private val context: Context) {
    private val resourcesMap: MutableMap<String, Int> = mutableMapOf()

    init {
        loadRawResources()
    }

    private fun loadRawResources() {
        val rawClass: Class<*> = R.raw::class.java
        val fields: Array<Field> = rawClass.declaredFields
        for (field in fields) {
            try {
                val resourceName = field.name
                val resourceId = field.getInt(null)
                resourcesMap[resourceName] = resourceId
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }

    fun getResourceId(resourceName: String): Int? {
        return resourcesMap[resourceName]
    }

    fun getResourceContent(resourceName: String): String? {
        val resourceId = getResourceId(resourceName) ?: return null
        val inputStream = context.resources.openRawResource(resourceId)
        return inputStream.bufferedReader().use { it.readText() }
    }

    fun getScript(resourceName: String): String {
        return getResourceContent(resourceName).toString()
    }
}