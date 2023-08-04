package com.peihua.simple

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window


/**
 * 全屏主题透明状态栏
 */
inline var Activity?.isTranslucentTheme: Boolean
    get() = this?.window.isTranslucentTheme
    set(value) {
        this?.window.isTranslucentTheme = value
    }

/**
 * 全屏主题透明状态栏
 */
inline var Window?.isTranslucentTheme: Boolean
    get() = if (this != null) decorView.isTranslucentLayoutStable else false
    set(value) {
        if (this != null) {
            decorView.isTranslucentLayoutStable = value
            statusBarColor = Color.TRANSPARENT
        }
    }

/**
 * 全屏高亮主题状态栏
 */
inline var Activity?.isTranslucentLightTheme: Boolean
    get() = this?.window.isTranslucentLightTheme
    set(value) {
        this?.window.isTranslucentLightTheme = value
    }
inline var Window?.isTranslucentLightTheme: Boolean
    get() = if (this != null) decorView.isTranslucentLightLayoutStable else false
    set(value) {
        if (this != null) {
            decorView.isTranslucentLightLayoutStable = value
            statusBarColor = Color.TRANSPARENT
        }
    }
inline var View.isTranslucentLayoutStable: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        (systemUiVisibility and (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)) != 0
    } else {
        (systemUiVisibility and (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)) != 0
    }
    set(value) {
        systemUiVisibility = if (value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                unsetSystemUiFlag(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
            } else {
                unsetSystemUiFlag(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)

            }
        }
    }

inline var View.isTranslucentLightLayoutStable: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        (systemUiVisibility and (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)) != 0
    } else {
        (systemUiVisibility and (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)) != 0
    }
    set(value) {
        systemUiVisibility = if (value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                unsetSystemUiFlag(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                )
            } else {
                unsetSystemUiFlag(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)

            }
        }
    }
fun View.unsetSystemUiFlag(systemUiFlag: Int): Int {
    return (systemUiVisibility and systemUiFlag.inv())
}