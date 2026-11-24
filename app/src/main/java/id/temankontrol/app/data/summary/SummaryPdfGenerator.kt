package id.temankontrol.app.data.summary

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SummaryPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 44
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    suspend fun generatePdfUri(
        context: Context,
        consultationId: String,
        title: String,
        bodyText: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val pdfFile = File(sharedDir, "Ringkasan_${consultationId.take(12)}.pdf")

            val document = PdfDocument()
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0, 105, 109)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 34, 34)
                textSize = 11f
            }

            val fullLayout = StaticLayout.Builder.obtain(
                bodyText,
                0,
                bodyText.length,
                bodyPaint,
                CONTENT_WIDTH
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1.15f)
                .build()

            var lineIdx = 0
            var pageNumber = 1
            while (lineIdx < fullLayout.lineCount || pageNumber == 1) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                var y = MARGIN.toFloat()
                if (pageNumber == 1) {
                    canvas.drawText("Teman Kontrol — Ringkasan Konsultasi", MARGIN.toFloat(), y + 16f, titlePaint)
                    y += 30f
                    val subLayout = StaticLayout.Builder.obtain(
                        title, 0, title.length,
                        TextPaint(titlePaint).apply { textSize = 14f; color = Color.BLACK },
                        CONTENT_WIDTH
                    ).build()
                    canvas.save()
                    canvas.translate(MARGIN.toFloat(), y)
                    subLayout.draw(canvas)
                    canvas.restore()
                    y += subLayout.height + 16f
                }

                val maxBottom = (PAGE_HEIGHT - MARGIN).toFloat()
                while (lineIdx < fullLayout.lineCount) {
                    val lineHeight = (fullLayout.getLineBottom(lineIdx) - fullLayout.getLineTop(lineIdx)).toFloat()
                    if (y + lineHeight > maxBottom) break
                    val start = fullLayout.getLineStart(lineIdx)
                    val end = fullLayout.getLineEnd(lineIdx)
                    val lineText = bodyText.substring(start, end).trimEnd('\n')
                    canvas.drawText(lineText, MARGIN.toFloat(), y - fullLayout.getLineAscent(lineIdx), bodyPaint)
                    y += lineHeight
                    lineIdx++
                }

                document.finishPage(page)
                pageNumber++
                if (lineIdx >= fullLayout.lineCount) break
            }

            FileOutputStream(pdfFile).use { fos ->
                document.writeTo(fos)
                fos.flush()
            }
            document.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
        }
    }
}
