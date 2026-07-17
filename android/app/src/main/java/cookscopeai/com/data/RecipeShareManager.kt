package cookscopeai.com.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import cookscopeai.com.data.model.Recipe
import java.io.File
import java.io.FileOutputStream

/**
 * Generates a shareable recipe summary card as a [Bitmap] and launches the
 * Android share sheet so users can post it to social media.
 *
 * The card uses the CookScope black-and-gold aesthetic and is rendered entirely
 * on an Android [Canvas] (no Compose composition required) for reliability.
 */
object RecipeShareManager {

    private const val CARD_W = 1080
    private const val CARD_H = 1350
    private const val PADDING = 72f
    private const val TAG = "RecipeShareManager"

    // Brand colors as ARGB Ints (Android Paint uses Int, not Compose Color)
    private const val COL_BG = 0xFF0A0A0A.toInt()
    private const val COL_SURFACE = 0xFF141414.toInt()
    private const val COL_GOLD = 0xFFE6B800.toInt()
    private const val COL_GOLD_LIGHT = 0xFFFFD700.toInt()
    private const val COL_WHITE = 0xFFF5F5F5.toInt()
    private const val COL_MUTED = 0xFFB0B0B0.toInt()
    private const val COL_DIVIDER = 0xFF2A2A2A.toInt()
    private const val COL_CARBS = 0xFFE9C46A.toInt()
    private const val COL_FAT = 0xFF2A9D8F.toInt()

    /**
     * Renders the recipe to a [Bitmap] card. Runs on a background thread.
     */
    suspend fun generateCard(context: Context, recipe: Recipe): Bitmap {
        val dishBitmap = loadRecipeImage(context, recipe.imageUrl)
        val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawHeroImage(canvas, dishBitmap)
        drawTitleOverlay(canvas, recipe)
        drawMetaRow(canvas, recipe)
        drawDescription(canvas, recipe)
        drawNutritionRow(canvas, recipe)
        drawBrandingFooter(canvas)

        return bitmap
    }

    /**
     * Generates the card, writes it to the cache dir, and launches the Android
     * share sheet. Call from a coroutine on Dispatchers.IO.
     *
     * @return true if the share sheet was launched successfully.
     */
    suspend fun shareRecipe(context: Context, recipe: Recipe): Boolean {
        return try {
            val card = generateCard(context, recipe)
            val file = File(context.cacheDir, "recipe_share_${recipe.id}.png")
            FileOutputStream(file).use { out -> card.compress(Bitmap.CompressFormat.PNG, 100, out) }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Check out this recipe on CookScope: ${recipe.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Recipe").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to share recipe", e)
            false
        }
    }

