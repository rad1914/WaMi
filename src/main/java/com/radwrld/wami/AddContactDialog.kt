// AddContactDialog.kt
package com.radwrld.wami

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.radwrld.wami.R

/**
 * A dialog for entering a new contact (name, phone number, avatar URL).
 * Calls onAdd(name, number, avatarUrl) when the user taps "Add".
 */
class AddContactDialog(
    context: Context,
    private val onAdd: (name: String, number: String, avatarUrl: String) -> Unit
) : Dialog(context) {

    private lateinit var etName: EditText
    private lateinit var etNumber: EditText
    private lateinit var etAvatar: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_add_contact)

        etName = findViewById(R.id.et_contact_name)
        etNumber = findViewById(R.id.et_contact_number)
        etAvatar = findViewById(R.id.et_contact_avatar)
        btnAdd = findViewById(R.id.btn_add_contact)
        btnCancel = findViewById(R.id.btn_cancel_contact)

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val number = etNumber.text.toString().trim()
            val avatarUrl = etAvatar.text.toString().trim()

            // --- APPLIED SUGGESTION: Robust validation ---
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
