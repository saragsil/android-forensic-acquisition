package com.gamezorck.forensicasq.export

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gamezorck.forensicasq.model.ArtifactResult
import java.io.File

object AppState {

    var currentCaseDir: File? by mutableStateOf(null)
        private set

    private val results = mutableListOf<ArtifactResult>()

    fun resetCase(caseDir: File) {
        currentCaseDir = caseDir
        results.clear()
    }

    fun clearCase() {
        currentCaseDir = null
        results.clear()
    }

    fun upsertResult(r: ArtifactResult) {
        val idx = results.indexOfFirst { it.artifact == r.artifact }
        if (idx >= 0) results[idx] = r else results.add(r)
    }

    fun getResults(): List<ArtifactResult> = results.toList()
}
