// app/src/main/java/com/radwrld/wami/AddContactDialog.kt
package com.radwrld.wami

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText

class AddContactDialog(
    private val context: Context, // Make sure to accept the context here
    private val onAddContact: (name: String, number: String, avatarUrl: String) -> Unit
) {

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_contact, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val etAvatar = view.findViewById<EditText>(R.id.etAvatar)

        AlertDialog.Builder(context)
            .setTitle("New Contact")
            .setView(view)
            .setPositiveButton("Add") { _, _ ->
                onAddContact(
                    etName.text.toString(),
                    etPhone.text.toString(),
                    etAvatar.text.toString().ifBlank { "https://via.placeholder.com/48" }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
