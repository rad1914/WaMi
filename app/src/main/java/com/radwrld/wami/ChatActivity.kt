// @path: app/src/main/java/com/radwrld/wami/ChatActivity.kt
package com.radwrld.wami

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.radwrld.wami.adapter.ChatListItem
import com.radwrld.wami.network.Message
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.ChatViewModel
import com.radwrld.wami.ui.viewmodel.ChatViewModelFactory
import com.radwrld.wami.ui.viewmodel.UiState
import com.radwrld.wami.util.ActiveChatManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

class ChatActivity : AppCompatActivity() {

    private val jid by lazy { intent.getStringExtra(EXTRA_JID).orEmpty() }
    private val name by lazy { intent.getStringExtra(EXTRA_NAME) ?: "Unknown" }
    private val isGroup by lazy { jid.endsWith("@g.us") }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(application, jid)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (jid.isBlank()) {
            Toast.makeText(this, "Error: Invalid JID.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        observeErrors()

        setContent {
            WamiTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                ChatScreen(
                    contactName = name,
                    isGroup = isGroup,
                    listItems = uiState.messages,
                    uiState = uiState,
                    onSendMessage = viewModel::sendText,
                    onSendMedia = viewModel::sendMedia,
                    onLoadOlderMessages = viewModel::loadOlderMessages,
                    onMediaClick = ::onMediaClick,
                    onReaction = { message, emoji -> viewModel.sendReaction(message.id, emoji) },
                    onContactClick = {
                        if (!isGroup) {
                            startActivity(
                                Intent(this, AboutActivity::class.java)
                                    .putExtra(AboutActivity.EXTRA_JID, jid)
                            )
                        }
                    },
                    onBackClick = ::finish
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ActiveChatManager.setActiveChat(jid)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(jid.hashCode())
    }

    override fun onPause() {
        super.onPause()
        ActiveChatManager.clearActiveChat()
    }

    private fun observeErrors() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errors.collectLatest {
                    Toast.makeText(this@ChatActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun onMediaClick(msg: Message) {
        lifecycleScope.launch {
            val file: File? = viewModel.getMediaFile(msg)
            if (file != null) {
                val uri = FileProvider.getUriForFile(
                    this@ChatActivity,
                    "${applicationContext.packageName}.provider",
                    file
                )

                val intent = if (msg.type in setOf("image", "video")) {
                    Intent(this@ChatActivity, MediaViewActivity::class.java).apply {
                        setDataAndType(uri, msg.mimetype)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent.createChooser(
                        Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, msg.mimetype)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        "Open file with"
                    )
                }
                startActivity(intent)
            } else {

                Toast.makeText(this@ChatActivity, "File downloading…", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_JID = "EXTRA_JID"
        const val EXTRA_NAME = "EXTRA_NAME"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    contactName: String,
    isGroup: Boolean,
    listItems: List<ChatListItem>,
    uiState: UiState,
    onSendMessage: (String) -> Unit,
    onSendMedia: (Uri) -> Unit,
    onLoadOlderMessages: () -> Unit,
    onMediaClick: (Message) -> Unit,
    onReaction: (Message, String) -> Unit,
    onContactClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let(onSendMedia)
        }
    )

    Scaffold(
        topBar = {
            ChatTopBar(
                contactName = contactName,
                lastSeen = null,
                onContactClick = onContactClick,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            MessageInput(
                onSendMessage = onSendMessage,
                onAttachFile = { pickFileLauncher.launch(arrayOf("*/*")) }
            )
        }
    ) { padding ->

        val pullToRefreshState = rememberPullToRefreshState()
        if (pullToRefreshState.isRefreshing) {
            LaunchedEffect(true) {
                onLoadOlderMessages()
            }
        }

        LaunchedEffect(uiState.loadingOlder) {
            if (!uiState.loadingOlder) {
                pullToRefreshState.endRefresh()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            MessageList(
                listItems = listItems,
                isGroup = isGroup,
                onMediaClick = onMediaClick,
                onReaction = onReaction,
                modifier = Modifier.fillMaxSize()
            )

            if (uiState.loading && listItems.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    contactName: String,
    lastSeen: String?,
    onContactClick: () -> Unit,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column(modifier = Modifier.clickable(onClick = onContactClick)) {
                Text(text = contactName, style = MaterialTheme.typography.titleLarge)
                if (lastSeen != null) {
                    Text(
                        text = lastSeen,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        }
    )
}

@Composable
private fun MessageInput(
    onSendMessage: (String) -> Unit,
    onAttachFile: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val hasText by remember { derivedStateOf { text.isNotBlank() } }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.message_hint)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 5,
                    )
                    if (!hasText) {
                        IconButton(onClick = onAttachFile) {

                            Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    onSendMessage(text)
                    text = ""
                },
                enabled = hasText,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                Icon(
                    imageVector = if (hasText) Icons.Default.Send else Icons.Default.Mic,
                    contentDescription = if (hasText) stringResource(R.string.send) else stringResource(R.string.voice_message)
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    listItems: List<ChatListItem>,
    isGroup: Boolean,
    onMediaClick: (Message) -> Unit,
    onReaction: (Message, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listItems.size) {
        if (listItems.isNotEmpty()) {
            listState.animateScrollToItem(listItems.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            items = listItems,
            key = { item ->
                when (item) {
                    is ChatListItem.MessageItem -> item.message.id
                    is ChatListItem.DividerItem -> "divider_${item.timestamp}"
                    is ChatListItem.WarningItem -> "warning_item"
                }
            },
            contentType = { item ->
                when (item) {
                    is ChatListItem.MessageItem -> "message"
                    is ChatListItem.DividerItem -> "divider"
                    is ChatListItem.WarningItem -> "warning"
                }
            }
        ) { item ->
            when (item) {
                is ChatListItem.MessageItem -> MessageBubble(
                    message = item.message,
                    isGroup = isGroup,
                    onMediaClick = onMediaClick,
                    onReaction = onReaction,
                )
                is ChatListItem.DividerItem -> DateDivider(timestamp = item.timestamp)
                is ChatListItem.WarningItem -> E2EWarning()
            }
        }
    }
}

@Composable
private fun DateDivider(timestamp: Long) {
    val context = LocalContext.current

    val dateText = remember(timestamp) { formatTimestampToDateHeader(context, timestamp) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isGroup: Boolean,
    onMediaClick: (Message) -> Unit,
    onReaction: (Message, String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showReactionPanel by remember { mutableStateOf(false) }
    val isOutgoing = message.isOutgoing
    val bubbleAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    val bubbleColor = if (isOutgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = bubbleAlignment
    ) {
        if (showReactionPanel) {
            ReactionPanel(
                onReactionSelected = { emoji ->
                    onReaction(message, emoji)
                    showReactionPanel = false
                },
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bubbleColor,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { onReaction(message, REACTIONS.first()) },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showReactionPanel = !showReactionPanel
                        },
                        onTap = { showReactionPanel = false }
                    )
                }
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                if (isGroup && !isOutgoing) {
                    Text(
                        text = message.senderName ?: "Unknown",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                message.quotedMessageText?.let {
                    ReplyToContent(
                        sender = message.name ?: "",
                        text = it
                    )
                }

                MessageContent(message = message, onMediaClick = onMediaClick)

                MessageInfo(
                    timestamp = message.timestamp,
                    status = if (isOutgoing) message.status else null,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
        if (message.reactions.isNotEmpty()) {
            ReactionGroup(
                reactions = message.reactions,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun MessageContent(message: Message, onMediaClick: (Message) -> Unit) {
    Column {
        if (message.hasMedia()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMediaClick(message) },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = message.localMediaPath?.let { File(it) } ?: message.mediaUrl,
                    contentDescription = "Media message preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = rememberVectorPainter(image = Icons.Default.Image),
                    error = rememberVectorPainter(image = Icons.Default.BrokenImage)
                )
                if (message.isVideo()) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play video button",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .padding(4.dp)
                    )
                }
            }
        }
        if (message.hasText()) {
            FormattedText(
                text = message.text!!,
                modifier = Modifier.padding(top = if (message.hasMedia()) 4.dp else 2.dp)
            )
        }
    }
}

@Composable
private fun ReplyToContent(sender: String, text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = sender,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun MessageInfo(timestamp: Long, status: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val time = remember(timestamp) {
        DateFormat.getTimeFormat(context).format(Date(timestamp))
    }
    Row(
        modifier = modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = time,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (status != null) {
            Spacer(Modifier.width(4.dp))
            val (icon, tint) = when (status) {
                "read" -> R.drawable.ic_read_receipt to MaterialTheme.colorScheme.primary
                "delivered" -> R.drawable.ic_delivered_receipt to MaterialTheme.colorScheme.onSurfaceVariant
                else -> R.drawable.ic_sending to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "Message status",
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun E2EWarning() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.e2e_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private val REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

@Composable
private fun ReactionPanel(
    onReactionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        modifier = modifier,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            REACTIONS.forEach { emoji ->
                Text(
                    text = emoji,
                    modifier = Modifier
                        .clickable { onReactionSelected(emoji) }
                        .padding(8.dp),
                    fontSize = 24.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReactionGroup(reactions: Map<String, Int>, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier) {
        reactions.forEach { (emoji, count) ->
            Surface(
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = emoji, fontSize = 12.sp)
                    Spacer(Modifier.width(2.dp))
                    Text(text = count.toString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FormattedText(text: String, modifier: Modifier = Modifier) {
    val monoBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val styledText = remember(text, monoBgColor) {
        buildAnnotatedString {
            val styleMap = mapOf(
                '*' to SpanStyle(fontWeight = FontWeight.Bold),
                '_' to SpanStyle(fontStyle = FontStyle.Italic),
                '~' to SpanStyle(textDecoration = TextDecoration.LineThrough),
                '`' to SpanStyle(fontFamily = FontFamily.Monospace, background = monoBgColor)
            )

            val regex = "([*_~`])(.*?)\\1".toRegex()
            var lastIndex = 0

            regex.findAll(text).forEach { matchResult ->
                val (delimiter, content) = matchResult.destructured
                if (matchResult.range.first > lastIndex) {
                    append(text.substring(lastIndex, matchResult.range.first))
                }
                styleMap[delimiter.first()]?.let { style ->
                    withStyle(style) {
                        append(content)
                    }
                }
                lastIndex = matchResult.range.last + 1
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
    Text(text = styledText, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    WamiTheme {
        val previewListItems = listOf(
            ChatListItem.WarningItem,
            ChatListItem.DividerItem(System.currentTimeMillis() - 86400000),
            ChatListItem.MessageItem(Message(id="1", jid="jane@example.com", isOutgoing = false, text = "Hola! *qué tal?*", timestamp = System.currentTimeMillis() - 200000, name = "Jane")),
            ChatListItem.MessageItem(Message(id="2", jid="me@example.com", isOutgoing = true, text = "Todo bien por acá en _Sayula_. `code`", timestamp = System.currentTimeMillis()- 100000, name = "Me")),
            ChatListItem.DividerItem(System.currentTimeMillis()),
            ChatListItem.MessageItem(Message(id="3", jid="jane@example.com", isOutgoing = false, text = "~rayado~", timestamp = System.currentTimeMillis(), name = "Jane"))
        )
        ChatScreen(
            contactName = "Jane Doe",
            isGroup = false,
            listItems = previewListItems,
            uiState = UiState(),
            onSendMessage = {},
            onSendMedia = {},
            onLoadOlderMessages = {},
            onMediaClick = {},
            onReaction = { _, _ -> },
            onContactClick = {},
            onBackClick = {}
        )
    }
}

private fun formatTimestampToDateHeader(context: Context, timestamp: Long): String {
    val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        messageCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) -> context.getString(R.string.today)
        messageCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) -> context.getString(R.string.yesterday)
        else -> SimpleDateFormat("MMMM dd, yy", Locale.getDefault()).format(Date(timestamp))
    }
}
