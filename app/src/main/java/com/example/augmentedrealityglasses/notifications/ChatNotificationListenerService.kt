package com.example.augmentedrealityglasses.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.augmentedrealityglasses.App
import com.example.augmentedrealityglasses.ble.devicedata.RemoteDeviceManager
import com.example.augmentedrealityglasses.settings.NotificationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatNotificationListenerService : NotificationListenerService() {

    private val TAG = "ChatNotificationService"

    private val scope = CoroutineScope(Dispatchers.IO)

    private val supportedPackages = setOf(
        "com.whatsapp",            // WhatsApp
        "com.whatsapp.w4b",        // WhatsApp Business
        "org.telegram.messenger",  // Telegram
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg !in supportedPackages) return

        //do not disturb mode check
        if (isDNDActiveCompat()) {
            Log.d(TAG, "DND on: discard notification from $pkg")
            return
        }

        //Do not send notification summaries
        val n = sbn.notification
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val notification = sbn.notification ?: return
        val parsed = parseChatNotification(notification) ?: return

        val appName = when (pkg) {
            "com.whatsapp", "com.whatsapp.w4b" -> NotificationSource.WHATSAPP
            "org.telegram.messenger" -> NotificationSource.TELEGRAM
            else -> return
        }

        scope.launch {
            if (!NotificationFilters.isEnabled(
                    context = this@ChatNotificationListenerService,
                    source = appName
                )
            ) return@launch

            val jsonToSend = JSONObject()

            jsonToSend.put("command", "n")
            jsonToSend.put("source", appName.toString().lowercase())
            //jsonToSend.put("conv", parsed.conversation)
            jsonToSend.put("sender", parsed.sender)
            jsonToSend.put("content", parsed.text)

            val msg = jsonToSend.toString()

            val proxy: RemoteDeviceManager =
                (applicationContext as App).container.proxy

            if (proxy.isConnected()) {
                proxy.send(msg)
                Log.d(TAG, "Sending to device: $msg")
            } else {
                Log.d(TAG, "Device offline. Notification: $msg")
            }
        }
    }

    private fun parseChatNotification(notification: Notification): ChatMessage? {

        //Style is used only for messaging applications
        val style =
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)

        if (style != null) {
            val messages = style.messages
            if (!messages.isNullOrEmpty()) {
                val last = messages.last()
                val text = last.text?.toString()?.trim().orEmpty()
                val sender = last.person?.name?.toString() ?: last.sender?.toString()
                //TODO: add conv?
                //val conv = style.conversationTitle?.toString()
                if (text.isNotEmpty()) {
                    val cleanedText = sanitize(replaceEmojisWithPlaceholder(cleanBidi(text)))
                    val cleanedSender =
                        sanitize(replaceEmojisWithPlaceholder(cleanBidi(sender.orEmpty())))

                    return ChatMessage(cleanedSender, cleanedText)
                }
            }
        }
        return null
    }

    /**
     * It removes tabs, newlines and multiple spaces. It removes also spaces at the beginning and at the end of the string.
     */
    private fun sanitize(input: String): String {
        return Regex("\\s+").replace(input, " ").trim()
    }


    private fun isDNDActiveCompat(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        val filter = nm.currentInterruptionFilter
        return filter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                || filter == android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS
                || filter == android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }

    /**
     * This function replace emojis with a placeholder ([?]). It puts a single placeholder also if emojis are longer than one code point
     */
    private fun replaceEmojisWithPlaceholder(
        input: String,
        placeholder: String = "[?]"
    ): String {

        if (input.isEmpty()) {
            return input
        }

        val sb = StringBuilder(input.length)
        var i = 0
        var lastWasEmoji = false

        while (i < input.length) {
            val cp = input.codePointAt(i)
            val emojiLike = isEmoji(cp)

            if (emojiLike) {
                if (!lastWasEmoji) sb.append(placeholder)
                lastWasEmoji = true
            } else {
                sb.appendCodePoint(cp)
                lastWasEmoji = false
            }

            i += Character.charCount(cp)
        }

        return sb.toString()
    }

    /**
     * This function finds emojis in a specific code point provided
     */
    private fun isEmoji(cp: Int): Boolean {
        return (cp in 0x1F600..0x1F64F)   // emoticons
                || (cp in 0x1F300..0x1F5FF)   // symbols & pictographs
                || (cp in 0x1F680..0x1F6FF)   // transport & map
                || (cp in 0x2600..0x26FF)     // misc symbols
                || (cp in 0x2700..0x27BF)     // dingbats
                || (cp in 0xFE00..0xFE0F)     // variation selectors
                || (cp in 0x1F900..0x1F9FF)   // supplemental symbols & pictographs (new emojis)
                || (cp in 0x1FA70..0x1FAFF)   // Symbols & Pictographs Extended-A
                || (cp == 0x200D)     // zero-width joiner
    }

    /**
     * It removes bidirectionals characters/zero-width (bidi), like this sequence: â€Ž
     */
    private fun cleanBidi(input: String): String {
        if (input.isEmpty()) return input
        val cleaned = input.filterNot { ch ->
            ch == '\u200B' || ch == '\u200C' || ch == '\u200D' || ch == '\u200E' || ch == '\u200F' || // zero-width + LRM/RLM
                    ch in '\u202A'..'\u202E' || // LRE/RLE/PDF/LRO/RLO
                    ch in '\u2066'..'\u2069' || // LRI/RLI/FSI/PDI
                    ch == '\uFEFF'              // BOM
        }
        return cleaned
    }

    data class ChatMessage(
        val sender: String?,
        val text: String
    )
}