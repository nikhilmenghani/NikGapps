package com.nikgapps.app.presentation.ui.component.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikgapps.R
import com.nikgapps.app.utils.extensions.HapticFeedback.slightHapticFeedback

@Composable
fun PasteFromClipBoardButton(onPaste: (String) -> Unit = {}) {
    val clipboardManager = LocalClipboardManager.current
    PasteButton(onClick = {
        clipboardManager.getText()?.let { onPaste(it.toString()) }
    })
}

@Composable
fun PasteButton(onClick: () -> Unit = {}) {
    IconButton(onClick = onClick) {
        Icon(
            Icons.Outlined.ContentPaste,
            stringResource(R.string.paste)
        )
    }
}

@Composable
fun AddButton(onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(
        onClick = onClick, enabled = enabled
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(
                R.string.add
            )
        )
    }
}

@Composable
fun ClearButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = Icons.Outlined.Cancel,
            contentDescription = stringResource(id = R.string.clear),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    val view = LocalView.current
    IconButton(modifier = Modifier, onClick = {
        onClick()
        view.slightHapticFeedback()
    }) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.back),
        )
    }
}

@Composable
fun UpdateIconButton(versionNumber: String = "", onClick: () -> Unit) {
    Box(modifier = Modifier.size(48.dp)) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "New Update Available"
            )
        }
        if (!versionNumber.isEmpty()){
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(4.dp, (-4).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = versionNumber,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview
@Composable
fun UpdateIconButtonPreview() {
    Column{
        UpdateIconButton(versionNumber = "0.36", onClick = {})
        UpdateIconButton(onClick = {})
    }

}