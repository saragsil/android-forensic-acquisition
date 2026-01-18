package com.gamezorck.forensicasq.collectors

import android.content.Context
import com.gamezorck.forensicasq.model.ArtifactResult
import java.io.File

interface ArtifactCollector {

    val artifactName: String

    /**
     * Performs logical acquisition of the artifact
     * and writes output files to the given case directory.
     */
    fun collect(
        context: Context,
        caseDir: File
    ): ArtifactResult
}
