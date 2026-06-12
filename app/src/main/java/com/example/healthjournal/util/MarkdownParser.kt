package com.example.healthjournal.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

object MarkdownParser {
    fun parse(markdown: String): AnnotatedString {
        return buildAnnotatedString {
            
            // Very simple parsing for now as per test requirements
            if (markdown.startsWith("**") && markdown.endsWith("**")) {
                val content = markdown.substring(2, markdown.length - 2)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(content)
                }
            } else if (markdown.startsWith("*") && markdown.endsWith("*")) {
                val content = markdown.substring(1, markdown.length - 1)
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(content)
                }
            } else if (markdown.startsWith("# ")) {
                val content = markdown.substring(2)
                withStyle(style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)) {
                    append(content)
                }
            } else {
                append(markdown)
            }
        }
    }
}
