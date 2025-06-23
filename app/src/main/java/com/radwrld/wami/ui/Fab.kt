// @path: app/src/main/java/com/radwrld/wami/ui/Fab.kt
// @path: app/src/main/java/com/radwrld/wami/AddContactDialog.kt

package com.radwrld.wami

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class AddContactDialog(
    context: Context,
    private val onAdd: (name: String, number: String, avatarUrl: String) -> Unit
) : Dialog(context) {

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window?.setBackgroundDrawableResource(android.R.color.transparent)

    setContentView(R.layout.dialog_add_contact)

    val etName = findViewById<EditText>(R.id.et_contact_name)
    val etNumber = findViewById<EditText>(R.id.et_contact_number)
    val etAvatar = findViewById<EditText>(R.id.et_contact_avatar)
    val btnAdd = findViewById<Button>(R.id.btn_add_contact)
    val btnCancel = findViewById<Button>(R.id.btn_cancel_contact)

    btnAdd.setOnClickListener {
        val name = etName.text.toString().trim()
        val number = etNumber.text.toString().trim()
        val avatarUrl = etAvatar.text.toString().trim()

        if (name.isEmpty() || number.isEmpty()) {
            Toast.makeText(context, "Name and number are required", Toast.LENGTH_SHORT).show()
        } else if (!Patterns.PHONE.matcher(number).matches()) {
            Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
        } else {
            onAdd(name, number, avatarUrl)
            dismiss()
        }
    }

    btnCancel.setOnClickListener { dismiss() }
}

}
