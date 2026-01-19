package com.gamezorck.forensicasq.logging

import com.gamezorck.forensicasq.util.TimeUtil
import java.io.File

object ChainOfCustody {

    private const val FILE_NAME = "chain_of_custody.txt"

    /**
     * Appends a custody event line to chain_of_custody.txt inside caseDir.
     * Format: [timestamp] EVENT | details
     */
    fun record(caseDir: File, event: String, details: String = "") {
        try {
            val f = File(caseDir, FILE_NAME)
            val ts = TimeUtil.nowReadable()
            val line = if (details.isBlank()) "[$ts] $event\n" else "[$ts] $event | $details\n"
            f.appendText(line)
        } catch (_: Exception) {
            // best-effort: do not crash app if custody logging fails
        }
    }
}
