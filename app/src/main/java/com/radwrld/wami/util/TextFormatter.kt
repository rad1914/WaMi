// @path: app/src/main/java/com/radwrld/wami/util/TextFormatter.kt
package com.radwrld.wami.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.*
import android.text.style.*
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.radwrld.wami.R
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * A utility object for parsing Markdown-style text and converting it to a styled CharSequence.
 * Supports bold, italic, underline, strikethrough, links, inline code, code blocks,
 * blockquotes, and spoilers.
 */
object TextFormatter {

    /**
     * Formats the given text with Markdown-style spans.
     * @param context The context, used for resolving colors.
     * @param text The raw string to format.
     * @return A CharSequence with all formatting spans applied.
     */
    fun format(context: Context, text: String?): CharSequence {
        if (text.isNullOrEmpty()) {
            return ""
        }

        val spannable = SpannableStringBuilder(text)

        // The order of application is important to handle nested styles correctly.
        // We typically go from block-level formats to inline, and from longer markup to shorter.
        applyBlockquotes(context, spannable)
        applyCodeBlocks(context, spannable)
        applyInlineCode(context, spannable)
        applyLinks(spannable)
        applySpoilers(context, spannable)

        // Bold and Underline can be nested. The order here allows for combinations like **__text__**.
        applyStyle(spannable, Pattern.compile("\\*\\*(.*?)\\*\\*"), { StyleSpan(Typeface.BOLD) })
        applyStyle(spannable, Pattern.compile("__(.*?)__"), { UnderlineSpan() })
        // Italic with '*' must come after bold with '**' to avoid conflicts.
        applyStyle(spannable, Pattern.compile("\\*(.*?)\\*"), { StyleSpan(Typeface.ITALIC) })
        applyStyle(spannable, Pattern.compile("~~(.*?)~~"), { StrikethroughSpan() })

        return spannable
    }

    private fun applyStyle(spannable: SpannableStringBuilder, pattern: Pattern, spanBuilder: () -> Any) {
        val matches = mutableListOf<java.util.regex.MatchResult>()
        val matcher: Matcher = pattern.matcher(spannable)
        // Find all matches first to avoid issues with concurrent modification
        while (matcher.find()) {
            matches.add(matcher.toMatchResult())
        }

        // Apply spans from the end of the string to the beginning to keep indices valid
        for (match in matches.asReversed()) {
            // Safe to assume group 1 exists due to regex pattern (.*?)
            val fullMatchStart = match.start()
            val fullMatchEnd = match.end()
            val contentStart = match.start(1)
            val contentEnd = match.end(1)
            
            val markupLength = (fullMatchEnd - fullMatchStart - (contentEnd - contentStart)) / 2

            spannable.setSpan(spanBuilder(), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Delete the markup characters
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
        
        val codeBackgroundColor = ContextCompat.getColor(context, R.color.code_background)
        val codeTextColor = ContextCompat.getColor(context, R.color.code_text)

        for (match in matches.asReversed()) {
            val fullMatchStart = match.start()
            val contentStart = match.start(1)
            val contentEnd = match.end(1)

            // Prevent formatting inside already-formatted code blocks
            val existingSpans = spannable.getSpans(contentStart, contentEnd, TypefaceSpan::class.java)
            if (existingSpans.any { it.family == "monospace" }) continue

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
        
        val stripeColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.GRAY)

        for (match in matches.asReversed()) {
            val fullLineStart = match.start()
            val fullLineEnd = match.end()
            val marker = match.group(1) ?: ""

            spannable.setSpan(QuoteSpan(stripeColor, 10, 20), fullLineStart, fullLineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
        private val hiddenColor = ContextCompat.getColor(context, R.color.spoiler_background_hidden)
        private val revealedColor = ContextCompat.getColor(context, R.color.spoiler_background_revealed)

        override fun onClick(widget: View) {
            isRevealed = !isRevealed
            // Re-triggers updateDrawState by invalidating the view's text layout
            if (widget is TextView) {
                widget.text = widget.text
            }
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.bgColor = if (isRevealed) revealedColor else hiddenColor
            // Hides the text by matching its color to the background
            ds.color = if (isRevealed) ds.linkColor else hiddenColor
            ds.isUnderlineText = false
        }
    }
}
