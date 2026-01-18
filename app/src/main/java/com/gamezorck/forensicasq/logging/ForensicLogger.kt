package com.gamezorck.forensicasq.logging

import com.gamezorck.forensicasq.util.TimeUtil
import java.io.File

class ForensicLogger(private val caseDir: File) {

    private val logFile = File(caseDir, "logs.txt")

    fun log(message: String) {
        val line = "[${TimeUtil.nowReadable()}] $message\n"
        logFile.appendText(line)
    }
}
