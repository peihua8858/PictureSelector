@file:JvmName("ViewUtils")
@file:JvmMultifileClass
package com.peihua.selector.util

import android.view.View
import androidx.core.view.ViewCompat


val View.isLayoutRtl: Boolean
    get() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
    }