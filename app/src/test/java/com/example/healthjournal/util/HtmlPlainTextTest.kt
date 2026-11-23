package com.example.healthjournal.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlPlainTextTest {

    @Test
    fun stripsStandardAndMalformedParagraphAndBreakTags() {
        assertEquals("Text", HtmlPlainText.toPlainText("<p>Text</p><br>"))
        assertEquals("Text", HtmlPlainText.toPlainText("<p>Text</><br>"))
        assertEquals("Line one Line two", HtmlPlainText.toPlainText("<p>Line one</p><p>Line two</p>"))
    }

    @Test
    fun decodesCommonAndNumericEntities() {
        assertEquals(
            "Tom & Jerry <3 \"quoted\" 'ok'",
            HtmlPlainText.toPlainText("<p>Tom &amp; Jerry &lt;3 &quot;quoted&quot; &#39;ok&#39;</p>")
        )
        assertEquals("Hello world", HtmlPlainText.toPlainText("Hello&nbsp;&nbsp;world"))
        assertEquals("A", HtmlPlainText.toPlainText("&#65;"))
        assertEquals("B", HtmlPlainText.toPlainText("&#x42;"))
    }

    @Test
    fun returnsEmptyForBlankOrMarkupOnlyHtml() {
        assertEquals("", HtmlPlainText.toPlainText(""))
        assertEquals("", HtmlPlainText.toPlainText("   \n\t  "))
        assertEquals("", HtmlPlainText.toPlainText("<p><br></p>&nbsp;"))
    }

    @Test
    fun preservesPlainTextWithoutTags() {
        assertEquals("Regular note 123", HtmlPlainText.toPlainText("  Regular   note\n123 "))
    }

    @Test
    fun decodesEditorEncodedPunctuation() {
        assertEquals("Run #Fitness!", HtmlPlainText.toPlainText("<p>Run &num;Fitness&excl;</p>"))
        assertEquals(
            "What? a.b,c:d; (x) [y] {z} 50% $5 *+= /\\ @user ^up `tick` |bar| _under_ -dash- ~tilde~",
            HtmlPlainText.toPlainText(
                "<p>What&quest; a&period;b&comma;c&colon;d&semi; &lpar;x&rpar; &lsqb;y&rsqb; " +
                    "&lcub;z&rcub; 50&percnt; &dollar;5 &ast;&plus;&equals; &sol;&bsol; " +
                    "&commat;user &Hat;up &grave;tick&grave; &verbar;bar&vert; " +
                    "&lowbar;under&lowbar; &hyphen;dash&dash; &tilde;tilde&tilde;</p>"
            )
        )
        assertEquals("&lt;escaped&gt;", HtmlPlainText.toPlainText("&amp;lt;escaped&amp;gt;"))
    }
}
