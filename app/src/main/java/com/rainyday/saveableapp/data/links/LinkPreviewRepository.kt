package com.rainyday.saveableapp.data.links

import com.rainyday.saveableapp.data.local.LinkPreviewCacheEntity
import com.rainyday.saveableapp.data.local.LinkPreviewDao
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val USER_AGENT = "Mozilla/5.0 (Android) SaveableApp-LinkPreview"
private const val MAX_HTML_BYTES = 65_536
private const val MAX_OEMBED_BYTES = 8_192
private const val CACHE_TTL_MILLIS = 30L * 24 * 60 * 60 * 1000 // 30 days

/** Title and thumbnail image URL resolved for a link, for showing a static preview card. */
data class LinkPreview(val title: String?, val imageUrl: String?)

/** Host name with a leading "www." stripped, or null if [url] can't be parsed — used as a fallback label. */
fun linkHostLabel(url: String): String? = runCatching { URL(url).host?.removePrefix("www.") }.getOrNull()

/**
 * Resolves a lightweight title + thumbnail for a URL, for a static link-preview card. Prefers a site's
 * own oEmbed endpoint — a couple of well-known ones (YouTube, Vimeo) plus whatever a page advertises via
 * `<link rel="alternate" type="application/json+oembed">` — and otherwise falls back to scraping Open
 * Graph meta tags. Results persist in [LinkPreviewDao] (with an in-memory fast path on top) so a preview
 * survives app restarts and isn't re-fetched on every scroll.
 */
class LinkPreviewRepository(private val dao: LinkPreviewDao) {
    private val json = Json { ignoreUnknownKeys = true }
    private val memoryCache = ConcurrentHashMap<String, LinkPreview>()

    suspend fun preview(url: String): LinkPreview? = withContext(Dispatchers.IO) {
        memoryCache[url]?.let { return@withContext it }

        val cached = dao.get(url)
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MILLIS) {
            val preview = LinkPreview(title = cached.title, imageUrl = cached.imageUrl)
            memoryCache[url] = preview
            return@withContext preview
        }

        val result = runCatching { fetchPreview(url) }.getOrNull()
        if (result != null) {
            memoryCache[url] = result
            dao.upsert(
                LinkPreviewCacheEntity(
                    url = url,
                    title = result.title,
                    imageUrl = result.imageUrl,
                    fetchedAt = System.currentTimeMillis()
                )
            )
        }
        result ?: cached?.let { LinkPreview(title = it.title, imageUrl = it.imageUrl) }
    }

    private fun fetchPreview(url: String): LinkPreview {
        knownOEmbedEndpoint(url)?.let { endpoint -> fetchOEmbed(endpoint)?.let { return it } }

        val html = httpGetText(url, MAX_HTML_BYTES)
            ?: return LinkPreview(title = linkHostLabel(url), imageUrl = null)

        discoverOEmbedEndpoint(html, url)?.let { endpoint -> fetchOEmbed(endpoint)?.let { return it } }

        return parseOpenGraph(html, url)
    }

    /** Hardcoded oEmbed endpoints for providers common enough to skip the discovery round-trip. */
    private fun knownOEmbedEndpoint(url: String): String? {
        val host = runCatching { URL(url).host?.lowercase() }.getOrNull() ?: return null
        val encoded = URLEncoder.encode(url, "UTF-8")
        return when {
            host.endsWith("youtube.com") || host == "youtu.be" -> "https://www.youtube.com/oembed?url=$encoded&format=json"
            host.endsWith("vimeo.com") -> "https://vimeo.com/api/oembed.json?url=$encoded"
            else -> null
        }
    }

    /** Finds an oEmbed discovery link (the standard `<link type="application/json+oembed">` tag) in [html]. */
    private fun discoverOEmbedEndpoint(html: String, baseUrl: String): String? {
        val typeThenHref = Regex(
            "<link[^>]*type=[\"']application/json\\+oembed[\"'][^>]*href=[\"'](.*?)[\"']",
            RegexOption.IGNORE_CASE
        )
        val hrefThenType = Regex(
            "<link[^>]*href=[\"'](.*?)[\"'][^>]*type=[\"']application/json\\+oembed[\"']",
            RegexOption.IGNORE_CASE
        )
        val href = typeThenHref.find(html)?.groupValues?.get(1) ?: hrefThenType.find(html)?.groupValues?.get(1)
        return href?.let { resolveUrl(baseUrl, decodeHtmlEntities(it)) }
    }

    private fun fetchOEmbed(endpoint: String): LinkPreview? {
        val body = httpGetText(endpoint, MAX_OEMBED_BYTES) ?: return null
        val dto = runCatching { json.decodeFromString<OEmbedResponseDto>(body) }.getOrNull() ?: return null
        if (dto.title == null && dto.thumbnailUrl == null) return null
        return LinkPreview(title = dto.title, imageUrl = dto.thumbnailUrl)
    }

    private fun parseOpenGraph(html: String, url: String): LinkPreview {
        val ogTitle = metaContent(html, "og:title")
        val ogImage = metaContent(html, "og:image")
        val titleTag = Regex("<title[^>]*>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)?.trim()
        val title = (ogTitle ?: titleTag ?: linkHostLabel(url))?.let { decodeHtmlEntities(it) }
        return LinkPreview(title = title, imageUrl = ogImage?.let { resolveUrl(url, it) })
    }

    private fun metaContent(html: String, property: String): String? {
        val nameThenContent = Regex(
            "<meta[^>]*(?:property|name)=[\"']$property[\"'][^>]*content=[\"'](.*?)[\"']",
            RegexOption.IGNORE_CASE
        )
        val contentThenName = Regex(
            "<meta[^>]*content=[\"'](.*?)[\"'][^>]*(?:property|name)=[\"']$property[\"']",
            RegexOption.IGNORE_CASE
        )
        return nameThenContent.find(html)?.groupValues?.get(1) ?: contentThenName.find(html)?.groupValues?.get(1)
    }

    private fun resolveUrl(base: String, maybeRelative: String): String =
        runCatching { URL(URL(base), maybeRelative).toString() }.getOrDefault(maybeRelative)

    private fun decodeHtmlEntities(text: String): String = text
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    private fun httpGetText(url: String, maxBytes: Int): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) return null
            String(readLimited(connection.inputStream, maxBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun readLimited(stream: InputStream, maxBytes: Int): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8_192)
        var total = 0
        while (total < maxBytes) {
            val read = stream.read(chunk, 0, minOf(chunk.size, maxBytes - total))
            if (read == -1) break
            buffer.write(chunk, 0, read)
            total += read
        }
        return buffer.toByteArray()
    }
}

@Serializable
private data class OEmbedResponseDto(
    val title: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null
)
