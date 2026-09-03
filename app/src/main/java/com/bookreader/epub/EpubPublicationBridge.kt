package com.bookreader.epub

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.IOException

class EpubParseResult(
    val title: String,
    val author: String?,
    val spineHrefs: List<String>
)

object EpubPublicationBridge {

    @JvmStatic
    fun parse(context: Context, epubFilePath: String): EpubParseResult = runBlocking {
        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
        val publicationParser = DefaultPublicationParser(context, httpClient, assetRetriever, null, emptyList())
        val publicationOpener = PublicationOpener(publicationParser)

        val assetTry = assetRetriever.retrieve(File(epubFilePath))
        val asset = assetTry.getOrNull() ?: throw IOException(
            "Readium could not retrieve the EPUB asset: ${assetTry.failureOrNull()}"
        )
        val publicationTry = publicationOpener.open(asset, allowUserInteraction = false)
        val publication = publicationTry.getOrNull() ?: throw IOException(
            "Readium could not parse the EPUB: ${publicationTry.failureOrNull()}"
        )

        val title = publication.metadata.title ?: "Untitled"
        val author = publication.metadata.authors.firstOrNull()?.name
        val spineHrefs = publication.readingOrder.map { it.href.toString() }
        EpubParseResult(title, author, spineHrefs)
    }
}
