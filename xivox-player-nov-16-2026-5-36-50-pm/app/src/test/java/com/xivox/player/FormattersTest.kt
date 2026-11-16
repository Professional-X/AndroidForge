package com.xivox.player

import com.xivox.player.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun formatsDurationsCorrectly() {
        assertEquals("00:00", Formatters.formatDurationMs(0L))
        assertEquals("01:05", Formatters.formatDurationMs(65_000L))
        assertEquals("1:01:05", Formatters.formatDurationMs(3_665_000L))
    }

    @Test
    fun formatsSizesCorrectly() {
        assertEquals("0 B", Formatters.formatFileSize(0L))
        assertEquals("512 KB", Formatters.formatFileSize(512L * 1024L))
        assertEquals("1.5 MB", Formatters.formatFileSize((1.5 * 1024 * 1024).toLong()))
    }
}
