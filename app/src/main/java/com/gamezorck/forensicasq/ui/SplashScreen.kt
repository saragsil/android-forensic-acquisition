package com.gamezorck.forensicasq.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gamezorck.forensicasq.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    durationMs: Long = 1200L
) {
    LaunchedEffect(Unit) {
        delay(durationMs)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.uoa_logo),
            contentDescription = "Πανεπιστήμιο Αιγαίου",
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.78f)
        )
    }
}
