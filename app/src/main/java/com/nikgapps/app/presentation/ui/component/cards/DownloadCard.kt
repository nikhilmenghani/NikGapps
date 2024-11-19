package com.nikgapps.app.presentation.ui.component.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.App.Companion.globalClass
import com.nikgapps.R
import com.nikgapps.app.data.model.GappsVariantPreference
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.ui.component.items.PreferenceItem

@Composable
fun DownloadNikGappsCard() {
    val downloadPrefs = globalClass.downloadManager.downloadPrefs
    val dialog = globalClass.singleChoiceDialog
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            PreferenceItem(
                label = stringResource(R.string.gapps_variant),
                supportingText = when (downloadPrefs.gappsVariant) {
                    GappsVariantPreference.CORE.ordinal -> stringResource(R.string.core)
                    GappsVariantPreference.BASIC.ordinal -> stringResource(R.string.basic)
                    GappsVariantPreference.OMNI.ordinal -> stringResource(R.string.omni)
                    GappsVariantPreference.STOCK.ordinal -> stringResource(R.string.stock)
                    GappsVariantPreference.FULL.ordinal -> stringResource(R.string.full)
                    else -> stringResource(R.string.core)
                },
                icon = Icons.Rounded.Adb,
                onClick = {
                    dialog.show(
                        title = globalClass.getString(R.string.gapps_variant),
                        description = globalClass.getString(R.string.select_variant_preference),
                        choices = listOf(
                            globalClass.getString(R.string.core),
                            globalClass.getString(R.string.basic),
                            globalClass.getString(R.string.omni),
                            globalClass.getString(R.string.stock),
                            globalClass.getString(R.string.full)
                        ),
                        selectedChoice = downloadPrefs.gappsVariant,
                        onSelect = { downloadPrefs.gappsVariant = it }
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = {}) {
                    Text(text = stringResource(R.string.download))
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewDownloadNikGappsCard() {
    NikGappsThemePreview {
        DownloadNikGappsCard()
    }
}
