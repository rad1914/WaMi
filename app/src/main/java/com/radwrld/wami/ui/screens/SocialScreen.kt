// @path: app/src/main/java/com/radwrld/wami/ui/screens/SocialScreen.kt
package com.radwrld.wami.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.radwrld.wami.R
import com.radwrld.wami.network.StatusItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    statuses: List<StatusItem>,
    onFetchStatuses: () -> Unit,
    onNavigateBack: () -> Unit,
    onShowToast: (String) -> Unit,
    onStatusClick: (StatusItem) -> Unit
) {

    LaunchedEffect(Unit) {
        onFetchStatuses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups & Social") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onShowToast("Iniciar un nuevo mensaje") }) {
                Icon(painterResource(R.drawable.ic_chat_circle_dots), contentDescription = "New Message")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            StatusSection(statuses = statuses, onAddStatus = {onShowToast("Añadir nuevo estado")}, onStatusClick = onStatusClick)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ActionItem(iconRes = R.drawable.ic_group_placeholder, text = "New Group") { onShowToast("Crear nuevo grupo") }
            ActionItem(iconRes = R.drawable.ic_users, text = "New Contact") { onShowToast("Abrir nuevo contacto") }
            ActionItem(iconRes = R.drawable.ic_group_placeholder, text = "New Community") { onShowToast("Crear nueva comunidad") }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SectionTitle(title = "Channels")
            ActionItem(iconRes = R.drawable.ic_add, text = "Create Channel") { onShowToast("Crear un canal") }
            ActionItem(iconRes = android.R.drawable.ic_menu_search, text = "Find Channels") { onShowToast("Buscar canales") }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun StatusSection(
    statuses: List<StatusItem>,
    onAddStatus: () -> Unit,
    onStatusClick: (StatusItem) -> Unit
) {
    Column {
        SectionTitle(title = "Status")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { AddStatusCard(onClick = onAddStatus) }
            items(statuses, key = { it.id }) { status ->
                StatusItemCard(status = status, onClick = { onStatusClick(status) })
            }
        }
    }
}

@Composable
private fun AddStatusCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(110.dp, 180.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    painter = painterResource(R.drawable.ic_users),
                    contentDescription = "User profile picture",
                    modifier = Modifier.size(60.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add status icon",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(2.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Add status", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatusItemCard(status: StatusItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(110.dp, 180.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = status.avatarUrl,
                contentDescription = status.senderName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.profile_picture_placeholder),
                error = painterResource(R.drawable.profile_picture_placeholder)
            )
            Text(
                text = status.senderName ?: "Status",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun ActionItem(iconRes: Int, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
