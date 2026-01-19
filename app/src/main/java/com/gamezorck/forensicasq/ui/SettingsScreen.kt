package com.gamezorck.forensicasq.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gamezorck.forensicasq.util.PermissionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val permissions = remember {
        arrayOf(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.READ_SMS
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    val okContacts = PermissionUtil.hasReadContacts(context)
    val okCalls = PermissionUtil.hasReadCallLog(context)
    val okSms = PermissionUtil.hasReadSms(context)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Permissions", style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        "Grant permissions to allow logical acquisition from the emulator.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text("READ_CONTACTS: ${if (okContacts) "GRANTED ✅" else "DENIED ❌"}", style = MaterialTheme.typography.bodySmall)
                    Text("READ_CALL_LOG: ${if (okCalls) "GRANTED ✅" else "DENIED ❌"}", style = MaterialTheme.typography.bodySmall)
                    Text("READ_SMS: ${if (okSms) "GRANTED ✅" else "DENIED ❌"}", style = MaterialTheme.typography.bodySmall)

                    Button(
                        onClick = { launcher.launch(permissions) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Request permissions") }
                }
            }
        }
    }
}
