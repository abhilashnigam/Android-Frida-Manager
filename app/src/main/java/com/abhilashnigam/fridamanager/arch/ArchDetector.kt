package com.abhilashnigam.fridamanager.arch

import android.os.Build

object ArchDetector {

    class UnsupportedArchException(abi: String) :
        Exception("Unsupported or unrecognized ABI: $abi")

    /** Returns frida's asset-naming token, e.g. "arm64", "arm", "x86_64", "x86". */
    fun detectFridaArch(): String {
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull()
            ?: throw UnsupportedArchException("none reported")

        return when (primaryAbi) {
            "arm64-v8a" -> "arm64"
            "armeabi-v7a", "armeabi" -> "arm"
            "x86_64" -> "x86_64"
            "x86" -> "x86"
            else -> throw UnsupportedArchException(primaryAbi)
        }
    }

    /** True when the device is (almost certainly) an emulator, for diagnostics/logging only. */
    fun isLikelyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        return fingerprint.startsWith("generic") ||
                fingerprint.contains("emulator") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.PRODUCT.contains("sdk")
    }
}