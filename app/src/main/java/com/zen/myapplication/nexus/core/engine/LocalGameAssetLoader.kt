package com.zen.myapplication.nexus.core.engine

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import com.zen.myapplication.nexus.core.storage.VfsDatabase
import java.io.InputStream
import java.net.URLConnection

/**
 * Custom PathHandler for WebViewAssetLoader that reads game files directly
 * from the Android Storage Access Framework (SAF) using the VFS Database.
 */
class LocalGameAssetLoader(
    private val context: Context,
    private val rootGameFolderUri: Uri
) : WebViewAssetLoader.PathHandler {
    private val assetDao = VfsDatabase.getDatabase(context).assetDao()
    private val gameId = rootGameFolderUri.toString()

    override fun handle(path: String): WebResourceResponse? {
        try {
            val normalizedPath = path.substringBefore('?').trimStart('/').ifEmpty { "index.html" }

            // Priority 1: Check for internal Ruffle assets
            if (normalizedPath.startsWith("ruffle/")) {
                val assetPath = normalizedPath
                val inputStream = context.assets.open(assetPath)
                val mimeType = guessMimeType(normalizedPath)
                return WebResourceResponse(mimeType, encodingFor(mimeType), inputStream).apply {
                    responseHeaders = buildResponseHeaders(assetPath, mimeType)
                }
            }

            // Priority 2: Use VFS Database for O(1) resolution
            val asset = kotlinx.coroutines.runBlocking {
                assetDao.getAsset(gameId, normalizedPath.lowercase())
            } ?: return null

            val assetUriString = asset.assetUri
            val assetUri = Uri.parse(assetUriString)
            val mimeType = guessMimeType(normalizedPath)
            val inputStream: InputStream = context.contentResolver.openInputStream(assetUri) ?: return null

            return WebResourceResponse(mimeType, encodingFor(mimeType), inputStream).apply {
                responseHeaders = buildResponseHeaders(normalizedPath, mimeType)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun guessMimeType(fileName: String): String {
        val lowerName = fileName.lowercase()
        val extension = lowerName.substringAfterLast('.', missingDelimiterValue = "")

        return when (extension) {
            "gz" -> guessMimeType(lowerName.removeSuffix(".gz"))
            "br" -> guessMimeType(lowerName.removeSuffix(".br"))
            "unityweb" -> when {
                lowerName.contains(".wasm.") -> "application/wasm"
                lowerName.contains(".js.") -> "application/javascript"
                else -> "application/octet-stream"
            }
            "html", "htm" -> "text/html"
            "js", "mjs" -> "application/javascript"
            "json" -> "application/json"
            "css" -> "text/css"
            "wasm" -> "application/wasm"
            "data", "mem", "pck", "pak" -> "application/octet-stream"
            "xml" -> "application/xml"
            "txt", "log" -> "text/plain"
            "png" -> "image/png"
            "rpgmvp" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "bmp" -> "image/bmp"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "mid", "midi" -> "audio/midi"
            "ogg" -> "audio/ogg"
            "oga" -> "audio/ogg"
            "wav" -> "audio/wav"
            "opus" -> "audio/opus"
            "rpgmvm" -> "audio/mp4"
            "rpgmvo" -> "audio/ogg"
            "mp4" -> "video/mp4"
            "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "ogv" -> "video/ogg"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "swf" -> "application/x-shockwave-flash"
            else -> URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"
        }
    }

    private fun encodingFor(mimeType: String): String? {
        return when {
            mimeType.startsWith("text/") -> "UTF-8"
            mimeType == "application/javascript" -> "UTF-8"
            mimeType == "application/json" -> "UTF-8"
            mimeType == "application/xml" -> "UTF-8"
            else -> null
        }
    }

    private fun buildResponseHeaders(fileName: String, mimeType: String): Map<String, String> {
        val headers = mutableMapOf(
            "Access-Control-Allow-Origin" to "*",
            "Cross-Origin-Resource-Policy" to "same-origin",
            "X-Content-Type-Options" to "nosniff",
            "Cache-Control" to cacheControlFor(mimeType)
        )

        if (fileName.endsWith(".gz", ignoreCase = true)) {
            headers["Content-Encoding"] = "gzip"
        }
        if (fileName.endsWith(".br", ignoreCase = true)) {
            headers["Content-Encoding"] = "br"
        }

        if (mimeType == "text/html" ||
            mimeType == "application/wasm" ||
            fileName.endsWith(".wasm.gz", ignoreCase = true) ||
            fileName.endsWith(".wasm.br", ignoreCase = true) ||
            fileName.endsWith(".wasm.unityweb", ignoreCase = true)
        ) {
            headers["Cross-Origin-Embedder-Policy"] = "require-corp"
            headers["Cross-Origin-Opener-Policy"] = "same-origin"
        }

        return headers
    }

    private fun cacheControlFor(mimeType: String): String {
        return when {
            mimeType == "text/html" -> "no-cache"
            mimeType == "application/javascript" -> "no-cache"
            mimeType == "application/json" -> "no-cache"
            mimeType.startsWith("text/") -> "no-cache"
            else -> "public, max-age=3600"
        }
    }
}
