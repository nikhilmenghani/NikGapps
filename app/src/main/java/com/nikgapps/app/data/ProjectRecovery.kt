package com.nikgapps.app.data

fun <T, R> recoverRecords(records: List<T>, decode: (T) -> R, onFailure: (T, Exception) -> Unit): List<R> =
    records.mapNotNull { record -> try { decode(record) } catch (error: Exception) { onFailure(record, error); null } }
