@file:JvmName("CursorUtils")
@file:JvmMultifileClass
package com.peihua.selector.util

import android.database.Cursor

/**
 * Get the string from the [this] cursor to be parsed with the [columnName] column name of the value.
 *
 * @return the string value from the [this], or `null` when [this] doesn't
 * contain [columnName]
 */
fun Cursor.getCursorString(columnName: String): String? {
    val index = getColumnIndex(columnName)
    return if (index != -1) getString(index) else null
}

/**
 * Get the long value from the [this] cursor to be parsed with the [columnName] column name of the value.
 *
 * @return the long value from the [this], or -1 when [this] doesn't contain
 * [columnName]
 */
fun Cursor.getCursorLong(columnName: String): Long {
    val index = getColumnIndex(columnName)
    if (index == -1) {
        return -1
    }
    val value = getString(index) ?: return -1
    return try {
        value.toLong()
    } catch (e: NumberFormatException) {
        -1
    }
}

/**
 * Get the int value from the [this] cursor to be parsed with the [columnName] column name of the value.
 *
 * @return the int value from the [this], or 0 when [this] doesn't contain
 * [columnName]
 */
fun Cursor.getCursorInt(columnName: String): Int {
    val index = getColumnIndex(columnName)
    return if (index != -1) getInt(index) else 0
}