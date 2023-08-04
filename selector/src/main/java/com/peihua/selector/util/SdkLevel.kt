@file:JvmName("SdkLevel")
@file:JvmMultifileClass
package com.peihua.selector.util

import android.os.Build
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.SDK_INT


/**
 * [Build.VERSION.SDK_INT]>= Android 13
 */
inline val isAtLeastTiramisu: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * [Build.VERSION.SDK_INT]>= Android 9
 */
inline val isAtLeastOreo: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

/**
 * [Build.VERSION.SDK_INT]>= Android 9
 */
inline val isAtLeastPie: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
inline val isUpsideDownCake: Boolean
    get() = Build.VERSION.SDK_INT >= 34

inline val isAtLeastM: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
inline val isAtLeastO: Boolean
    get() {
        return SDK_INT >= Build.VERSION_CODES.O
    }
inline val isAtLeastQ: Boolean
    get() {
        return SDK_INT >= Build.VERSION_CODES.Q
    }
/** Checks if the device is running on a release version of Android R or newer.  */
inline val isAtLeastR: Boolean
    get() {
        return SDK_INT >= 30
    }

/** Checks if the device is running on a release version of Android S or newer.  */
inline val isAtLeastS: Boolean
    get() {
        return SDK_INT >= 31
    }

/** Checks if the device is running on a release version of Android S_V2 or newer  */
inline val isAtLeastSv2: Boolean
    get() {
        return SDK_INT >= 32
    }

/** Checks if the device is running on a release version of Android Tiramisu or newer  */
inline val isAtLeastT: Boolean
    get() {
        return SDK_INT >= 33
    }

/** Checks if the device is running on a release version of Android UpsideDownCake or newer  */
inline val isAtLeastU: Boolean
    get() {
        return SDK_INT >= 34 || SDK_INT == 33 && isAtLeastPreReleaseCodename("UpsideDownCake")
    }

/** Checks if the device is running on a pre-release version of Android V or newer  */
inline val isAtLeastV: Boolean
    get() {
        return SDK_INT >= 34 && isAtLeastPreReleaseCodename("VanillaIceCream")
    }

fun isAtLeastPreReleaseCodename(codename: String): Boolean {
    // Special case "REL", which means the build is not a pre-release build.
    return if ("REL" == CODENAME) {
        false
    } else CODENAME >= codename

    // Otherwise lexically compare them. Return true if the build codename is equal to or
    // greater than the requested codename.
}