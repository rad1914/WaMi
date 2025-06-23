// @path: app/src/main/java/com/radwrld/wami/ui/TextFormatter.kt
package com.radwrld.wami.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.*
import android.text.style.*
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.radwrld.wami.R
import java.util.regex.Matcher
import java.util.regex.Pattern

object TextFormatter {

    fun format(context: Context, text: String?): CharSequence {
        if (text.isNullOrEmpty()) {
            return ""
        }

        val spannable = SpannableStringBuilder(text)

        applyBlockquotes(context, spannable)
        applyCodeBlocks(context, spannable)
        applyInlineCode(context, spannable)
        applyLinks(spannable)
        applySpoilers(context, spannable)

        applyStyle(spannable, Pattern.compile("\\*\\*(.*?)\\*\\*")) { StyleSpan(Typeface.BOLD) }
        applyStyle(spannable, Pattern.compile("__(.*?)__")) { UnderlineSpan() }
        applyStyle(spannable, Pattern.compile("\\*(.*?)\\*")) { StyleSpan(Typeface.ITALIC) }
        applyStyle(spannable, Pattern.compile("~~(.*?)~~")) { StrikethroughSpan() }

        return spannable
    }

    private fun applyStyle(spannable: SpannableStringBuilder, pattern: Pattern, spanBuilder: () -> Any) {
        val matches = mutableListOf<java.util.regex.MatchResult>()
        val matcher: Matcher = pattern.matcher(spannable)
        while (matcher.find()) {
            matches.add(matcher.toMatchResult())
        }

        for (match in matches.asReversed()) {
            val fullMatchStart = match.start()
            val fullMatchEnd = match.end()
            val contentStart = match.start(1)
            val contentEnd = match.end(1)

            val markupLength = (fullMatchEnd - fullMatchStart - (contentEnd - contentStart)) / 2

            spannable.setSpan(spanBuilder(), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            spannable.delete(contentEnd, contentEnd + markupLength)
            spannable.delete(fullMatchStart, fullMatchStart + markupLength)
        }
    }

    private fun applyCode(context: Context, spannable: SpannableStringBuilder, pattern: Pattern, markupLength: Int) {
        val matches = mutableListOf<java.util.regex.MatchResult>()
        val matcher: Matcher = pattern.matcher(spannable)
        while (matcher.find()) {
            matches.add(matcher.toMatchResult())
        }

        val codeBackgroundColor = MaterialColors.getColor(context, MaterialR.attr.colorSurfaceContainerHighest, Color.GRAY)
        val codeTextColor = MaterialColors.getColor(context, MaterialR.attr.colorOnSurfaceVariant, Color.BLACK)

        for (match in matches.asReversed()) {
            val fullMatchStart = match.start()
            val contentStart = match.start(1)
            val contentEnd = match.end(1)

            val existingSpans = spannable.getSpans(contentStart, contentEnd, Any::class.java)
            if (existingSpans.any { it is TypefaceSpan && it.family == "monospace" }) continue

            spannable.setSpan(TypefaceSpan("monospace"), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(BackgroundColorSpan(codeBackgroundColor), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(codeTextColor), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            spannable.delete(contentEnd, contentEnd + markupLength)
            spannable.delete(fullMatchStart, fullMatchStart + markupLength)
        }
    }

    private fun applyInlineCode(context: Context, spannable: SpannableStringBuilder) {
        applyCode(context, spannable, Pattern.compile("`(.*?)`"), 1)
    }

    private fun applyCodeBlocks(context: Context, spannable: SpannableStringBuilder) {
        applyCode(context, spannable, Pattern.compile("```(.*?)```", Pattern.DOTALL), 3)
    }
    
    private fun applyLinks(spannable: SpannableStringBuilder) {
        val pattern = Pattern.compile("\\[(.*?)]\\((.*?)\\)")
        val matches = mutableListOf<java.util.regex.MatchResult>()
        val matcher: Matcher = pattern.matcher(spannable)
        while (matcher.find()) {
            matches.add(matcher.toMatchResult())
        }

        for (match in matches.asReversed()) {
            val text = match.group(1) ?: continue
            val url = match.group(2) ?: continue
            val start = match.start()
            val end = match.end()

            spannable.replace(start, end, text)

            spannable.setSpan(URLSpan(url), start, start + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyBlockquotes(context: Context, spannable: SpannableStringBuilder) {
        val pattern = Pattern.compile("^(> ?)(.*)", Pattern.MULTILINE)
        val matches = mutableListOf<java.util.regex.MatchResult>()
        val matcher: Matcher = pattern.matcher(spannable)
        while (matcher.find()) {
            matches.add(matcher.toMatchResult())
        }

        val stripeColor = MaterialColors.getColor(context, MaterialR.attr.colorOutline, Color.GRAY)
        val stripeWidth = 10
        val gapWidth = 20   

        for (match in matches.asReversed()) {
            val fullLineStart = match.start()
            val fullLineEnd = match.end()
            val marker = match.group(1) ?: ""

            spannable.setSpan(QuoteSpan(stripeColor, stripeWidth, gapWidth), fullLineStart, fullLineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.ITALIC), fullLineStart, fullLineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            spannable.delete(fullLineStart, fullLineStart + marker.length)
        }
    }

    private fun applySpoilers(context: Context, spannable: SpannableStringBuilder) {
        val pattern = Pattern.compile("\\|\\|(.*?)\\|\\|", Pattern.DOTALL)
        val matches = mutableListOf<java.util.regex.MatchResult>()
        val matcher: Matcher = pattern.matcher(spannable)
        while (matcher.find()) {
            matches.add(matcher.toMatchResult())
        }

        for (match in matches.asReversed()) {
            val start = match.start()
            val end = match.end()
            val contentStart = match.start(1)
            val contentEnd = match.end(1)

            spannable.setSpan(SpoilerSpan(context), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            spannable.delete(contentEnd, end)
            spannable.delete(start, contentStart)
        }
    }

    private class SpoilerSpan(context: Context) : ClickableSpan() {
        private var isRevealed = false

        private val hiddenColor = MaterialColors.getColor(context, MaterialR.attr.colorSurfaceVariant, Color.DKGRAY)
        private val revealedColor = Color.TRANSPARENT

        override fun onClick(widget: View) {
            isRevealed = !isRevealed

            widget.invalidate()
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
            if (isRevealed) {
                ds.bgColor = revealedColor

            } else {

                ds.bgColor = hiddenColor
                ds.color = hiddenColor
            }
        }
    }
}
