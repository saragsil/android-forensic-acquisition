package com.gamezorck.forensicasq.model

data class ArtifactResult(
    val artifact: String,
    val success: Boolean,
    val recordCount: Int = 0,
    val outputFile: String? = null,
    val error: String? = null
)
