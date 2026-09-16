package com.gamezorck.forensicasq.model

/**
 * Case-level metadata filled by the examiner.
 *
 * Note: The case folder name (case id) is handled separately by the CaseManager.
 */
data class CaseMeta(
    val caseName: String,
    val examinerName: String = "",
    val examinerPhone: String = "",
    val examinerEmail: String = "",
    val organization: String = "Not Specified",
    val notes: String = ""
)
