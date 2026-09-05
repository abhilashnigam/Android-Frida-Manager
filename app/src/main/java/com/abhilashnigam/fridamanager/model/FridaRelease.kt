package com.abhilashnigam.fridamanager.model

data class FridaAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long
)

data class FridaRelease(
    val tagName: String,
    val publishedAt: String,
    val assets: List<FridaAsset>
) {
    fun assetForArch(arch: String): FridaAsset? =
        assets.firstOrNull { it.name == "frida-server-$tagName-android-$arch.xz" }
}