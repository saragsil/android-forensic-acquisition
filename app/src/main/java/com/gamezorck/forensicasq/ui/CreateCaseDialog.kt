package com.gamezorck.forensicasq.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gamezorck.forensicasq.model.CaseMeta
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCaseDialog(
    onDismiss: () -> Unit,
    onCreate: (CaseMeta) -> Unit,
    caseNameExists: (String) -> Boolean
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Dummy focus target so the cursor stops blinking (TextField truly loses focus)
    val dummyFocusRequester = remember { FocusRequester() }

    fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun hideImeHard() {
        // 1) Compose controller
        keyboard?.hide()

        // 2) WindowInsetsControllerCompat (reliable across many devices/IMEs)
        context.findActivity()?.let { activity ->
            WindowInsetsControllerCompat(activity.window, view)
                .hide(WindowInsetsCompat.Type.ime())
        }

        // 3) Fallback to IMM
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun clearUiFocusAndHideIme() {
        // Move focus away from any TextField FIRST -> cursor stops blinking
        runCatching { dummyFocusRequester.requestFocus() }

        // Extra safety
        focusManager.clearFocus(force = true)
        view.clearFocus()

        // Hide IME on next frame to prevent reopening
        scope.launch {
            awaitFrame()
            hideImeHard()
        }
    }

    // Required
    var caseName by remember { mutableStateOf("") }

    // Optional info
    var examinerName by remember { mutableStateOf("") }
    var examinerPhone by remember { mutableStateOf("") }
    var examinerEmail by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Organization dropdown
    val orgOptions = listOf("Not Specified", "Personal", "University", "Company", "Law Enforcement")
    var orgExpanded by remember { mutableStateOf(false) }
    var organization by remember { mutableStateOf(orgOptions.first()) }

    // ---- Validation (unique case name) ----
    val trimmedName = remember(caseName) { caseName.trim() }
    val exists = remember(trimmedName) { trimmedName.isNotBlank() && caseNameExists(trimmedName) }

    val caseNameError = trimmedName.isBlank() || exists
    val canCreate = !caseNameError

    AlertDialog(
        onDismissRequest = {
            clearUiFocusAndHideIme()
            onDismiss()
        },
        title = { Text("Create Case") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { clearUiFocusAndHideIme() })
                    }
            ) {
                // Invisible focus target so we can "park" focus away from TextFields
                Box(
                    modifier = Modifier
                        .size(0.dp)
                        .focusRequester(dummyFocusRequester)
                        .focusable()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = caseName,
                        onValueChange = { caseName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Case Name *") },
                        supportingText = {
                            when {
                                trimmedName.isBlank() -> Text("Case Name is required to continue.")
                                exists -> Text("This case name already exists. Choose another one.")
                            }
                        },
                        isError = caseNameError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { clearUiFocusAndHideIme() }
                        )
                    )

                    Divider()

                    Text("Optional Information", style = MaterialTheme.typography.titleSmall)

                    // ----- Examiner card -----
                    ElevatedCard {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Examiner", style = MaterialTheme.typography.titleSmall)

                            OutlinedTextField(
                                value = examinerName,
                                onValueChange = { examinerName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Name") },
                                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { clearUiFocusAndHideIme() }
                                )
                            )

                            OutlinedTextField(
                                value = examinerPhone,
                                onValueChange = { examinerPhone = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Phone") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { clearUiFocusAndHideIme() }
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = examinerEmail,
                                onValueChange = { examinerEmail = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Email") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { clearUiFocusAndHideIme() }
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 96.dp),
                                label = { Text("Notes") },
                                trailingIcon = {
                                    IconButton(
                                        modifier = Modifier.focusProperties { canFocus = false },
                                        onClick = { clearUiFocusAndHideIme() }
                                    ) {
                                        Icon(Icons.Outlined.Done, contentDescription = "Done")
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Default // multiline -> enter = newline
                                ),
                                singleLine = false,
                                maxLines = 4
                            )
                        }
                    }

                    // ----- Organization card -----
                    ElevatedCard {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Organization", style = MaterialTheme.typography.titleSmall)

                            ExposedDropdownMenuBox(
                                expanded = orgExpanded,
                                onExpandedChange = { orgExpanded = !orgExpanded }
                            ) {
                                OutlinedTextField(
                                    value = organization,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    label = { Text("Organization analysis is being done for") },
                                    leadingIcon = { Icon(Icons.Outlined.Business, contentDescription = null) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = orgExpanded)
                                    }
                                )

                                ExposedDropdownMenu(
                                    expanded = orgExpanded,
                                    onDismissRequest = { orgExpanded = false }
                                ) {
                                    orgOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                clearUiFocusAndHideIme()
                                                organization = opt
                                                orgExpanded = false
                                            }
                                        )
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
                enabled = canCreate,
                onClick = {
                    clearUiFocusAndHideIme()
                    onCreate(
                        CaseMeta(
                            caseName = trimmedName,
                            caseNumber = "",
                            examinerName = examinerName.trim(),
                            examinerPhone = examinerPhone.trim(),
                            examinerEmail = examinerEmail.trim(),
                            notes = notes.trim(),
                            organization = organization
                        )
                    )
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    clearUiFocusAndHideIme()
                    onDismiss()
                }
            ) { Text("Cancel") }
        }
    )
}
