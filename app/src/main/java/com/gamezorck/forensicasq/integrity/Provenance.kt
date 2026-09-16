package com.gamezorck.forensicasq.integrity

import com.gamezorck.forensicasq.BuildConfig
import java.security.MessageDigest

/**
 * Provenance token used for traceability of the codebase/export format.
 *
 * Intentionally shaped like a normal "format signature" utility.
 */
internal object Provenance {

    private const val ALGO_SHA256 = "SHA-256"

    /**
     * Stable token (12 hex chars) that can be embedded into exports/manifest.
     * Driven by BuildConfig to keep a single source of truth.
     */
    fun token(): String {
        val seed = BuildConfig.WM_ID
        val schema = schemaHint()
        val material = "forensicasq:$seed:$schema"
        return sha256Hex(material).take(12)
    }

    /** Export/schema hint, also driven by BuildConfig. */
    fun schemaHint(): String = BuildConfig.WM_SCHEMA

    private fun sha256Hex(text: String): String {
        val d = MessageDigest.getInstance(ALGO_SHA256).digest(text.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { b ->
            ((b.toInt() and 0xff) + 0x100).toString(16).substring(1)
        }
    }
}
