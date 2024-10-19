package com.nikgapps.ui.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.ui.theme.NikGappsThemePreview
import com.nikgapps.R
import com.nikgapps.ui.components.items.PreferenceSubtitle

@Composable
fun OutlinedButtonWithIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
    )
    {
        Icon(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            imageVector = icon,
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = text
        )
    }
}

@Composable
fun TextButtonWithIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
    )
    {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = icon,
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = text
            )
        }

    }
}

@Composable
fun FilledTonalButtonWithIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        colors = colors
    )
    {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = text
        )
    }
}

@Composable
fun FilledButtonWithIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        enabled = enabled
    )
    {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = text
        )
    }
}

@Composable
fun ConfirmButton(
    text: String = stringResource(R.string.confirm),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(text)
    }
}

@Composable
fun DismissButton(text: String = stringResource(R.string.dismiss), onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text)
    }
}

@Composable
fun LinkButton(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.confirm),
    icon: ImageVector = Icons.AutoMirrored.Outlined.OpenInNew,
    link: String = "Test"
) {
    val uriHandler = LocalUriHandler.current
    TextButtonWithIcon(
        modifier = modifier,
        onClick = { uriHandler.openUri(link) },
        icon = icon,
        text = text
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LongTapTextButton(
    onClick: () -> Unit,
    onClickLabel: String,
    onLongClick: () -> Unit,
    onLongClickLabel: String,
    modifier: Modifier = Modifier,
    shape: Shape = ButtonDefaults.shape,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ButtonWithIconContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onClickLabel = onClickLabel,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                Row(
                    Modifier
                        .defaultMinSize(
                            minWidth = ButtonDefaults.MinWidth,
                            minHeight = ButtonDefaults.MinHeight
                        )
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}


@Composable
@Preview
fun Preview() {
    NikGappsThemePreview() {
        Surface {
            Column {
                PreferenceSubtitle(text = "Preview")
                OutlinedButtonWithIcon(
                    onClick = { /* Do something */ },
                    icon = Icons.Default.Favorite,
                    text = "Click Me"
                )
                TextButtonWithIcon(
                    onClick = { /* Do something */ },
                    icon = Icons.Default.FavoriteBorder,
                    text = "Click Me"
                )
                FilledTonalButtonWithIcon(
                    onClick = {},
                    icon = Icons.Default.Favorite,
                    text = "Click Me"
                )
                FilledButtonWithIcon(
                    onClick = {},
                    icon = Icons.Default.Favorite,
                    text = "Click Me"
                )
                ConfirmButton(
                    onClick = {}
                )
                DismissButton(){}
                LinkButton()
                LongTapTextButton(
                    onClick = {},
                    onClickLabel = "OnClickLabel",
                    onLongClick = {},
                    onLongClickLabel = "onLongClickLabel",
                    content = {
                        Column {
                            Text(text = "test")
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "test1")
                        }
                    }
                )
            }
        }
    }
}