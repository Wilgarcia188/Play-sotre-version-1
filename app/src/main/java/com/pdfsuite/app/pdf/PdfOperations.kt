package com.pdfsuite.app.pdf

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdmodel.PDDocument

/** PDF manipulation (merge, page extraction) backed by PdfBox-Android. */
object PdfOperations {

    suspend fun mergePdfs(
        context: Context,
        sources: List<Uri>,
        destination: Uri,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(sources.size >= 2) { "Need at least two files to merge" }

            val resolver = context.contentResolver
            val merger = PDFMergerUtility()

            resolver.openOutputStream(destination)?.use { output ->
                merger.destinationStream = output
                sources.forEach { uri ->
                    val input = resolver.openInputStream(uri)
                        ?: error("Could not open $uri")
                    merger.addSource(input)
                }
                merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
            } ?: error("Could not open destination $destination")
        }
    }

    /** Writes a new PDF to [destination] containing only [pageIndices] (0-based) from [source], in order. */
    suspend fun extractPages(
        context: Context,
        source: Uri,
        destination: Uri,
        pageIndices: List<Int>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(pageIndices.isNotEmpty()) { "No pages selected" }

            val resolver = context.contentResolver
            val input = resolver.openInputStream(source) ?: error("Could not open $source")

            input.use { stream ->
                PDDocument.load(stream).use { sourceDoc ->
                    PDDocument().use { newDoc ->
                        pageIndices.sorted().forEach { index ->
                            newDoc.importPage(sourceDoc.getPage(index))
                        }
                        resolver.openOutputStream(destination)?.use { output ->
                            newDoc.save(output)
                        } ?: error("Could not open destination $destination")
                    }
                }
            }
        }
    }
}
