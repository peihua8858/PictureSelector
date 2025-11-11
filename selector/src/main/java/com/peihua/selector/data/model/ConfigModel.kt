package com.peihua.selector.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class ConfigModel(
    var isShowGif: Boolean = true,
    var isShowWebp: Boolean = true,
    var isShowBmp: Boolean = true,
    var isPageSyncAsCount: Boolean = false,
    var sortOrder: String = "",
    var pageSize: Int = 60,
    var filterVideoMinSecond: Long = 0,
    var filterVideoMaxSecond: Long = 0,
    var filterMinFileSize: Long = 0,
    var filterMaxFileSize: Long = 0,
    var queryOnlyList: MutableList<String> = mutableListOf()
) : Parcelable {
    companion object {
        @JvmStatic
        fun default(): ConfigModel {
            return ConfigModel()
        }
    }
}
