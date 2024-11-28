package com.nikgapps.app.utils.deviceinfo

import java.io.BufferedReader
import java.io.InputStreamReader


fun hasDynamicPartitions(): Boolean {
    return try {
        val process = Runtime.getRuntime().exec("getprop ro.boot.dynamic_partitions")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val result = reader.readLine()
        process.waitFor()
        result == "true"
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun isABDevice(): Boolean {
    return try {
        val process = Runtime.getRuntime().exec("getprop ro.build.ab_update")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val result = reader.readLine()
        process.waitFor()
        result == "true"
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun getActiveSlot(): String {
    return try {
        val process = Runtime.getRuntime().exec("getprop ro.boot.slot_suffix")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val result = reader.readLine()
        process.waitFor()
        result
    } catch (e: Exception) {
        e.printStackTrace()
        "unknown"
    }
}