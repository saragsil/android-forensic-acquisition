package com.gamezorck.forensicasq.model

/**
 * Result of a single artifact collection operation.
 *
 * Conventions:
 * - If [success] is true:
 *   - [error] should be null
 *   - [recordCount] >= 0
 * - If [success] is false:
 *   - [error] should contain a human-readable message
 *   - [recordCount] is typically 0
 */
data class ArtifactResult(
    val artifact: String,
    val success: Boolean,
    val recordCount: Int = 0,
    val outputFile: String? = null,
    val error: String? = null
) {

    companion object {

        fun success(
            artifact: String,
            recordCount: Int,
            outputFile: String? = null
        ): ArtifactResult =
            ArtifactResult(
                artifact = artifact,
                success = true,
                recordCount = recordCount,
                outputFile = outputFile,
                error = null
            )

        fun failure(
            artifact: String,
            error: String
        ): ArtifactResult =
            ArtifactResult(
                artifact = artifact,
                success = false,
                recordCount = 0,
                outputFile = null,
                error = error
            )
    }
}
