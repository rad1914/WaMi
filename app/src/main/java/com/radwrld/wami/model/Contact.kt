// @path: app/src/main/java/com/radwrld/wami/model/Contact.kt
package com.radwrld.wami.model

data class Contact(
    val id: String, // Use JID as the unique ID
    val name: String,
    val phoneNumber: String,
    val avatarUrl: String
) {
    // Add a secondary constructor for convenience if needed, though not required here.
    // The primary change is adding the 'id' field which will store the JID.
}
