
package com.example.sn_distributors.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    secondary = ForestGreenButton,
    background = SurfaceOffWhite,
    surface = SurfaceOffWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDarkPrimary,
    onSurface = TextDarkPrimary
)

@Composable
fun SN_DistributorsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
