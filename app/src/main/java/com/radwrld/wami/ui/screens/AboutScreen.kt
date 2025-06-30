// @path: app/src/main/java/com/radwrld/wami/ui/screens/AboutScreen.kt
package com.radwrld.wami.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.radwrld.wami.R
import com.radwrld.wami.ui.viewmodel.AboutUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    uiState: AboutUiState,
    onNavigateBack: () -> Unit,
    onSharedMediaClick: () -> Unit,
    onBlockClick: () -> Unit,
    onReportClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isGroup) uiState.contactName else "Información del contacto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                ProfileHeader(
                    name = uiState.contactName,
                    avatarUrl = uiState.avatarUrl,
                    lastSeen = uiState.lastSeen
                )

                InfoCard(text = uiState.info)

                Spacer(Modifier.height(16.dp))

                NavigationRow(
                    text = "Multimedia, enlaces y docs",
                    count = uiState.mediaCount.toString(),
                    onClick = onSharedMediaClick
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                if (!uiState.isGroup) {
                    UserSpecificInfo(uiState)
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                
                if (!uiState.isGroup) {
                    ActionRow(
                        text = "Grupos en común",
                        count = uiState.commonGroupsCount.toString(),
                        icon = painterResource(R.drawable.ic_users),
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = {  }
                    )
                }
                
                ActionRow(
                    text = if(uiState.isBlocked) "Desbloquear a ${uiState.contactName}" else "Bloquear a ${uiState.contactName}",
                    icon = painterResource(R.drawable.ic_close),
                    textColor = MaterialTheme.colorScheme.error,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = onBlockClick
                )

                ActionRow(
                    text = if(uiState.isGroup) "Reportar grupo" else "Reportar a ${uiState.contactName}",
                    icon = painterResource(R.drawable.ic_flag),
                    textColor = MaterialTheme.colorScheme.error,
                    iconTint = MaterialTheme.colorScheme.error,
                    enabled = !uiState.isReported,
                    onClick = onReportClick
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(name: String, avatarUrl: String?, lastSeen: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Profile picture",
            placeholder = painterResource(R.drawable.profile_picture_placeholder),
            error = painterResource(R.drawable.profile_picture_placeholder),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.height(16.dp))
        Text(name, style = MaterialTheme.typography.headlineSmall)
        lastSeen?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Info.", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun UserSpecificInfo(uiState: AboutUiState) {
    Column {
        uiState.phoneNumber?.let {
            DetailRow(
                title = "Teléfono",
                subtitle = it,
                icon = painterResource(R.drawable.ic_phone)
            )
        }
        // CORRECCIÓN: Se usa .let para manejar el valor nulo de forma segura
        uiState.localTime?.let { time ->
            DetailRow(
                title = "Hora local",
                subtitle = time,
                icon = painterResource(R.drawable.ic_clock)
            )
        }
    }
}

@Composable
private fun DetailRow(title: String, subtitle: String, icon: Painter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NavigationRow(text: String, count: String, onClick: () -> Unit) {
     Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(count, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ActionRow(text: String, count: String? = null, icon: Painter, textColor: Color = LocalContentColor.current, iconTint: Color = textColor, enabled: Boolean = true, onClick: () -> Unit) {
     Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val alpha = if (enabled) 1f else 0.5f
        Icon(painter = icon, contentDescription = null, tint = iconTint.copy(alpha = alpha))
        Text(text, modifier = Modifier.weight(1f).padding(start = 24.dp), color = textColor.copy(alpha = alpha), style = MaterialTheme.typography.bodyLarge)
        count?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
    }
}
