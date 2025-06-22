// @path: app/src/main/java/com/radwrld/wami/MainActivity.kt
package com.radwrld.wami

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.radwrld.wami.adapter.ConversationAdapter
import com.radwrld.wami.adapter.SearchResultAdapter
import com.radwrld.wami.databinding.ActivityMainBinding
import com.radwrld.wami.model.Contact
import com.radwrld.wami.network.ApiClient
import com.radwrld.wami.storage.ServerConfigStorage
import com.radwrld.wami.sync.SyncService // No olvides el import
import com.radwrld.wami.ui.viewmodel.ConversationListViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var serverConfigStorage: ServerConfigStorage
    private val viewModel: ConversationListViewModel by viewModels()

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var searchResultAdapter: SearchResultAdapter

    // ++ NUEVO: Referencia al SearchView para poder cerrarlo
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        serverConfigStorage = ServerConfigStorage(this)
        setupEventListeners()
        setupRecyclerViews()
        observeViewModel()

        // Iniciar el servicio de sincronización
        Intent(this, SyncService::class.java).also { intent ->
            intent.action = SyncService.ACTION_START
            startService(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)

        // ++ MODIFICADO: Guardamos la referencia al SearchView
        searchView = searchItem.actionView as? SearchView
        searchView?.let { sv ->
            sv.queryHint = "Search contacts or messages..."

            sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    // La búsqueda se hace en tiempo real, no se necesita acción aquí
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.onSearchQueryChanged(newText.orEmpty())
                    return true
                }
            })

            sv.setOnCloseListener {
                viewModel.onSearchQueryChanged("")
                false // Permite que el sistema maneje el cierre
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        // SyncManager maneja la conexión automáticamente
    }

    override fun onPause() {
        super.onPause()
        // SyncManager maneja la conexión automáticamente
    }

    private fun setupEventListeners() {
        binding.toolbar.setNavigationOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val navMessages: ImageButton = binding.navMessages
        val navAdd: ImageButton = binding.navAdd
        val navContacts: ImageButton = binding.navContacts
        navMessages.isSelected = true

        navMessages.setOnClickListener {
            navMessages.isSelected = true
            navContacts.isSelected = false
        }
        navAdd.setOnClickListener {
            showFastContactDialog()
        }
        navContacts.setOnClickListener {
            navContacts.isSelected = true
            navMessages.isSelected = false
            startActivity(Intent(this, ContactsActivity::class.java))
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.load()
        }
    }

    private fun showFastContactDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Fast Contact")
        builder.setMessage("Enter a phone number to start a new chat.")
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            hint = "e.g., 15551234567"
        }
        builder.setView(input)
        builder.setPositiveButton("Chat") { dialog, _ ->
            val number = input.text.toString().trim()
            if (number.isNotEmpty()) {
                val jid = "$number@s.whatsapp.net"
                // No llamamos a navigateToChat aquí porque es un contacto rápido, no uno existente
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("EXTRA_JID", jid)
                    putExtra("EXTRA_NAME", number)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Phone number cannot be empty.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun setupRecyclerViews() {
        conversationAdapter = ConversationAdapter(
            onItemClicked = { contact -> navigateToChat(contact) },
            onItemLongClicked = { contact, _ -> confirmHide(contact) }
        )

        searchResultAdapter = SearchResultAdapter(
            onItemClicked = { contact -> navigateToChat(contact) }
        )

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = conversationAdapter
        }
    }

    /**
     * ++ MODIFICADO: Ahora también cierra la búsqueda antes de navegar.
     */
    private fun navigateToChat(contact: Contact) {
        // Cierra el SearchView y limpia la consulta para volver a la lista normal.
        searchView?.let {
            if (!it.isIconified) {
                it.setQuery("", false) // Limpia el texto sin ejecutar una nueva búsqueda
                it.isIconified = true      // Contrae el SearchView a un ícono
            }
        }

        // Navega a la actividad de chat
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("EXTRA_JID", contact.id)
            putExtra("EXTRA_NAME", contact.name)
        }
        startActivity(intent)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observa el estado de la lista de conversaciones
                launch {
                    viewModel.conversationState.collect { state ->
                        binding.swipeRefreshLayout.isRefreshing = state.isLoading
                        // Solo actualiza el adaptador de conversaciones si no estamos buscando
                        if (viewModel.searchState.value.query.isBlank()) {
                            conversationAdapter.submitList(state.conversations)
                        }

                        state.error?.let { errorMsg ->
                            Toast.makeText(this@MainActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                            if (errorMsg.contains("401")) {
                                logout(false)
                            }
                        }
                    }
                }

                // Observa el estado de la búsqueda
                launch {
                    viewModel.searchState.collect { searchState ->
                        if (searchState.query.isBlank()) {
                            // Si no hay búsqueda, asegúrate de que el adaptador sea el de conversaciones
                            if (binding.rvMessages.adapter != conversationAdapter) {
                                binding.rvMessages.adapter = conversationAdapter
                                conversationAdapter.submitList(viewModel.conversationState.value.conversations)
                            }
                        } else {
                            // Si hay una búsqueda, cambia al adaptador de búsqueda
                            if (binding.rvMessages.adapter != searchResultAdapter) {
                                binding.rvMessages.adapter = searchResultAdapter
                            }
                            searchResultAdapter.submitList(searchState.results)
                        }
                    }
                }
            }
        }
    }

    private fun confirmHide(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Hide Conversation")
            .setMessage("Are you sure you want to hide the conversation with ${contact.name}?")
            .setPositiveButton("Hide") { _, _ ->
                viewModel.hide(contact.id)
                Toast.makeText(this, "Conversation hidden", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout(callServer: Boolean) {
        // Detener el servicio de sincronización
        Intent(this, SyncService::class.java).also { intent ->
            intent.action = SyncService.ACTION_STOP
            startService(intent) // Usar startService con la acción STOP es más robusto
        }

        lifecycleScope.launch {
            if (callServer) {
                try {
                    ApiClient.getInstance(this@MainActivity).logout()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Server logout failed", e)
                }
            }
            ApiClient.close()
            serverConfigStorage.saveSessionId(null)
            serverConfigStorage.saveLoginState(false)
            val intent = Intent(this@MainActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
