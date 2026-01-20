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

    /** Returns true if a case directory already exists for this name (after sanitize). */
    fun caseExists(context: Context, caseName: String): Boolean {
        val safe = sanitizeCaseName(caseName)
        if (safe.isBlank()) return false
        return File(exportsRoot(context), safe).exists()
    }

    /**
     * Creates a NEW case directory. If it already exists -> throws.
     * Path: /Android/data/<pkg>/files/exports/<CaseName>/
     */
    fun createNewCase(context: Context, caseName: String): File {
        val safeName = sanitizeCaseName(caseName)
        require(safeName.isNotBlank()) { "Case name cannot be blank." }

        val dir = File(exportsRoot(context), safeName)
        require(!dir.exists()) { "Case name already exists." }

        dir.mkdirs()

        ChainOfCustody.record(
            dir,
            "CASE_CREATED",
            "caseName=$safeName | path=${dir.absolutePath}"
        )

        return dir
    }

    /**
     * Opens an EXISTING case directory. If it does not exist -> throws.
     * Path: /Android/data/<pkg>/files/exports/<CaseName>/
     */
    fun openExistingCase(context: Context, caseName: String): File {
        val safeName = sanitizeCaseName(caseName)
        require(safeName.isNotBlank()) { "Case name cannot be blank." }

        val dir = File(exportsRoot(context), safeName)
        require(dir.exists() && dir.isDirectory) { "Case does not exist." }

        ChainOfCustody.record(
            dir,
            "CASE_OPENED",
            "caseName=$safeName | path=${dir.absolutePath}"
        )

        return dir
    }

    /**
     * Old behavior: create if missing, else open.
     * Keep it only if you need it in older code paths.
     */
    @Deprecated(
        message = "Use createNewCase() or openExistingCase() to enforce unique names correctly.",
        replaceWith = ReplaceWith("createNewCase(context, caseName)")
    )
    fun openOrCreateCase(context: Context, caseName: String): File {
        val safeName = sanitizeCaseName(caseName)
        require(safeName.isNotBlank()) { "Case name cannot be blank." }

        val dir = File(exportsRoot(context), safeName)

        val existedBefore = dir.exists()
        if (!existedBefore) dir.mkdirs()

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
        // Δεν γράφουμε custody εδώ γιατί ο φάκελος διαγράφεται.
        // Το UI μπορεί να γράφει "CASE_DELETED" πριν το delete, αν θέλετε.
        return ok
    }
}
