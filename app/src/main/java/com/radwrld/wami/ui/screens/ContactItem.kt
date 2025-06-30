// @path: app/src/main/java/com/radwrld/wami/ui/screens/ContactItem.kt
package com.radwrld.wami.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.radwrld.wami.R
import com.radwrld.wami.network.Contact

@Composable
fun ContactItem(
    contact: Contact,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val placeholder = if (contact.isGroup)
            painterResource(id = R.drawable.ic_group_placeholder)
        else
            painterResource(id = R.drawable.ic_profile_placeholder)

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(contact.avatarUrl)
                .crossfade(true)
                .build(),
            placeholder = placeholder,
            error = placeholder,
            contentDescription = "${contact.name} avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}