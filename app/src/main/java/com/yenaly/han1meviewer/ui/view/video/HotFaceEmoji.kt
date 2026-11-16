package com.yenaly.han1meviewer.ui.view.video

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.yenaly.han1meviewer.R

private const val HOT_FACE = "\uD83E\uDD75"

/** 系统字库没有 🥵 时，改用 Noto 标准表情里的同一个字形。有这个字的手机保持原来的系统字形。 */
fun TextView.withFallbackHotFace(text: CharSequence): CharSequence {
    if (!text.contains(HOT_FACE) || paint.hasGlyph(HOT_FACE)) return text
    val size = (textSize * 1.15f).toInt().coerceAtLeast(1)
    val spannable = SpannableString(text)
    var index = text.indexOf(HOT_FACE)
    while (index >= 0) {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_h_keyframe)?.mutate()
            ?: return text
        drawable.setBounds(0, 0, size, size)
        spannable.setSpan(
            CenteredImageSpan(drawable),
            index,
            index + HOT_FACE.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        index = text.indexOf(HOT_FACE, index + HOT_FACE.length)
    }
    return spannable
}

private class CenteredImageSpan(private val image: Drawable) : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        val height = image.bounds.height()
        if (fm != null) {
            val metrics = paint.fontMetricsInt
            val center = (metrics.ascent + metrics.descent) / 2
            fm.ascent = center - height / 2
            fm.descent = center + height / 2
            fm.top = fm.ascent
            fm.bottom = fm.descent
        }
        return image.bounds.width()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        canvas.save()
        canvas.translate(x, (top + bottom - image.bounds.height()) / 2f)
        image.draw(canvas)
        canvas.restore()
    }
}
