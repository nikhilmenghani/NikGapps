package com.nikgapps.app.presentation.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil3.compose.SubcomposeAsyncImage
import com.nikgapps.app.data.DisplayPrefs
import com.nikgapps.app.data.GithubPrefs
import com.nikgapps.app.network.LocalInternetAvailable
import com.nikgapps.app.utils.network.EliteMembershipRepository
import com.nikgapps.app.utils.network.GitHubDeviceAuth
import kotlinx.coroutines.CancellationException

internal data class AccountMembership(val isElite: Boolean = false, val resolved: Boolean = false)
internal val LocalAccountMembership = staticCompositionLocalOf { AccountMembership() }

/** One membership lookup shared by project limits and the profile popup. */
@Composable
internal fun AccountMembershipProvider(content: @Composable () -> Unit) {
    val username = GithubPrefs.username
    val isOnline = LocalInternetAvailable.current
    val previewNonElite = DisplayPrefs.nonElitePreviewEnabled
    var membership by remember(username, isOnline, previewNonElite) { mutableStateOf(AccountMembership()) }
    LaunchedEffect(username, isOnline, previewNonElite) {
        val elite = try {
            isOnline && username.isNotBlank() && EliteMembershipRepository.isElite(username)
        } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { false }
        membership = AccountMembership(isElite = elite, resolved = true)
    }
    LaunchedEffect(username, GithubPrefs.token) {
        if (GithubPrefs.avatarUrl.isBlank() && GithubPrefs.token.isNotBlank()) {
            try {
                GithubPrefs.avatarUrl = GitHubDeviceAuth.accountProfile(GithubPrefs.token).avatarUrl
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (_: Exception) { /* Keep the fallback profile icon. */ }
        }
    }
    CompositionLocalProvider(LocalAccountMembership provides membership, content = content)
}

@Composable
internal fun AccountProfileButton(size: Dp = 56.dp) {
    val username = GithubPrefs.username
    val membership = LocalAccountMembership.current
    var expanded by rememberSaveable(username) { mutableStateOf(false) }
    val popupVisibility = remember(username) { MutableTransitionState(false) }
    popupVisibility.targetState = expanded
    val popupWidth = minOf(360.dp, (LocalConfiguration.current.screenWidthDp - 32).coerceAtLeast(0).dp)
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        with(density) { AccountPopupPositionProvider(margin = 16.dp.roundToPx(), gap = 8.dp.roundToPx()) }
    }
    Box {
        ProjectGlassSurface(modifier = Modifier.size(size), shape = CircleShape) {
        Surface(onClick = { expanded = !expanded },
            modifier = Modifier.size(size).semantics { role = Role.Button; contentDescription = "Profile" },
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AccountAvatar(modifier = Modifier.size(36.dp))
            }
        }
        }
        if (popupVisibility.currentState || popupVisibility.targetState) Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true)
        ) {
            AnimatedVisibility(
                visibleState = popupVisibility,
                enter = fadeIn(tween(180)) + slideInVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing), initialOffsetY = { it / 8 }),
                exit = fadeOut(tween(120)) + slideOutVertically(
                    animationSpec = tween(180, easing = FastOutSlowInEasing), targetOffsetY = { it / 8 })
            ) {
                Surface(modifier = Modifier.width(popupWidth), shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 8.dp) {
                    WelcomeCard(username, membership.isElite)
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(username: String, isElite: Boolean) {
    val textHeight = with(LocalDensity.current) {
        MaterialTheme.typography.titleMedium.lineHeight.toDp() * 2 +
            MaterialTheme.typography.labelSmall.lineHeight.toDp()
    } + 2.dp
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.width(76.dp).height(textHeight.coerceAtLeast(68.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                AccountAvatar(modifier = Modifier.size(40.dp),
                    contentDescription = "$username's GitHub profile picture")
                // The fixed column height prevents membership updates from moving the card.
                if (isElite) Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("ELITE", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
            Column(Modifier.weight(1f).height(textHeight),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
                Text(if (username.isBlank()) "Loading account…" else "Welcome, $username",
                    style = MaterialTheme.typography.titleMedium, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text("Signed in with GitHub",
                    style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}


@Composable
private fun AccountAvatar(modifier: Modifier, contentDescription: String? = null) {
    if (GithubPrefs.avatarUrl.isBlank()) {
        Icon(Icons.Default.AccountCircle, contentDescription = contentDescription, modifier = modifier)
    } else {
        SubcomposeAsyncImage(
            model = GithubPrefs.avatarUrl,
            contentDescription = contentDescription,
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop,
            loading = { Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.fillMaxSize()) },
            error = { Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.fillMaxSize()) }
        )
    }
}

/** Keep the card close to its button, above it when possible, with room at both screen edges. */
internal class AccountPopupPositionProvider(private val margin: Int, private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
        layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val preferredX = if (layoutDirection == LayoutDirection.Ltr)
            anchorBounds.right - popupContentSize.width else anchorBounds.left
        val x = preferredX.coerceIn(margin, maxX)
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        val above = anchorBounds.top - popupContentSize.height - gap
        val below = anchorBounds.bottom + gap
        val y = when {
            above >= margin -> above.coerceAtMost(maxY)
            below <= maxY -> below
            else -> above.coerceIn(margin, maxY)
        }
        return IntOffset(x, y)
    }
}