    private suspend fun loadRecipeImage(context: Context, url: String): Bitmap? {
        if (url.isBlank()) return null
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .build()
            val result = loader.execute(request)
            result.image?.toBitmap()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Image load failed for share card", e)
            null
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COL_BG }
        canvas.drawRect(0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(), paint)
    }

    private fun drawHeroImage(canvas: Canvas, image: Bitmap?) {
        val heroHeight = 560
        val heroRect = Rect(0, 0, CARD_W, heroHeight)
        if (image != null) {
            val srcW = image.width
            val srcH = image.height
            val scale = maxOf(heroRect.width().toFloat() / srcW, heroRect.height().toFloat() / srcH)
            val scaledW = srcW * scale
            val scaledH = srcH * scale
            val srcLeft = (scaledW - heroRect.width()) / 2f / scale
            val srcTop = (scaledH - heroRect.height()) / 2f / scale
            val srcRect = Rect(
                srcLeft.toInt(),
                srcTop.toInt(),
                (srcLeft + heroRect.width() / scale).toInt(),
                (srcTop + heroRect.height() / scale).toInt(),
            )
            canvas.drawBitmap(image, srcRect, heroRect, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COL_SURFACE }
            canvas.drawRect(heroRect, paint)
            val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COL_GOLD
                textSize = 120f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT
            }
            canvas.drawText("\uD83C\uDF73", CARD_W / 2f, heroHeight / 2f + 40f, gold)
        }
        // Bottom gradient for legibility
        val gradient = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, heroHeight - 320f,
                0f, heroHeight.toFloat(),
                intArrayOf(0x00000000, 0xAA000000.toInt(), 0xF0000000.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, heroHeight - 320f, CARD_W.toFloat(), heroHeight.toFloat(), gradient)
    }

    private fun drawTitleOverlay(canvas: Canvas, recipe: Recipe) {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COL_WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val maxWidth = CARD_W - PADDING.toInt() * 2
        val layout = StaticLayout.Builder.obtain(
            recipe.title, 0, recipe.title.length, titlePaint, maxWidth,
        ).setLineSpacing(6f, 1f).build()
        val titleBottom = 560 - 28f
        val titleTop = titleBottom - layout.height
        canvas.save()
        canvas.translate(PADDING, titleTop)
        layout.draw(canvas)
        canvas.restore()

        if (recipe.isPremium) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COL_GOLD }
            val badgeText = "PREMIUM"
            val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COL_BG
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val badgeW = badgeTextPaint.measureText(badgeText) + 36f
            val badgeH = 44f
            val badgeLeft = PADDING
            val badgeTop = titleTop - badgeH - 16f
            canvas.drawRoundRect(
                RectF(badgeLeft, badgeTop, badgeLeft + badgeW, badgeTop + badgeH),
                8f, 8f, badgePaint,
            )
            canvas.drawText(
                badgeText,
                badgeLeft + 18f,
                badgeTop + badgeH - 14f,
                badgeTextPaint,
            )
        }
    }

    private fun drawMetaRow(canvas: Canvas, recipe: Recipe) {
        val y = 600f
        val items = listOf(
            "\uD83C\uDF5D" to recipe.cuisine,
            "\u23F1" to "${recipe.cookTimeMin} min",
            "\uD83D\uDD73" to "${recipe.servings} srv",
            "\uD83D\uDD25" to "${recipe.caloriesPerServing} kcal",
        )
        val sectionW = (CARD_W - PADDING * 2) / items.size
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COL_MUTED
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        items.forEachIndexed { i, (emoji, value) ->
            val cx = PADDING + sectionW * i + sectionW / 2f
            canvas.drawText(emoji, cx, y, emojiPaint)
            canvas.drawText(value, cx, y + 38f, textPaint)
        }
    }

    private fun drawDescription(canvas: Canvas, recipe: Recipe) {
        val top = 720f
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COL_GOLD
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ABOUT", PADDING, top, labelPaint)

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COL_WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val maxWidth = CARD_W - PADDING.toInt() * 2
        val fullText = recipe.description.ifBlank { "A delicious ${recipe.cuisine} recipe." }
        val layout = StaticLayout.Builder.obtain(
            fullText, 0, fullText.length, bodyPaint, maxWidth,
        ).setLineSpacing(10f, 1f).build()

        // Clip to first 4 lines by translating and using canvas clip
        val maxLines = 4
        val linesToDraw = minOf(layout.lineCount, maxLines)
        canvas.save()
        canvas.translate(PADDING, top + 48f)
        // Draw only the lines we want by clipping
        val clipBottom = layout.getLineTop(linesToDraw).toFloat()
        canvas.clipRect(0f, 0f, maxWidth.toFloat(), clipBottom)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawNutritionRow(canvas: Canvas, recipe: Recipe) {
        val top = 1000f
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COL_GOLD
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("NUTRITION PER SERVING", PADDING, top, labelPaint)

        val cards = listOf(
            Triple("Protein", "${recipe.proteinG}g", COL_GOLD_LIGHT),
            Triple("Carbs", "${recipe.carbsG}g", COL_CARBS),
            Triple("Fat", "${recipe.fatG}g", COL_FAT),
        )
        val cardSpacing = 24f
        val cardW = (CARD_W - PADDING * 2 - cardSpacing * (cards.size - 1)) / cards.size
        val cardH = 150f
        val cardTop = top + 32f
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COL_SURFACE }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COL_WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COL_MUTED
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        cards.forEachIndexed { i, (name, value, color) ->
            val left = PADDING + (cardW + cardSpacing) * i
            val rect = RectF(left, cardTop, left + cardW, cardTop + cardH)
            canvas.drawRoundRect(rect, 20f, 20f, cardPaint)
            dotPaint.color = color
            canvas.drawCircle(left + cardW / 2f, cardTop + 26f, 8f, dotPaint)
            canvas.drawText(value, left + cardW / 2f, cardTop + 86f, valuePaint)
            canvas.drawText(name, left + cardW / 2f, cardTop + 124f, namePaint)
        }
    }

    private fun drawBrandingFooter(canvas: Canvas) {
        val top = 1220f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COL_DIVIDER
            strokeWidth = 2f
        }
        canvas.drawLine(PADDING, top, CARD_W - PADDING, top, linePaint)

        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COL_GOLD
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("CookScope", PADDING, top + 56f, brandPaint)

        val tagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COL_MUTED
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Your AI Culinary Companion", CARD_W - PADDING, top + 56f, tagPaint)
    }
}
