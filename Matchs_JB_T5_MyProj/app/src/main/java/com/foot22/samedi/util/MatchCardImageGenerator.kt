package com.foot22.samedi.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.PlayerPosition
import com.foot22.samedi.data.model.TeamSide
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object MatchCardImageGenerator {

    private data class PlayerCoords(
        val player: PlayerEntity,
        val x: Float,
        val y: Float,
        val side: TeamSide
    )

    fun generateLineupOrScoreCard(
        context: Context,
        match: MatchEntity,
        bluePlayers: List<PlayerEntity>,
        orangePlayers: List<PlayerEntity>,
        settings: AppSettings,
        blueAvg: Double? = null,
        orangeAvg: Double? = null
    ): Uri {
        val width = 1280
        val height = 720

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Realistic grass lawn with alternating vertical stripes
        drawPitchGrass(canvas, width, height)

        // 2. White field lines
        val pitchRect = RectF(60f, 40f, width - 60f, height - 40f)
        drawPitchMarkings(canvas, pitchRect)

        // 3. Top TV broadcast banner
        drawTopBroadcastBanner(canvas, width, match, bluePlayers.size, orangePlayers.size, settings)

        // 4. Compute tactical positions (1-2-2-1 or adapted to team size)
        val blueCoords = computeTeamPositions(bluePlayers, TeamSide.BLEU, pitchRect)
        val orangeCoords = computeTeamPositions(orangePlayers, TeamSide.ORANGE, pitchRect)

        // 5. Draw player badges (photos or jerseys)
        (blueCoords + orangeCoords).forEach { coord ->
            drawPlayerBadgeOnPitch(context, canvas, coord.player, coord.x, coord.y, coord.side)
        }

        // 6. Save PNG and return FileProvider URI
        val outDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(outDir, "tactical_pitch_${match.id}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun drawPitchGrass(canvas: Canvas, width: Int, height: Int) {
        val stripeCount = 14
        val stripeWidth = width.toFloat() / stripeCount
        val darkGreen = Color.parseColor("#1B6B2B")
        val lightGreen = Color.parseColor("#228B36")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until stripeCount) {
            paint.color = if (i % 2 == 0) darkGreen else lightGreen
            canvas.drawRect(i * stripeWidth, 0f, (i + 1) * stripeWidth, height.toFloat(), paint)
        }
    }

    private fun drawPitchMarkings(canvas: Canvas, r: RectF) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.FILL
        }

        // Outer boundary
        canvas.drawRect(r, linePaint)

        // Halfway line & center circle
        val midX = r.centerX()
        canvas.drawLine(midX, r.top, midX, r.bottom, linePaint)
        canvas.drawCircle(midX, r.centerY(), 90f, linePaint)
        canvas.drawCircle(midX, r.centerY(), 5f, fillPaint)

        // Left penalty area (Bleu)
        val boxWidth = 190f
        val boxHeight = 320f
        val boxTop = r.centerY() - boxHeight / 2f
        val boxBottom = r.centerY() + boxHeight / 2f
        canvas.drawRect(r.left, boxTop, r.left + boxWidth, boxBottom, linePaint)

        // Left goal box
        val goalBoxW = 75f
        val goalBoxH = 150f
        canvas.drawRect(r.left, r.centerY() - goalBoxH / 2f, r.left + goalBoxW, r.centerY() + goalBoxH / 2f, linePaint)

        // Left penalty spot & arc
        val leftSpotX = r.left + 140f
        canvas.drawCircle(leftSpotX, r.centerY(), 4.5f, fillPaint)
        val leftArc = RectF(leftSpotX - 70f, r.centerY() - 70f, leftSpotX + 70f, r.centerY() + 70f)
        canvas.drawArc(leftArc, -55f, 110f, false, linePaint)

        // Right penalty area (Orange)
        canvas.drawRect(r.right - boxWidth, boxTop, r.right, boxBottom, linePaint)

        // Right goal box
        canvas.drawRect(r.right - goalBoxW, r.centerY() - goalBoxH / 2f, r.right, r.centerY() + goalBoxH / 2f, linePaint)

        // Right penalty spot & arc
        val rightSpotX = r.right - 140f
        canvas.drawCircle(rightSpotX, r.centerY(), 4.5f, fillPaint)
        val rightArc = RectF(rightSpotX - 70f, r.centerY() - 70f, rightSpotX + 70f, r.centerY() + 70f)
        canvas.drawArc(rightArc, 125f, 110f, false, linePaint)

        // Corner arcs
        val cornerR = 20f
        canvas.drawArc(RectF(r.left - cornerR, r.top - cornerR, r.left + cornerR, r.top + cornerR), 0f, 90f, false, linePaint)
        canvas.drawArc(RectF(r.left - cornerR, r.bottom - cornerR, r.left + cornerR, r.bottom + cornerR), 270f, 90f, false, linePaint)
        canvas.drawArc(RectF(r.right - cornerR, r.top - cornerR, r.right + cornerR, r.top + cornerR), 90f, 90f, false, linePaint)
        canvas.drawArc(RectF(r.right - cornerR, r.bottom - cornerR, r.right + cornerR, r.bottom + cornerR), 180f, 90f, false, linePaint)
    }

    private fun drawTopBroadcastBanner(
        canvas: Canvas,
        width: Int,
        match: MatchEntity,
        blueCount: Int,
        orangeCount: Int,
        settings: AppSettings
    ) {
        val bannerW = 680f
        val bannerH = 54f
        val bannerLeft = (width - bannerW) / 2f
        val bannerTop = 50f
        val rect = RectF(bannerLeft, bannerTop, bannerLeft + bannerW, bannerTop + bannerH)

        // Subtle gradient container
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            setShadowLayer(14f, 0f, 6f, Color.parseColor("#80000000"))
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(rect, 27f, 27f, bgPaint)
        canvas.drawRoundRect(rect, 27f, 27f, borderPaint)

        // Dot accents
        val blueDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2563EB") }
        val orangeDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F97316") }
        canvas.drawCircle(rect.left + 24f, rect.centerY(), 6f, blueDot)
        canvas.drawCircle(rect.right - 24f, rect.centerY(), 6f, orangeDot)

        // Text title
        val hasScore = match.blueScore != null && match.orangeScore != null
        val titleText = if (hasScore) {
            "RÉSULTAT • ${match.blueScore} - ${match.orangeScore} • ${settings.blueTeamName.uppercase()} vs ${settings.orangeTeamName.uppercase()}"
        } else {
            "MATCHS JB-T5 • $blueCount VS $orangeCount • BLEU VS ORANGE"
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
        }
        canvas.drawText(titleText, rect.centerX(), rect.centerY() + 7f, textPaint)
    }

    private fun computeTeamPositions(
        players: List<PlayerEntity>,
        side: TeamSide,
        pitch: RectF
    ): List<PlayerCoords> {
        if (players.isEmpty()) return emptyList()

        val isBlue = side == TeamSide.BLEU
        val halfLeft = if (isBlue) pitch.left else pitch.centerX()
        val halfRight = if (isBlue) pitch.centerX() else pitch.right
        val halfWidth = halfRight - halfLeft

        // Sort by position: GB -> DEF -> MIL/POLY -> ATT
        val gks = players.filter { it.position == PlayerPosition.GARDIEN }
        val defs = players.filter { it.position == PlayerPosition.DEFENSEUR }
        val atts = players.filter { it.position == PlayerPosition.ATTAQUANT }
        val mids = players.filter { it !in gks && it !in defs && it !in atts }

        // If no explicit positions or uneven, distribute nicely
        val ordered = mutableListOf<PlayerEntity>().apply {
            addAll(gks)
            addAll(defs)
            addAll(mids)
            addAll(atts)
        }

        val results = mutableListOf<PlayerCoords>()

        // Specific 6v6 TV tactical layout (1 - 2 - 2 - 1)
        if (ordered.size == 6) {
            val gk = gks.firstOrNull() ?: ordered[0]
            val rest1 = ordered.filter { it.id != gk.id }
            val d1 = defs.getOrNull(0) ?: rest1.getOrNull(0)
            val d2 = defs.getOrNull(1) ?: rest1.getOrNull(1)
            val rest2 = rest1.filter { it.id != d1?.id && it.id != d2?.id }
            val a1 = atts.firstOrNull() ?: rest2.lastOrNull()
            val mList = rest2.filter { it.id != a1?.id }
            val m1 = mList.getOrNull(0)
            val m2 = mList.getOrNull(1)

            val xGK = if (isBlue) halfLeft + halfWidth * 0.16f else halfRight - halfWidth * 0.16f
            val xDEF = if (isBlue) halfLeft + halfWidth * 0.38f else halfRight - halfWidth * 0.38f
            val xMIL = if (isBlue) halfLeft + halfWidth * 0.62f else halfRight - halfWidth * 0.62f
            val xATT = if (isBlue) halfLeft + halfWidth * 0.85f else halfRight - halfWidth * 0.85f

            gk?.let { results.add(PlayerCoords(it, xGK, pitch.centerY() + 8f, side)) }
            d1?.let { results.add(PlayerCoords(it, xDEF, pitch.centerY() - 175f, side)) }
            d2?.let { results.add(PlayerCoords(it, xDEF, pitch.centerY() + 190f, side)) }
            m1?.let { results.add(PlayerCoords(it, xMIL, pitch.centerY() - 95f, side)) }
            m2?.let { results.add(PlayerCoords(it, xMIL, pitch.centerY() + 105f, side)) }
            a1?.let { results.add(PlayerCoords(it, xATT, pitch.centerY() + 8f, side)) }

            return results
        }

        // Generic dynamic fallback for 4 to 8 players
        val count = ordered.size
        for (i in 0 until count) {
            val p = ordered[i]
            val progress = (i + 0.5f) / count
            val x = if (isBlue) {
                halfLeft + 60f + (halfWidth - 120f) * (0.15f + 0.75f * (i.toFloat() / maxOf(1, count - 1)))
            } else {
                halfRight - 60f - (halfWidth - 120f) * (0.15f + 0.75f * (i.toFloat() / maxOf(1, count - 1)))
            }
            val ySpread = when (i % 3) {
                0 -> pitch.centerY()
                1 -> pitch.centerY() - 150f
                else -> pitch.centerY() + 150f
            }
            results.add(PlayerCoords(p, x, ySpread, side))
        }
        return results
    }

    private fun drawPlayerBadgeOnPitch(
        context: Context,
        canvas: Canvas,
        player: PlayerEntity,
        cx: Float,
        cy: Float,
        side: TeamSide
    ) {
        val radius = 34f

        // 1. Shadow under badge
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#60000000")
            setShadowLayer(8f, 0f, 4f, Color.parseColor("#80000000"))
        }
        canvas.drawCircle(cx, cy, radius, shadowPaint)

        // 2. Load player photo if available
        val loadedBitmap = loadPlayerBitmapSafe(context, player.photoUri)

        if (loadedBitmap != null) {
            // Draw round photo avatar
            drawCircularBitmap(canvas, loadedBitmap, cx, cy, radius)
        } else {
            // Draw Stylized Jersey Badge
            drawJerseyBadge(canvas, cx, cy, radius, side, player.position == PlayerPosition.GARDIEN)
        }

        // 3. Ring border
        val ringColor = if (side == TeamSide.BLEU) Color.parseColor("#3B82F6") else Color.parseColor("#F97316")
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        val outerGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(cx, cy, radius, ringPaint)
        canvas.drawCircle(cx, cy, radius + 2f, outerGlow)

        // 4. Name & Position Label capsule below badge
        drawPlayerNamePill(canvas, player, cx, cy + radius + 15f)
    }

    private fun loadPlayerBitmapSafe(context: Context, photoUri: String?): Bitmap? {
        if (photoUri.isNullOrBlank()) return null
        return runCatching {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            if (photoUri.startsWith("content://") || photoUri.startsWith("file://")) {
                val uri = Uri.parse(photoUri)
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else {
                BitmapFactory.decodeFile(photoUri, options)
            }

            // Calculate sample size for target ~200px
            val targetSize = 200
            var sampleSize = 1
            while (options.outWidth / (sampleSize * 2) >= targetSize && options.outHeight / (sampleSize * 2) >= targetSize) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            if (photoUri.startsWith("content://") || photoUri.startsWith("file://")) {
                val uri = Uri.parse(photoUri)
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
            } else {
                BitmapFactory.decodeFile(photoUri, decodeOpts)
            }
        }.getOrNull()
    }

    private fun drawCircularBitmap(canvas: Canvas, src: Bitmap, cx: Float, cy: Float, radius: Float) {
        val minDim = minOf(src.width, src.height)
        val cropX = (src.width - minDim) / 2
        val cropY = (src.height - minDim) / 2
        val squareBitmap = if (src.width == src.height) {
            src
        } else {
            Bitmap.createBitmap(src, cropX, cropY, minDim, minDim)
        }
        val size = (radius * 2).toInt()
        val scaled = Bitmap.createScaledBitmap(squareBitmap, size, size, true)
        val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = Matrix().apply {
            setTranslate(cx - radius, cy - radius)
        }
        shader.setLocalMatrix(matrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawJerseyBadge(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        side: TeamSide,
        isGk: Boolean
    ) {
        // Base dark silver badge disc
        val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
        }
        canvas.drawCircle(cx, cy, radius, discPaint)

        // Jersey colors
        val jerseyColor = when {
            isGk && side == TeamSide.BLEU -> Color.parseColor("#06B6D4") // Cyan GK
            isGk && side == TeamSide.ORANGE -> Color.parseColor("#EAB308") // Yellow GK
            side == TeamSide.BLEU -> Color.parseColor("#2563EB") // Blue shirt
            else -> Color.parseColor("#EA580C") // Orange shirt
        }
        val collarColor = Color.parseColor("#F8FAFC")

        val path = Path()
        // Stylized shirt shape centered at (cx, cy)
        val w = radius * 0.85f
        val h = radius * 0.95f
        val top = cy - h * 0.5f
        val bottom = cy + h * 0.55f

        path.moveTo(cx - w * 0.45f, top)
        path.lineTo(cx - w * 0.85f, top + h * 0.35f) // Left sleeve
        path.lineTo(cx - w * 0.55f, top + h * 0.55f)
        path.lineTo(cx - w * 0.42f, top + h * 0.40f)
        path.lineTo(cx - w * 0.45f, bottom)          // Left waist
        path.lineTo(cx + w * 0.45f, bottom)          // Right waist
        path.lineTo(cx + w * 0.42f, top + h * 0.40f)
        path.lineTo(cx + w * 0.55f, top + h * 0.55f)
        path.lineTo(cx + w * 0.85f, top + h * 0.35f) // Right sleeve
        path.lineTo(cx + w * 0.45f, top)
        path.close()

        val shirtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = jerseyColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, shirtPaint)

        // White V-collar
        val collar = Path().apply {
            moveTo(cx - w * 0.22f, top)
            lineTo(cx, top + h * 0.22f)
            lineTo(cx + w * 0.22f, top)
            close()
        }
        val collarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = collarColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(collar, collarPaint)
    }

    private fun drawPlayerNamePill(
        canvas: Canvas,
        player: PlayerEntity,
        cx: Float,
        cy: Float
    ) {
        val name = player.displayName
        val pos = "[${player.position.shortLabel}]"
        val fullText = "$name $pos"

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textW = textPaint.measureText(fullText)
        val pillW = maxOf(textW + 16f, 68f)
        val pillH = 22f
        val pillRect = RectF(cx - pillW / 2f, cy - pillH / 2f, cx + pillW / 2f, cy + pillH / 2f)

        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E60F172A")
            setShadowLayer(4f, 0f, 2f, Color.parseColor("#90000000"))
        }
        val pillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(pillRect, 11f, 11f, pillBg)
        canvas.drawRoundRect(pillRect, 11f, 11f, pillBorder)

        canvas.drawText(fullText, cx, cy + 4.5f, textPaint)
    }
}
