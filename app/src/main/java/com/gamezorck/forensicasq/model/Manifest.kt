package com.gamezorck.forensicasq.model

data class Manifest(
    val caseId: String,
    val timestamp: String,
    val device: String,
    val artifacts: List<ArtifactResult>,
    val files: List<ManifestFile>
)

data class ManifestFile(
    /** Relative to caseDir (exports/<CaseName>/...) */
    val path: String,
    val size: Long,
    val sha256: String
)
