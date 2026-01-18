package com.gamezorck.forensicasq.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ForensicHomeScreen()
            }
        }
    }
}

@Composable
fun ForensicHomeScreen() {
    var status by remember { mutableStateOf("[Init] App started.") }
    val scrollState = rememberScrollState()

    fun log(msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        status += "\n[$ts] $msg"
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Forensic Acquisition (Emulator)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Button(
                onClick = { log("Start/Create Case clicked (TODO).") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start / Create Case") }

            Button(
                onClick = { log("Collect Contacts clicked (TODO).") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Collect Contacts") }

            Button(
                onClick = { log("Collect Call Logs clicked (TODO).") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Collect Call Logs") }

            Button(
                onClick = { log("Collect SMS clicked (TODO).") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Collect SMS (optional)") }

            Button(
                onClick = { log("Collect Installed Apps clicked (TODO).") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Collect Installed Apps") }

            Button(
                onClick = { log("Collect Device Info clicked (TODO).") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Collect Device Info") }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { log("Generate Export Pack clicked (TODO).") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Generate Export Pack (manifest + hashes)") }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = status,
                onValueChange = { /* read-only */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp),
                readOnly = true
            )
        }
    }
}
