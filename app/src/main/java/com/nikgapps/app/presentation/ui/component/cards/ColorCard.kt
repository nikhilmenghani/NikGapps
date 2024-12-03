package com.nikgapps.app.presentation.ui.component.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.app.presentation.theme.NikGappsThemePreview

@Composable
fun ColorCard(color: androidx.compose.ui.graphics.Color, onColor: androidx.compose.ui.graphics.Color, label: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = onColor
            )
        }
    }
}

@Composable
fun ColorPalettePreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        ColorCard(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, "Primary / OnPrimary")
        ColorCard(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "PrimaryContainer / OnPrimaryContainer")
        ColorCard(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, "Secondary / OnSecondary")
        ColorCard(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "SecondaryContainer / OnSecondaryContainer")
        ColorCard(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary, "Tertiary / OnTertiary")
        ColorCard(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "TertiaryContainer / OnTertiaryContainer")
        ColorCard(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onBackground, "Background / OnBackground")
        ColorCard(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, "Surface / OnSurface")
        ColorCard(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "SurfaceVariant / OnSurfaceVariant")
        ColorCard(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError, "Error / OnError")
        ColorCard(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "ErrorContainer / OnErrorContainer")
    }
}

@Preview
@Composable
fun DynamicColorPreview() {
    NikGappsThemePreview (useDynamicColor = true) {
        ColorPalettePreview()
    }
}

@Preview
@Composable
fun DarkColorPreview() {
    NikGappsThemePreview (useDynamicColor = false) {
        ColorPalettePreview()
    }
}