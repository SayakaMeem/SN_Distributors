package com.example.sn_distributors.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A bounded viewport shared by every route, including future screens. */
@Composable
fun ScreenViewport(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), content = content)
}

/** Dialog actions stay fixed while long form bodies scroll within the available height. */
@Composable
fun ScrollableDialogBody(
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = verticalArrangement,
        content = content
    )
}
