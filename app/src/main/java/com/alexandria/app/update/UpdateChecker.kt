package com.alexandria.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String
)

object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/siriusra/Alexandria/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "GitHub API returned ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "")
            val publishedAt = json.optString("published_at", "")

            val remoteVersionCode = extractVersionCode(tagName)
            if (remoteVersionCode == null) {
                Log.w(TAG, "Could not parse version code from tag: $tagName")
                return@withContext null
            }

            if (remoteVersionCode <= currentVersionCode) {
                Log.i(TAG, "App is up to date (current=$currentVersionCode, remote=$remoteVersionCode)")
                return@withContext null
            }

            val assets = json.getJSONArray("assets")
            val apkAsset = findApkAsset(assets)

            if (apkAsset == null) {
                Log.w(TAG, "No APK found in release assets")
                return@withContext null
            }

            val downloadUrl = apkAsset.getString("browser_download_url")

            UpdateInfo(
                versionName = tagName.removePrefix("v"),
                versionCode = remoteVersionCode,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes,
                publishedAt = publishedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update", e)
            null
        }
    }

    private fun extractVersionCode(tagName: String): Int? {
        val plusIndex = tagName.lastIndexOf('+')
        return if (plusIndex > 0) {
            tagName.substring(plusIndex + 1).toIntOrNull()
        } else {
            null
        }
    }

    private fun findApkAsset(assets: JSONArray): JSONObject? {
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.getString("name")
            if (name.endsWith(".apk")) {
                return asset
            }
        }
        return null
    }

    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: ${response.code}")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()

            val cacheDir = File(context.cacheDir, "updates")
            cacheDir.mkdirs()
            val apkFile = File(cacheDir, "alexandria-update.apk")

            apkFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            onProgress(totalBytesRead.toFloat() / contentLength)
                        }
                    }
                }
            }

            Log.i(TAG, "Download complete: ${apkFile.absolutePath} (${apkFile.length()} bytes)")

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update", e)
            false
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(intent)
    }

    fun cleanupOldDownloads(context: Context) {
        val cacheDir = File(context.cacheDir, "updates")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }
}
