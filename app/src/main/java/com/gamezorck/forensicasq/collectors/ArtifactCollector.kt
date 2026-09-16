package com.gamezorck.forensicasq.collectors

import android.content.Context
import com.gamezorck.forensicasq.model.ArtifactResult
import java.io.File

/**
 * Collects a single logical artifact and writes its output into [caseDir].
 *
 * Notes:
 * - Prefer deterministic output (stable ordering, stable paths) to aid verification.
 * - Output files should be written under [caseDir] and referenced by relative filename in [ArtifactResult].
 */
interface ArtifactCollector {

    /** Stable artifact identifier used in logs/manifests (e.g. "installed_apps"). */
    val artifactName: String

    /**
     * Performs logical acquisition of the artifact and writes output files to [caseDir].
     *
     * @return [ArtifactResult] describing success/failure and produced output file (if any).
     */
    fun collect(context: Context, caseDir: File): ArtifactResult
}
