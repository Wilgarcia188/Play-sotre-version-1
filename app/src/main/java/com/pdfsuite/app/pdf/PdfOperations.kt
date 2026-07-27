package com.pdfsuite.app.pdf

import android.content.Context
import android.net.Uri
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument

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
            // addSource() only registers the stream; PDFMergerUtility never closes the
            // caller-supplied InputStreams itself, so we must track and close them ourselves.
            val openedStreams = mutableListOf<InputStream>()

            try {
                resolver.openOutputStream(destination)?.use { output ->
                    merger.destinationStream = output
                    sources.forEach { uri ->
                        val input = resolver.openInputStream(uri)
                            ?: error("Could not open $uri")
                        openedStreams += input
                        merger.addSource(input)
                    }
                    merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
                } ?: error("Could not open destination $destination")
            } finally {
                openedStreams.forEach { it.close() }
            }
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

    /** A page to carry over into a reorganized PDF: which page of the source to copy
     * ([originalIndex], 0-based), in what order (the position of this entry in the list
     * passed to [reorganizePages]), and how many extra degrees to rotate it on top of
     * its existing rotation (0/90/180/270, clockwise). */
    data class PageRotation(val originalIndex: Int, val rotationDelta: Int)

    /** Writes a new PDF to [destination] with [source]'s pages copied in [pagePlan]'s
     * order (so this also deletes any page whose index isn't included), each rotated by
     * its [PageRotation.rotationDelta]. */
    suspend fun reorganizePages(
        context: Context,
        source: Uri,
        destination: Uri,
        pagePlan: List<PageRotation>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(pagePlan.isNotEmpty()) { "No pages to save" }

            val resolver = context.contentResolver
            val input = resolver.openInputStream(source) ?: error("Could not open $source")

            input.use { stream ->
                PDDocument.load(stream).use { sourceDoc ->
                    PDDocument().use { newDoc ->
                        pagePlan.forEach { plan ->
                            val newPage = newDoc.importPage(sourceDoc.getPage(plan.originalIndex))
                            if (plan.rotationDelta != 0) {
                                newPage.rotation = (newPage.rotation + plan.rotationDelta) % 360
                            }
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
