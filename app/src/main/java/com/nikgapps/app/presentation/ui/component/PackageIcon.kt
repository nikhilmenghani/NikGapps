package com.nikgapps.app.presentation.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.nikgapps.app.registry.PackageIconRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Use the same device / registry / standard fallback in every package view. */
@Composable
internal fun PackageIcon(packageName: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model by produceState<Any?>(null, context, packageName) {
        value = null
        if (!packageName.isNullOrBlank()) value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
                ?: PackageIconRepository.icon(context, packageName)
        }
    }
    val fallback = rememberVectorPainter(Icons.Default.Android)
    if (model != null) AsyncImage(model, null, modifier,
        placeholder = fallback, error = fallback, fallback = fallback)
    else Icon(Icons.Default.Android, null, modifier)
}
