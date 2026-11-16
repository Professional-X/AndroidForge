package com.xivox.player

import com.xivox.player.data.subtitle.SubtitleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleParserTest {

    @Test
    fun parsesSrtAndFindsCueWithDelay() {
        val srt = """
            1
            00:00:01,000 --> 00:00:04,000
            <i>Beyond Playback.</i>

            2
            00:00:05,500 --> 00:00:08,000
            Second line
            with wrap
        """.trimIndent()

        val cues = SubtitleParser.parseText(srt, "demo.srt")
        assertEquals(2, cues.size)
        assertEquals("Beyond Playback.", cues[0].text)
        assertEquals(1000L, cues[0].startMs)
        assertEquals(4000L, cues[0].endMs)

        // Exact cue match
        val hit = SubtitleParser.findActiveCue(cues, positionMs = 2000L, delayMs = 0L)
        assertEquals("Beyond Playback.", hit?.text)

        // With +1500ms subtitle delay, cue originally at 1000..4000 appears at 2500..5500
        val delayedMiss = SubtitleParser.findActiveCue(cues, positionMs = 2000L, delayMs = 1500L)
        assertNull(delayedMiss)

        val delayedHit = SubtitleParser.findActiveCue(cues, positionMs = 3000L, delayMs = 1500L)
        assertEquals("Beyond Playback.", delayedHit?.text)
    }

    @Test
    fun parsesWebVttAndAssFormats() {
        val vtt = """
            WEBVTT

            00:02.000 --> 00:05.500 align:middle
            Hello VTT
        """.trimIndent()
        val vttCues = SubtitleParser.parseText(vtt, "sample.vtt")
        assertEquals(1, vttCues.size)
        assertEquals("Hello VTT", vttCues[0].text)

        val ass = """
            [Script Info]
            Title: Test

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:10.50,0:00:14.00,Default,,0,0,0,,{\pos(320,240)}Line1\NLine2
        """.trimIndent()
        val assCues = SubtitleParser.parseText(ass, "sample.ass")
        assertEquals(1, assCues.size)
        assertEquals("Line1\nLine2", assCues[0].text)
        assertEquals(10_500L, assCues[0].startMs)
        assertEquals(14_000L, assCues[0].endMs)
    }
}
