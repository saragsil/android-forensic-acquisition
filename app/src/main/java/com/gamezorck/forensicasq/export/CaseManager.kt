package com.gamezorck.forensicasq.export

import android.content.Context
import com.gamezorck.forensicasq.logging.ChainOfCustody
import java.io.File

object CaseManager {

    private fun exportsRoot(context: Context): File {
        val root = File(context.getExternalFilesDir(null), "exports")
        if (!root.exists()) root.mkdirs()
        return root
    }

    fun sanitizeCaseName(input: String): String {
        val trimmed = input.trim()
        // safe folder name: letters, numbers, dot, dash, underscore
        val safe = trimmed.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return safe.take(40)
    }

    /**
     * Creates (if missing) or opens an existing case directory:
     * /Android/data/<pkg>/files/exports/<CaseName>/
     */
    fun openOrCreateCase(context: Context, caseName: String): File {
        val safeName = sanitizeCaseName(caseName)
        require(safeName.isNotBlank()) { "Case name cannot be blank." }

        val dir = File(exportsRoot(context), safeName)

        val existedBefore = dir.exists()
        if (!existedBefore) dir.mkdirs()

        // Chain of Custody
        if (!existedBefore) {
            ChainOfCustody.record(
                dir,
                "CASE_CREATED",
                "caseName=$safeName | path=${dir.absolutePath}"
            )
        } else {
            ChainOfCustody.record(
                dir,
                "CASE_OPENED",
                "caseName=$safeName | path=${dir.absolutePath}"
            )
        }

        return dir
    }

    /**
     * Lists existing cases (directories under exports/), newest first.
     */
    fun listCases(context: Context): List<File> {
        val root = exportsRoot(context)
        val dirs = root.listFiles()?.filter { it.isDirectory } ?: emptyList()
        return dirs.sortedByDescending { it.lastModified() }
    }

    fun deleteCase(caseDir: File): Boolean {
        val ok = caseDir.deleteRecursively()
        // δεν μπορούμε να γράψουμε custody μέσα στο case αφού διαγράφηκε,
        // οπότε αυτό το event καλύτερα να το γράφει το UI πριν το delete.
        return ok
    }
}
