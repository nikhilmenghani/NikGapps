package com.nikgapps.app.presentation.ui.component.cards


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.app.presentation.theme.NikGappsTheme


@Composable
fun PermissionsCard(
    title: String = "Test Permission",
    description: String = "This is a test permission",
    isPermissionGranted: Boolean = false,
    onRequestPermission: () -> Unit,
    permissionsText: String = "Permission"
) {
    val backgroundColor = if (isPermissionGranted) Color(0xFFB9F6CA) else Color(0xFFF36170)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color.Black
            )
            Divider(color = Color.Gray, thickness = 1.dp)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.Black
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(permissionsText)
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview
@Composable
fun PreviewSamplePermissionsManagerCard() {
    NikGappsTheme {
        PermissionsCard(
            title = "Test Permission",
            description = "This is a test permission",
            isPermissionGranted = false,
            onRequestPermission = { },
            permissionsText = "Request Permission"
        )
    }
}
