// @path: app/src/main/java/com/radwrld/wami/ui/screens/chat/ChatComposables.kt
package com.radwrld.wami.ui.screens.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.radwrld.wami.R
import com.radwrld.wami.adapter.ChatListItem
import com.radwrld.wami.network.Message
import com.radwrld.wami.ui.TextFormatter
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    item: ChatListItem.MessageItem,
    isGroup: Boolean,
    onMediaClick: (Message) -> Unit,
    onReaction: (Message, String) -> Unit
) {
    val message = item.message
    val isOutgoing = message.isOutgoing
    var showReactionPanel by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
            Column {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isOutgoing) 16.dp else 0.dp,
                                bottomEnd = if (isOutgoing) 0.dp else 16.dp
                            )
                        )
                        .background(
                            if (isOutgoing) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .combinedClickable(
                            onClick = { showReactionPanel = false },
                            onLongClick = { showReactionPanel = true },
                            onDoubleClick = { onReaction(message, "👍") }
                        )
                ) {
                    MessageBubbleContent(message, isGroup, onMediaClick)
                }

                if (message.reactions.isNotEmpty()) {
                    ReactionGroup(
                        reactions = message.reactions,
                        modifier = Modifier
                            .padding(
                                start = if (isOutgoing) 0.dp else 8.dp,
                                end = if (isOutgoing) 8.dp else 0.dp
                            )
                    )
                }
            }

            if (showReactionPanel) {
                ReactionPanel(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-40).dp)
                        .shadow(4.dp, CircleShape)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    onReaction = { emoji ->
                        onReaction(message, emoji)
                        showReactionPanel = false
                    }
                )
            }
        }
    }
}

@Composable
fun MessageBubbleContent(
    message: Message,
    isGroup: Boolean,
    onMediaClick: (Message) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        if (!message.isOutgoing && isGroup) {
            Text(
                text = message.senderName ?: "Unknown",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (message.hasMedia()) {
            MediaContent(message, onMediaClick)
        } else {
            Text(
                text = TextFormatter
                    .format(LocalContext.current, message.text ?: "")
                    .toString(),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        MessageInfo(message)
    }
}

@Composable
fun MediaContent(message: Message, onMediaClick: (Message) -> Unit) {
    Column(modifier = Modifier.clickable { onMediaClick(message) }) {
        AsyncImage(
            model = message.localMediaPath ?: message.mediaUrl,
            contentDescription = "Media",
            modifier = Modifier
                .widthIn(max = 280.dp)
                .heightIn(max = 280.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Fit
        )
        if (message.hasText()) {
            Text(
                text = TextFormatter
                    .format(LocalContext.current, message.text!!)
                    .toString(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ColumnScope.MessageInfo(message: Message) {
    Row(
        modifier = Modifier
            .align(if (message.isOutgoing) Alignment.End else Alignment.Start)
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = DateFormat
                .getTimeInstance(DateFormat.SHORT)
                .format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        if (message.isOutgoing) {
            Icon(
                painter = painterResource(
                    id = when (message.status) {
                        "read" -> R.drawable.ic_read_receipt
                        "delivered" -> R.drawable.ic_delivered_receipt
                        else -> R.drawable.ic_sending
                    }
                ),
                contentDescription = "Status",
                modifier = Modifier
                    .size(14.dp)
                    .padding(start = 4.dp),
                tint = if (message.status == "read")
                    MaterialTheme.colorScheme.secondary
                else Color.Unspecified
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionGroup(reactions: Map<String, Int>, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier) {
        reactions.forEach { (emoji, count) ->
            Text(
                text = "$emoji $count",
                modifier = Modifier
                    .padding(end = 4.dp, top = 2.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun ReactionPanel(modifier: Modifier = Modifier, onReaction: (String) -> Unit) {
    val reactions = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
    Row(modifier = modifier) {
        reactions.forEach { emoji ->
            Text(
                text = emoji,
                modifier = Modifier
                    .clickable { onReaction(emoji) }
                    .padding(8.dp),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
fun DateDivider(item: ChatListItem.DividerItem) {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)

    calendar.timeInMillis = item.timestamp
    val messageDay = calendar.get(Calendar.DAY_OF_YEAR)

    val dateText = when {
        today == messageDay -> "Hoy"
        today - messageDay == 1 -> "Ayer"
        else -> SimpleDateFormat("dd MMMM yy", Locale.getDefault()).format(Date(item.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
