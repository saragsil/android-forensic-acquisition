package com.gamezorck.forensicasq.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gamezorck.forensicasq.export.CaseManager
import com.gamezorck.forensicasq.export.CaseMetaWriter
import com.gamezorck.forensicasq.model.CaseMeta
import java.io.File
import kotlinx.coroutines.launch

private enum class CaseDialog { None, Create, Open, Delete, DeleteConfirm }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseScreen(
    onOpenCase: (caseDir: File) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun clearUiFocus() {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    // Cases list
    var cases by remember { mutableStateOf<List<File>>(emptyList()) }
    fun refreshCases() { cases = CaseManager.listCases(context) }
    LaunchedEffect(Unit) { refreshCases() }

    // Navigation to settings screen (kept as-is)
    var showSettings by remember { mutableStateOf(false) }
    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    // Dialog + selection state
    var activeDialog by remember { mutableStateOf(CaseDialog.None) }
    var openSelected by remember { mutableStateOf<File?>(null) }
    var deleteSelected by remember { mutableStateOf<File?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Case Management") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { refreshCases() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        // Tap background to dismiss focus/keyboard
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) { detectTapGestures(onTap = { clearUiFocus() }) }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text("Choose an action", style = MaterialTheme.typography.titleMedium)

                ActionBox(
                    title = "Create new case",
                    subtitle = "Create a new case with metadata (optional fields)",
                    icon = Icons.Outlined.AddBox,
                    onClick = {
                        clearUiFocus()
                        activeDialog = CaseDialog.Create
                    }
                )

                ActionBox(
                    title = "Open existing case",
                    subtitle = "Select a case and continue to dashboard",
                    icon = Icons.Outlined.FolderOpen,
                    onClick = {
                        clearUiFocus()
                        refreshCases()
                        openSelected = null
                        activeDialog = CaseDialog.Open
                    }
                )

                ActionBox(
                    title = "Delete case",
                    subtitle = "Permanently remove a case (with confirmation)",
                    icon = Icons.Outlined.Delete,
                    onClick = {
                        clearUiFocus()
                        refreshCases()
                        deleteSelected = null
                        activeDialog = CaseDialog.Delete
                    }
                )

                HorizontalDivider()

                RecentCasesPreview(cases = cases)
            }
        }
    }

    // ---------------- CREATE CASE DIALOG ----------------
    if (activeDialog == CaseDialog.Create) {
        CreateCaseDialog(
            onDismiss = {
                clearUiFocus()
                activeDialog = CaseDialog.None
            },
            caseNameExists = { name -> CaseManager.caseExists(context, name) },
            onCreate = { meta: CaseMeta ->
                try {
                    val dir = CaseManager.createNewCase(context, meta.caseName)
                    CaseMetaWriter.write(dir, meta)
                    refreshCases()
                    clearUiFocus()
                    activeDialog = CaseDialog.None
                    onOpenCase(dir)
                } catch (e: IllegalArgumentException) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            e.message ?: "Case name already exists."
                        )
                    }
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Failed to create case: ${e.message ?: "Unknown error"}"
                        )
                    }
                }
            }
        )
    }

    // ---------------- OPEN EXISTING CASE DIALOG ----------------
    if (activeDialog == CaseDialog.Open) {
        CasePickerDialog(
            title = "Open existing case",
            emptyText = "No cases available.",
            cases = cases,
            selected = openSelected,
            icon = Icons.Outlined.Folder,
            selectedContainer = MaterialTheme.colorScheme.secondaryContainer,
            unselectedContainer = MaterialTheme.colorScheme.surfaceVariant,
            onDismiss = {
                clearUiFocus()
                activeDialog = CaseDialog.None
            },
            onSelect = {
                clearUiFocus()
                openSelected = it
            },
            confirmText = "Open",
            confirmEnabled = openSelected != null,
            onConfirm = {
                clearUiFocus()
                val chosen = openSelected ?: return@CasePickerDialog
                activeDialog = CaseDialog.None
                onOpenCase(chosen)
            }
        )
    }

    // ---------------- DELETE CASE DIALOG ----------------
    if (activeDialog == CaseDialog.Delete) {
        CasePickerDialog(
            title = "Delete case",
            headerText = "Select a case to delete. This action is permanent.",
            emptyText = "No cases available.",
            cases = cases,
            selected = deleteSelected,
            icon = Icons.Outlined.Delete,
            selectedContainer = MaterialTheme.colorScheme.errorContainer,
            unselectedContainer = MaterialTheme.colorScheme.surfaceVariant,
            onDismiss = {
                clearUiFocus()
                activeDialog = CaseDialog.None
            },
            onSelect = {
                clearUiFocus()
                deleteSelected = it
            },
            confirmText = "Delete",
            confirmEnabled = deleteSelected != null,
            confirmColors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            onConfirm = {
                clearUiFocus()
                if (deleteSelected != null) activeDialog = CaseDialog.DeleteConfirm
            }
        )
    }

    // ---------------- DELETE CONFIRM DIALOG ----------------
    if (activeDialog == CaseDialog.DeleteConfirm) {
        val target = deleteSelected
        AlertDialog(
            onDismissRequest = {
                clearUiFocus()
                activeDialog = CaseDialog.Delete
            },
            title = { Text("Confirm deletion") },
            text = {
                Text(
                    "Are you sure you want to delete:\n${target?.name.orEmpty()}\n\nThis cannot be undone.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        clearUiFocus()
                        if (target != null) {
                            CaseManager.deleteCase(target)
                            refreshCases()
                        }
                        deleteSelected = null
                        activeDialog = CaseDialog.None
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Yes, delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clearUiFocus()
                        activeDialog = CaseDialog.Delete
                    }
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RecentCasesPreview(cases: List<File>) {
    if (cases.isNotEmpty()) {
        Text("Existing cases: ${cases.size}", style = MaterialTheme.typography.bodySmall)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(cases.take(6)) { c ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(c.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            c.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    } else {
        Text("No cases found yet.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CasePickerDialog(
    title: String,
    cases: List<File>,
    selected: File?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedContainer: androidx.compose.ui.graphics.Color,
    unselectedContainer: androidx.compose.ui.graphics.Color,
    emptyText: String,
    onDismiss: () -> Unit,
    onSelect: (File) -> Unit,
    confirmText: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    headerText: String? = null,
    confirmColors: ButtonColors = ButtonDefaults.buttonColors()
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (cases.isEmpty()) {
                Text(emptyText, style = MaterialTheme.typography.bodySmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (headerText != null) {
                        Text(headerText, style = MaterialTheme.typography.bodySmall)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(cases) { c ->
                            val isSelected = selected?.absolutePath == c.absolutePath
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (isSelected) selectedContainer else unselectedContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(c) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, contentDescription = null)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            c.absolutePath,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = confirmColors
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ActionBox(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}
