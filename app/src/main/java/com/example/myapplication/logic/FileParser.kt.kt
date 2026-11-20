package com.example.myapplication.logic

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/** Читает содержимое выбранного файла: обычный текст или FB2. */
object FileParser {

    fun readFile(context: Context, uri: Uri, fileName: String): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Не удалось открыть файл")

        val text = inputStream.use { stream ->
            if (fileName.endsWith(".fb2", ignoreCase = true)) {
                parseFb2(stream)
            } else {
                stream.bufferedReader().readText()
            }
        }

        if (text.isBlank()) throw IllegalStateException("Файл пустой")
        return text
    }

    /** Достаёт из FB2 только текст книги (содержимое тега body). */
    private fun parseFb2(inputStream: InputStream): String {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(inputStream, null)
        }

        val result = StringBuilder()
        var insideBody = false
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "body" -> insideBody = true
                    "title" -> if (insideBody) result.append("\n\n*** ")
                }

                XmlPullParser.TEXT -> if (insideBody) result.append(parser.text)

                XmlPullParser.END_TAG -> when (parser.name) {
                    "title" -> if (insideBody) result.append(" ***\n\n")
                    "p" -> if (insideBody) result.append("\n\n")
                    "body" -> insideBody = false
                }
            }
            eventType = parser.next()
        }

        return result.toString()
            .replace(Regex("[ \t]+\n"), "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
