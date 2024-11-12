package com.nikgapps.ui.components.items

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikgapps.app.presentation.theme.NikGappsThemePreview
import com.nikgapps.app.presentation.theme.applyOpacity

private const val horizontal = 8
private const val vertical = 12

private val PreferenceTitle
    @Composable get() = MaterialTheme.typography.titleMedium


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreferenceItem(
    title: String,
    description: String? = null,
    icon: Any? = null,
    enabled: Boolean = true,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onClickLabel = onClickLabel,
            enabled = enabled,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal.dp, vertical.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke()

            when (icon) {
                is ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 16.dp)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.applyOpacity(enabled)
                    )
                }

                is Painter -> {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 16.dp)
                            .size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.applyOpacity(enabled)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (icon == null && leadingIcon == null) 8.dp else 0.dp)
                    .padding(end = 8.dp)
            ) {
                PreferenceItemTitle(text = title, enabled = enabled)
                if (!description.isNullOrEmpty()) PreferenceItemDescription(
                    text = description,
                    enabled = enabled
                )
            }
            trailingIcon?.let {
                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 8.dp)
                        .align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                trailingIcon.invoke()
            }
        }
    }

}

@Composable
internal fun PreferenceItemTitle(
    modifier: Modifier = Modifier,
    text: String,
    maxLines: Int = 2,
    style: TextStyle = PreferenceTitle,
    enabled: Boolean,
    color: Color = MaterialTheme.colorScheme.onBackground,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    Text(
        modifier = modifier,
        text = text,
        maxLines = maxLines,
        style = style,
        color = color.applyOpacity(enabled),
        overflow = overflow
    )
}

@Composable
internal fun PreferenceItemDescription(
    modifier: Modifier = Modifier,
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    enabled: Boolean,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    Text(
        modifier = modifier,
        text = text,
        maxLines = maxLines,
        style = style,
        color = color.applyOpacity(enabled),
        overflow = overflow
    )
}

@Composable
fun PreferenceSubtitle(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        top = 20.dp,
        bottom = 8.dp,
    ),
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        modifier = modifier.padding(contentPadding),
        color = color,
        style = MaterialTheme.typography.labelLarge
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreferenceItemVariant(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onLongClickLabel: String? = null,
    onLongClick: () -> Unit = {},
    onClickLabel: String? = null,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.combinedClickable(
            enabled = enabled,
            onClick = onClick,
            onClickLabel = onClickLabel,
            onLongClick = onLongClick,
            onLongClickLabel = onLongClickLabel
        )
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 16.dp)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.applyOpacity(enabled)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (icon == null) 12.dp else 0.dp)
                    .padding(end = 8.dp)
            ) {
                PreferenceItemTitle(text = title, enabled = enabled)
                if (description != null) {
                    PreferenceItemDescription(text = description, enabled = enabled)
                }
            }
        }
    }

}



@Composable
@Preview
fun PreferenceItemPreview() {
    NikGappsThemePreview() {
        Surface {
            Column {
                PreferenceSubtitle(text = "Preview")
                PreferenceItem(title = "title1", description = "description2")
                PreferenceItem(
                    title = "title3",
                    description = "description4",
                    icon = Icons.Outlined.Update
                )
                PreferenceItemVariant(
                    title = "title5",
                    description = "description6",
                    icon = Icons.Outlined.Upload
                )
            }
        }
    }
}