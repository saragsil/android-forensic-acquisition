package com.gamezorck.forensicasq.export

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gamezorck.forensicasq.model.ArtifactResult
import java.io.File

object AppState {

    var currentCaseDir: File? by mutableStateOf(null)
        private set

    private val results = mutableStateListOf<ArtifactResult>()

    fun resetCase(caseDir: File) {
        currentCaseDir = caseDir
        results.clear()
    }

    fun clearCase() {
        currentCaseDir = null
        results.clear()
    }

    fun upsertResult(result: ArtifactResult) {
        val idx = results.indexOfFirst { it.artifact == result.artifact }
        if (idx >= 0) {
            results[idx] = result
        } else {
            results.add(result)
        }
    }

    fun getResults(): List<ArtifactResult> = results.toList()
}
