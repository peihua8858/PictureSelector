package com.peihua8858.selector.crop

import androidx.annotation.IntDef

@IntDef(GestureTypes.NONE, GestureTypes.SCALE, GestureTypes.ROTATE, GestureTypes.ALL)
@Retention(AnnotationRetention.SOURCE)
annotation class GestureTypes {
    companion object {
        const val NONE = 0
        const val SCALE = 1
        const val ROTATE = 2
        const val ALL = 3
    }
}