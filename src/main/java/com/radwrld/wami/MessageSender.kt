package com.radwrld.wami.network

import android.content.Context
import android.widget.TextView
import android.widget.Toast
import com.radwrld.wami.api.SendMessageRequest
import com.radwrld.wami.api.WaApi
import com.radwrld.wami.storage.ServerConfigStorage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessageSender(
    private val context: Context,
    private val config: ServerConfigStorage,
    private val onReconnect: () -> Unit,
    private val tvStatus: TextView
) {
    private lateinit var waApi: WaApi

    init {
        RetrofitManager(context, config, onReconnect).also {
            it.start()
            waApi = it.waApi
        }
    }

    fun sendMessage(jidFull: String, text: String, onSuccess: () -> Unit) {
        val jid = jidFull.substringBefore("@") + "@s.whatsapp.net"
        attemptSend(jid, text, 0)
    }

    private fun attemptSend(jid: String, text: String, tries: Int) {
        waApi.sendMessage(SendMessageRequest(jid, text)).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful && tries == 0) {
                    attemptSend(jid, text, 1)
                } else if (!response.isSuccessful) {
                    Toast.makeText(context, "Send error ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                if (tries == 0) {
                    config.moveToNextServer()
                    onReconnect()
                    attemptSend(jid, text, 1)
                } else {
                    Toast.makeText(context, "Send failed", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
