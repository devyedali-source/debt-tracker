package com.elyedali.debttracker.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.elyedali.debttracker.data.LedgerData
import java.io.File

/** Builds PDF reports with Android's built-in PDF tools (no library, no server). */
object PdfExporter {
    private const val W = 595
    private const val H = 842
    private const val M = 40f

    fun export(context: Context, data: LedgerData, currency: String, personId: String?): File {
        val doc = PdfDocument()

        fun paint(size: Float, color: Int, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            isFakeBoldText = bold
        }

        val brand = 0xFF146C43.toInt()
        val greenC = 0xFF1E9E55.toInt()
        val redC = 0xFFD93025.toInt()
        val grayC = 0xFF777777.toInt()
        val titleP = paint(20f, brand, bold = true)
        val nameP = paint(14f, Color.BLACK, bold = true)
        val headP = paint(10f, grayC, bold = true)
        val textP = paint(11f, 0xFF222222.toInt())
        val greenP = paint(11f, greenC)
        val redP = paint(11f, redC)
        val greenB = paint(12f, greenC, bold = true)
        val redB = paint(12f, redC, bold = true)
        val smallP = paint(9f, grayC)
        val ruleP = Paint().apply { color = 0xFFDDDDDD.toInt(); strokeWidth = 1f }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun canvas(): Canvas = page!!.canvas

        fun finishPage() {
            val p = page ?: return
            val c = p.canvas
            c.drawText("Generated with Debt Tracker", M, H - 20f, smallP)
            val label = "Page $pageNo"
            c.drawText(label, W - M - smallP.measureText(label), H - 20f, smallP)
            doc.finishPage(p)
            page = null
        }

        fun newPage() {
            finishPage()
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            y = M + 10f
        }

        fun ensure(height: Float) {
            if (page == null || y + height > H - M - 20f) newPage()
        }

        fun left(s: String, p: Paint, x: Float = M) = canvas().drawText(s, x, y, p)
        fun right(s: String, p: Paint, xRight: Float = W - M) = canvas().drawText(s, xRight - p.measureText(s), y, p)
        fun rule() = canvas().drawLine(M, y, W - M, y, ruleP)
        fun fit(s: String, p: Paint, maxWidth: Float): String {
            if (p.measureText(s) <= maxWidth) return s
            var t = s
            while (t.isNotEmpty() && p.measureText("$t…") > maxWidth) t = t.dropLast(1)
            return "$t…"
        }

        val persons = if (personId == null) {
            data.persons.sortedBy { it.name.lowercase() }
        } else {
            data.persons.filter { it.id == personId }
        }

        newPage()
        if (personId == null) {
            left("Debt Tracker - Summary report", titleP); y += 18f
            left("Generated on ${Dates.format(System.currentTimeMillis())}", smallP); y += 26f
            left("You'll get: ${Money.format(data.totalToGet, currency)}", greenB); y += 18f
            left("You'll give: ${Money.format(data.totalToGive, currency)}", redB); y += 28f
            left("PERSON", headP); right("BALANCE", headP); y += 6f
            rule(); y += 16f
            persons.forEach { p ->
                ensure(18f)
                val b = data.balanceOf(p.id)
                left(fit(p.name, textP, 320f), textP)
                when {
                    b > 0 -> right("You'll get ${Money.format(b, currency)}", greenP)
                    b < 0 -> right("You'll give ${Money.format(b, currency)}", redP)
                    else -> right("Settled", smallP)
                }
                y += 18f
            }
            y += 24f
        }

        val dateX = M
        val noteX = M + 80f
        val gaveR = 380f
        val gotR = 465f
        val balR = W - M

        persons.forEach { p ->
            val entries = data.entriesOf(p.id).reversed()
            ensure(90f)
            if (personId != null) {
                left("Statement - ${fit(p.name, titleP, 420f)}", titleP); y += 18f
                left("Generated on ${Dates.format(System.currentTimeMillis())}", smallP); y += 26f
            } else {
                left(fit(p.name, nameP, 420f), nameP); y += 18f
            }
            if (p.phone.isNotBlank()) {
                left(p.phone, smallP); y += 16f
            }
            left("DATE", headP, dateX); left("NOTE", headP, noteX)
            right("YOU GAVE", headP, gaveR); right("YOU GOT", headP, gotR); right("BALANCE", headP, balR)
            y += 6f
            rule(); y += 16f
            var running = 0L
            entries.forEach { e ->
                ensure(18f)
                running += if (e.gave) e.amountCents else -e.amountCents
                left(Dates.format(e.date), textP, dateX)
                left(fit(e.note, textP, gaveR - noteX - 70f), textP, noteX)
                if (e.gave) right(Money.format(e.amountCents, currency), redP, gaveR)
                else right(Money.format(e.amountCents, currency), greenP, gotR)
                val balPaint = if (running > 0) greenP else if (running < 0) redP else textP
                right(Money.format(running, currency), balPaint, balR)
                y += 18f
            }
            if (entries.isEmpty()) {
                left("No entries", smallP); y += 18f
            }
            ensure(30f)
            rule(); y += 18f
            val b = data.balanceOf(p.id)
            when {
                b > 0 -> left("Balance: you'll get ${Money.format(b, currency)}", greenB)
                b < 0 -> left("Balance: you'll give ${Money.format(b, currency)}", redB)
                else -> left("Balance: settled", nameP)
            }
            y += 32f
        }
        finishPage()

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val name = if (personId == null) {
            "debt-tracker-report.pdf"
        } else {
            val safe = persons.firstOrNull()?.name?.replace(Regex("[^A-Za-z0-9]+"), "-")?.trim('-')
            "statement-${if (safe.isNullOrEmpty()) "person" else safe}.pdf"
        }
        val file = File(dir, name)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
