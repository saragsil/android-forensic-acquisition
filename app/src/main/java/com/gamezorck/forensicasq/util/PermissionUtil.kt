package com.gamezorck.forensicasq.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Centralized permission helpers for forensic artifact acquisition.
 *
 * NOTE:
 * - Requesting permissions is handled via ActivityResultLauncher in UI.
 * - This utility ONLY checks permission state.
 */
object PermissionUtil {

    /** Permissions required for full logical acquisition */
    val requiredPermissions = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS
    )

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    fun hasReadContacts(context: Context): Boolean =
        hasPermission(context, Manifest.permission.READ_CONTACTS)

    fun hasReadCallLog(context: Context): Boolean =
        hasPermission(context, Manifest.permission.READ_CALL_LOG)

    fun hasReadSms(context: Context): Boolean =
        hasPermission(context, Manifest.permission.READ_SMS)

    /** True if ALL required permissions are granted */
    fun hasAllRequiredPermissions(context: Context): Boolean =
        requiredPermissions.all { hasPermission(context, it) }
}
