// @path: app/src/main/java/com/radwrld/wami/ChatActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.radwrld.wami.network.Message
import com.radwrld.wami.ui.screens.chat.ChatScreen
import com.radwrld.wami.ui.theme.WamiTheme
import com.radwrld.wami.ui.viewmodel.ChatViewModel
import com.radwrld.wami.ui.viewmodel.ChatViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ChatActivity : ComponentActivity() {

    private val jid by lazy { intent.getStringExtra("EXTRA_JID").orEmpty() }
    private val name by lazy { intent.getStringExtra("EXTRA_NAME") ?: "Unknown" }
    private val isGroup get() = jid.endsWith("@g.us")

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(application, jid, name)
    }

    private val pickFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.also { uri ->
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.sendMedia(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (jid.isBlank()) {
            Toast.makeText(this, "Error: JID inválido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        observeErrors()

        setContent {
            WamiTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                ChatScreen(
                    state = uiState,
                    isGroup = isGroup,
                    contactName = name,
                    onNavigateBack = { finish() },
                    onLoadOlder = viewModel::loadOlderMessages,
                    onSendText = viewModel::sendText,
                    onSendMedia = {
                        pickFile.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        })
                    }
                )
            }
        }
    }
    
    private fun observeErrors() {
        lifecycleScope.launch {
            viewModel.errors.collectLatest { errorMsg ->
                Toast.makeText(this@ChatActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onMediaClick(msg: Message) {
        lifecycleScope.launch {
            viewModel.setLoading(true)
            val file = viewModel.getMediaFile(msg)
            viewModel.setLoading(false)

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
                Toast.makeText(this@ChatActivity, "Descargando archivo...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
