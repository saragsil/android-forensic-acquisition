package com.gamezorck.forensicasq.export

import android.content.Context
import com.gamezorck.forensicasq.logging.ChainOfCustody
import java.io.File

object CaseManager {

    private const val EXPORTS_DIR = "exports"
    private const val MAX_CASE_NAME_LEN = 40

    // If you want to disallow digits in case names, set this to false.
    private const val ALLOW_DIGITS_IN_CASE_NAME = true

    private val INVALID_CHARS_REGEX =
        if (ALLOW_DIGITS_IN_CASE_NAME) Regex("[^a-zA-Z0-9._-]+")
        else Regex("[^a-zA-Z._-]+")

    private val MULTI_UNDERSCORE_REGEX = Regex("_+")

    private fun exportsRoot(context: Context): File {
        val base = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("External files dir is not available.")

        val root = File(base, EXPORTS_DIR)
        if (!root.exists() && !root.mkdirs()) {
            throw IllegalStateException("Failed to create exports directory: ${root.absolutePath}")
        }
        return root
    }

    /**
     * Produces a safe folder name:
     * - trims
     * - replaces invalid chars with "_"
     * - collapses multiple underscores
     * - trims leading/trailing underscores/dots/dashes
     * - limits length
     */
    fun sanitizeCaseName(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ""

        val replaced = trimmed.replace(INVALID_CHARS_REGEX, "_")
        val collapsed = replaced.replace(MULTI_UNDERSCORE_REGEX, "_")
        val cleanedEdges = collapsed.trim { it == '_' || it == '.' || it == '-' }

        return cleanedEdges.take(MAX_CASE_NAME_LEN)
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
        if (!dir.mkdirs()) {
            throw IllegalStateException("Failed to create case directory: ${dir.absolutePath}")
        }

        recordCaseEvent(dir, "CASE_CREATED", safeName)
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

        recordCaseEvent(dir, "CASE_OPENED", safeName)
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

        if (!existedBefore && !dir.mkdirs()) {
            throw IllegalStateException("Failed to create case directory: ${dir.absolutePath}")
        }

        recordCaseEvent(dir, if (existedBefore) "CASE_OPENED" else "CASE_CREATED", safeName)
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
        // Note: no custody record here because the folder is deleted.
        // If desired, record "CASE_DELETED" BEFORE calling this.
        return caseDir.deleteRecursively()
    }

    private fun recordCaseEvent(caseDir: File, event: String, safeName: String) {
        ChainOfCustody.record(
            caseDir,
            event,
            "caseName=$safeName | path=${caseDir.absolutePath}"
        )
    }
}
