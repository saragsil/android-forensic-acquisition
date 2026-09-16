package com.gamezorck.forensicasq.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Time utilities for forensic logging and exports.
 *
 * All timestamps are generated in the device's local time zone,
 * but formatted deterministically.
 */
object TimeUtil {

    private val LOCAL_ZONE: ZoneId = ZoneId.systemDefault()

    private val FOLDER_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    private val READABLE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** Timestamp suitable for folder/file names */
    fun nowForFolder(): String =
        ZonedDateTime.ofInstant(Instant.now(), LOCAL_ZONE)
            .format(FOLDER_FORMAT)

    /** Human-readable timestamp for logs, custody, manifests */
    fun nowReadable(): String =
        ZonedDateTime.ofInstant(Instant.now(), LOCAL_ZONE)
            .format(READABLE_FORMAT)
}
