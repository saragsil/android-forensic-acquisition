package com.gamezorck.forensicasq.ui

import androidx.compose.ui.graphics.vector.ImageVector

data class ActionCard(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val status: ActionStatus = ActionStatus.NotRun,
    val count: Int? = null
)

enum class ActionStatus { NotRun, Ok, Error }
