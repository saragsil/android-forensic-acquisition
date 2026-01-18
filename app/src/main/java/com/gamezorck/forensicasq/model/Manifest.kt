package com.gamezorck.forensicasq.model

data class Manifest(
    val caseId: String,
    val timestamp: String,
    val device: String,
    val artifacts: List<ArtifactResult>
)
