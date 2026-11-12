package com.foot22.samedi.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.domain.SaturdayScheduler
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object MatchCardImageGenerator {

    fun generateLineupOrScoreCard(
        context: Context,
        match: MatchEntity,
        bluePlayers: List<PlayerEntity>,
        orangePlayers: List<PlayerEntity>,
        settings: AppSettings,
        blueAvg: Double? = null,
        orangeAvg: Double? = null
    ): Uri {
        val width = 1080
        val maxRows = maxOf(bluePlayers.size, orangePlayers.size, 5)
        val height = 430 + maxRows * 74 + 100

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Deep night stadium background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B1120") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle pitch lines
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(RectF(28f, 28f, width - 28f, height - 28f), 32f, 32f, linePaint)
        canvas.drawCircle(width / 2f, height / 2f + 40f, 90f, linePaint)

        // Header title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }

        val hasScore = match.blueScore != null && match.orangeScore != null
        val header = if (hasScore) "RÉSULTAT DU MATCH" else "FEUILLE DE MATCH"
        canvas.drawText(header, width / 2f, 96f, titlePaint)
        canvas.drawText(
            "${SaturdayScheduler.formatFrenchDateTime(match.dateTimeIso)}  •  ${match.location}",
            width / 2f,
            140f,
            subtitlePaint
        )

        // Score or VS banner
        val bannerRect = RectF(70f, 175f, width - 70f, 265f)
        val bannerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#162033") }
        canvas.drawRoundRect(bannerRect, 24f, 24f, bannerFill)

        val centerLabel = if (hasScore) {
            "${match.blueScore}  -  ${match.orangeScore}"
        } else {
            "VS"
        }
        val vsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (hasScore) 48f else 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(centerLabel, width / 2f, 236f, vsPaint)

        // Left & Right columns
        val colWidth = (width - 140f) / 2f
        val leftX = 56f
        val rightX = 56f + colWidth + 28f
        val topY = 300f
        val bottomY = height - 96f

        drawTeamColumn(
            canvas = canvas,
            rect = RectF(leftX, topY, leftX + colWidth, bottomY),
            teamTitle = settings.blueTeamName,
            avg = blueAvg,
            players = bluePlayers,
            accentHex = "#2563EB",
            bgHex = "#0F1D3A"
        )
        drawTeamColumn(
            canvas = canvas,
            rect = RectF(rightX, topY, rightX + colWidth, bottomY),
            teamTitle = settings.orangeTeamName,
            avg = orangeAvg,
            players = orangePlayers,
            accentHex = "#F97316",
            bgHex = "#2A180D"
        )

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Samedi 22h • Bleu vs Orange", width / 2f, height - 48f, footerPaint)

        val outDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(outDir, "match_${match.id}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun drawTeamColumn(
        canvas: Canvas,
        rect: RectF,
        teamTitle: String,
        avg: Double?,
        players: List<PlayerEntity>,
        accentHex: String,
        bgHex: String
    ) {
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(bgHex) }
        canvas.drawRoundRect(rect, 24f, 24f, cardPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(accentHex)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(rect, 24f, 24f, borderPaint)

        val headerRect = RectF(rect.left, rect.top, rect.right, rect.top + 76f)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(accentHex) }
        canvas.drawRoundRect(headerRect, 24f, 24f, headerPaint)
        canvas.drawRect(rect.left, rect.top + 40f, rect.right, rect.top + 76f, headerPaint)

        val teamTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val avgTag = if (avg != null && avg > 0.0) {
            " (${String.format(Locale.US, "%.1f", avg)})"
        } else {
            ""
        }
        canvas.drawText("$teamTitle$avgTag", rect.centerX(), rect.top + 48f, teamTitlePaint)

        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E293B") }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val posPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 22f
            textAlign = Paint.Align.RIGHT
        }

        var y = rect.top + 102f
        players.forEachIndexed { idx, player ->
            val pill = RectF(rect.left + 18f, y, rect.right - 18f, y + 58f)
            canvas.drawRoundRect(pill, 14f, 14f, rowPaint)
            canvas.drawText("${idx + 1}. ${player.displayName}", pill.left + 18f, pill.centerY() + 9f, namePaint)
            canvas.drawText(player.position.shortLabel, pill.right - 18f, pill.centerY() + 8f, posPaint)
            y += 70f
        }
    }
}
