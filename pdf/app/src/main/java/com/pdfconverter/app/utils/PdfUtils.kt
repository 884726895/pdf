package com.pdfconverter.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfUtils {

    fun getOutputDir(context: Context): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "PDFConverter"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generateFileName(prefix: String, ext: String): String {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${prefix}_${date}.$ext"
    }

    fun pdfToImage(
        context: Context,
        pdfUri: Uri,
        outputDir: File,
        format: Bitmap.CompressFormat,
        quality: Int,
        pageRange: IntRange?
    ): Result<List<File>> {
        return try {
            val files = mutableListOf<File>()
            val fd = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return Result.failure(Exception("无法打开文件"))

            fd.use { pfd ->
                val renderer = PdfRenderer(pfd)
                val totalPages = renderer.pageCount
                val range = pageRange ?: (0 until totalPages)

                for (i in range) {
                    if (i >= totalPages) break
                    val page = renderer.openPage(i)
                    val width = page.width * 2
                    val height = page.height * 2
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val ext = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
                    val file = File(outputDir, generateFileName("page_${i + 1}", ext))
                    FileOutputStream(file).use { out ->
                        bitmap.compress(format, quality, out)
                    }
                    bitmap.recycle()
                    files.add(file)
                }
                renderer.close()
            }
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun imageToPdf(
        imageUris: List<Uri>,
        context: Context,
        outputDir: File
    ): Result<File> {
        return try {
            val file = File(outputDir, generateFileName("merged", "pdf"))
            val document = PDDocument()

            for (uri in imageUris) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val byteArray = baos.toByteArray()
                bitmap.recycle()

                val pdImage = PDImageXObject.createFromByteArray(document, byteArray, "png")
                val page = PDPage(PDRectangle(pdImage.width.toFloat(), pdImage.height.toFloat()))
                document.addPage(page)

                val cs = PDPageContentStream(document, page)
                cs.drawImage(pdImage, 0f, 0f, pdImage.width.toFloat(), pdImage.height.toFloat())
                cs.close()
            }

            document.save(file)
            document.close()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pdfToText(context: Context, pdfUri: Uri, password: String? = null): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return Result.failure(Exception("无法打开文件"))

            val document = PDDocument.load(inputStream, password)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            inputStream.close()
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun textToPdf(text: String, outputDir: File): Result<File> {
        return try {
            val file = File(outputDir, generateFileName("document", "pdf"))
            val document = PDDocument()
            val page = PDPage()
            document.addPage(page)

            val cs = PDPageContentStream(document, page)
            cs.beginText()
            cs.setFont(PDType1Font.HELVETICA, 12f)
            cs.newLineAtOffset(50f, 750f)

            val lines = text.split("\n")
            for (line in lines) {
                cs.showText(line)
                cs.newLineAtOffset(0f, -18f)
            }
            cs.endText()
            cs.close()

            document.save(file)
            document.close()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun mergePdfs(pdfUris: List<Uri>, context: Context, outputDir: File): Result<File> {
        return try {
            val file = File(outputDir, generateFileName("merged", "pdf"))
            val mergedDoc = PDDocument()

            for (uri in pdfUris) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val doc = PDDocument.load(inputStream)
                for (i in 0 until doc.numberOfPages) {
                    mergedDoc.addPage(doc.getPage(i))
                }
                doc.close()
                inputStream?.close()
            }

            mergedDoc.save(file)
            mergedDoc.close()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun splitPdf(
        context: Context,
        pdfUri: Uri,
        outputDir: File,
        ranges: List<IntRange>
    ): Result<List<File>> {
        return try {
            val files = mutableListOf<File>()
            val inputStream = context.contentResolver.openInputStream(pdfUri)
            val doc = PDDocument.load(inputStream)

            for ((idx, range) in ranges.withIndex()) {
                val splitDoc = PDDocument()
                for (i in range) {
                    if (i < doc.numberOfPages) {
                        splitDoc.addPage(doc.getPage(i))
                    }
                }
                val file = File(outputDir, generateFileName("split_${idx + 1}", "pdf"))
                splitDoc.save(file)
                splitDoc.close()
                files.add(file)
            }
            doc.close()
            inputStream?.close()
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun compressPdf(
        context: Context,
        pdfUri: Uri,
        outputDir: File,
        qualityPercent: Int
    ): Result<File> {
        return try {
            val file = File(outputDir, generateFileName("compressed", "pdf"))
            val inputStream = context.contentResolver.openInputStream(pdfUri)
            val doc = PDDocument.load(inputStream)

            for (i in 0 until doc.numberOfPages) {
                val page = doc.getPage(i)
                val resources = page.resources
                if (resources != null) {
                    val xObjects = resources.xObjects
                    val keysToUpdate = xObjects.keys.filter {
                        xObjects[it] is PDImageXObject
                    }
                    for (key in keysToUpdate) {
                        val xObj = xObjects[key] as PDImageXObject
                        val bitmap = xObj.image
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent, baos)
                        val newImage = PDImageXObject.createFromByteArray(
                            doc, baos.toByteArray(), xObj.getSuffix()
                        )
                        xObjects.put(key, newImage)
                    }
                }
            }

            doc.save(file)
            doc.close()
            inputStream?.close()
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
