package com.sl.mycycle.data.report

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.sl.mycycle.R
import com.sl.mycycle.data.repository.CycleDayRepository
import com.sl.mycycle.domain.engine.CycleDetector
import com.sl.mycycle.domain.model.Cycle
import com.sl.mycycle.domain.model.CycleDay
import com.sl.mycycle.domain.model.Symptom
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.first

class DoctorReportService(
    private val context: Context,
    private val cycleDayRepository: CycleDayRepository,
    private val cycleDetector: CycleDetector
) {

    companion object {
        private const val MAX_CYCLES = 12
    }

    suspend fun buildPdf(): ByteArray {
        val days = cycleDayRepository.observeAll().first().sortedBy { it.date }
        val cycles = cycleDetector.detectCycles(days)
        val document = PdfDocument()
        return try {
            ReportWriter(context, document).write(days, cycles.takeLast(MAX_CYCLES))
            ByteArrayOutputStream().use { output ->
                document.writeTo(output)
                output.toByteArray()
            }
        } finally {
            document.close()
        }
    }
}

private class ReportWriter(
    private val context: Context,
    private val document: PdfDocument
) {

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 42f
        private const val TITLE_SIZE = 19f
        private const val HEADING_SIZE = 13f
        private const val BODY_SIZE = 10f
        private const val LINE_HEIGHT = 15f
        private const val SECTION_GAP = 10f
        private const val BOTTOM_MARGIN = 48f
        private const val MAX_SYMPTOMS = 8
    }

    private val locale = context.resources.configuration.locales[0]
    private val dateFormatter = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
    private val titlePaint = paint(TITLE_SIZE, true)
    private val headingPaint = paint(HEADING_SIZE, true)
    private val bodyPaint = paint(BODY_SIZE, false)
    private var pageNumber = 0
    private lateinit var page: PdfDocument.Page
    private var y = MARGIN

    fun write(days: List<CycleDay>, cycles: List<Cycle>) {
        newPage()
        line(context.getString(R.string.report_title), titlePaint)
        gap()
        line(
            context.getString(
                R.string.report_generated,
                LocalDate.now().format(dateFormatter)
            ),
            bodyPaint
        )
        wrapped(context.getString(R.string.report_disclaimer), bodyPaint)
        gap()
        writeSummary(cycles)
        writeCycles(cycles)
        writeSymptoms(days, cycles)
        finishPage()
    }

    private fun writeSummary(cycles: List<Cycle>) {
        heading(context.getString(R.string.report_summary))
        val completed = cycles.filter { it.isComplete && it.length != null }
        val lengths = completed.mapNotNull { it.length }
        val periodLengths = completed.mapNotNull { it.periodLength }

        wrapped(
            context.getString(
                R.string.report_cycle_count,
                completed.size
            ),
            bodyPaint
        )
        if (lengths.isNotEmpty()) {
            wrapped(
                context.getString(
                    R.string.report_cycle_range,
                    lengths.min(),
                    lengths.max()
                ),
                bodyPaint
            )
        }
        if (periodLengths.isNotEmpty()) {
            wrapped(
                context.getString(
                    R.string.report_period_range,
                    periodLengths.min(),
                    periodLengths.max()
                ),
                bodyPaint
            )
        }
        gap()
    }

    private fun writeCycles(cycles: List<Cycle>) {
        heading(context.getString(R.string.report_recent_cycles))
        if (cycles.isEmpty()) {
            wrapped(context.getString(R.string.report_no_cycles), bodyPaint)
            gap()
            return
        }

        cycles.asReversed().forEach { cycle ->
            val length = cycle.length?.toString()
                ?: context.getString(R.string.report_current)
            val bleeding = cycle.periodLength?.toString()
                ?: context.getString(R.string.report_unknown)
            wrapped(
                context.getString(
                    R.string.report_cycle_row,
                    cycle.startDate.format(dateFormatter),
                    length,
                    bleeding
                ),
                bodyPaint
            )
        }
        gap()
    }

    private fun writeSymptoms(days: List<CycleDay>, cycles: List<Cycle>) {
        heading(context.getString(R.string.report_symptoms))
        val start = cycles.firstOrNull()?.startDate
        val relevantDays = if (start == null) {
            days
        } else {
            days.filter { !it.date.isBefore(start) }
        }
        val counts = Symptom.entries
            .map { symptom ->
                symptom to relevantDays.count { symptom in it.symptoms }
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(MAX_SYMPTOMS)

        if (counts.isEmpty()) {
            wrapped(context.getString(R.string.report_no_symptoms), bodyPaint)
        } else {
            counts.forEach { (symptom, count) ->
                wrapped(
                    context.getString(
                        R.string.report_symptom_row,
                        context.getString(symptom.labelRes),
                        count
                    ),
                    bodyPaint
                )
            }
        }
        gap()
        wrapped(context.getString(R.string.report_notes_omitted), bodyPaint)
    }

    private fun heading(text: String) {
        ensureSpace(LINE_HEIGHT + SECTION_GAP)
        y += SECTION_GAP
        line(text, headingPaint)
    }

    private fun gap() {
        y += SECTION_GAP
    }

    private fun line(text: String, paint: Paint) {
        ensureSpace(LINE_HEIGHT)
        page.canvas.drawText(text, MARGIN, y, paint)
        y += LINE_HEIGHT
    }

    private fun wrapped(text: String, paint: Paint) {
        text.split('\n').forEach { paragraph ->
            if (paragraph.isBlank()) {
                y += LINE_HEIGHT
            } else {
                wrapParagraph(paragraph, paint).forEach { line ->
                    this.line(line, paint)
                }
            }
        }
    }

    private fun wrapParagraph(text: String, paint: Paint): List<String> {
        val width = PAGE_WIDTH - MARGIN * 2
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""

        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= width) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }

    private fun ensureSpace(height: Float) {
        if (y + height <= PAGE_HEIGHT - BOTTOM_MARGIN) return
        finishPage()
        newPage()
    }

    private fun newPage() {
        pageNumber++
        page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
        y = MARGIN
    }

    private fun finishPage() {
        if (::page.isInitialized) {
            document.finishPage(page)
        }
    }

    private fun paint(size: Float, bold: Boolean): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}
