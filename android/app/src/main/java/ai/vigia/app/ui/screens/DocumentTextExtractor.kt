package ai.vigia.app.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipInputStream

/** Lit localement le texte d'un PDF, DOCX ou TXT avant de l'envoyer à l'analyse d'emploi. */
object DocumentTextExtractor {
    private const val MAX_TEXT = 20_000

    fun read(context: Context, uri: Uri, mime: String, fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val text = when {
            mime == "application/pdf" || extension == "pdf" -> readPdf(context, uri)
            extension == "docx" || mime.contains("wordprocessingml") -> readDocx(context, uri)
            extension == "txt" || mime.startsWith("text/") ->
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            else -> throw IllegalArgumentException("Choisis un fichier PDF, Word (.docx) ou texte (.txt).")
        }.trim().take(MAX_TEXT)

        if (text.length < 20) {
            throw IllegalArgumentException("Je n'ai pas trouvé de texte lisible dans ce document. Pour un PDF scanné, choisis une photo nette de la page.")
        }
        return text
    }

    private fun readPdf(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Impossible d'ouvrir ce document.")
        input.use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper().apply {
                    startPage = 1
                    endPage = document.numberOfPages.coerceAtMost(12)
                }
                return stripper.getText(document)
            }
        }
    }

    private fun readDocx(context: Context, uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Impossible d'ouvrir ce document.")
        input.use { source ->
            ZipInputStream(source).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == "word/document.xml") {
                        val parser = Xml.newPullParser()
                        parser.setInput(zip, "UTF-8")
                        val result = StringBuilder()
                        var event = parser.eventType
                        while (event != XmlPullParser.END_DOCUMENT && result.length < MAX_TEXT) {
                            if (event == XmlPullParser.TEXT) result.append(parser.text).append(' ')
                            event = parser.next()
                        }
                        return result.toString()
                    }
                    zip.closeEntry()
                }
            }
        }
        return ""
    }
}
