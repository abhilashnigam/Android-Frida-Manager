package com.abhilashnigam.fridamanager.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import com.abhilashnigam.fridamanager.model.FridaAsset
import com.abhilashnigam.fridamanager.model.FridaRelease

private const val RELEASES_URL = "https://api.github.com/repos/frida/frida/releases?per_page=30"

class GitHubReleaseApi(private val client: OkHttpClient = OkHttpClient()) {

    suspend fun fetchReleases(): List<FridaRelease>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(RELEASES_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                parseReleases(body)
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun parseReleases(json: String): List<FridaRelease> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val assetsJson = obj.getJSONArray("assets")
            val assets = (0 until assetsJson.length()).map { j ->
                val a = assetsJson.getJSONObject(j)
                FridaAsset(
                    name = a.getString("name"),
                    downloadUrl = a.getString("browser_download_url"),
                    sizeBytes = a.getLong("size")
                )
            }
            FridaRelease(
                tagName = obj.getString("tag_name"),
                publishedAt = obj.getString("published_at"),
                assets = assets
            )
        }
    }
}