package dev.akash.channelchecker

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService

class YouTubeAccessibilityService : AccessibilityService() {

    private var LastNotifiedChannel:String=""
    //private val browsers = arrayOf("com.google.android.youtube", "com.brave.browser" , "org.mozilla.firefox")
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event == null) return

        // Only react to content changes in the YouTube app
        val rootNode = rootInActiveWindow
        if (event.packageName == "com.google.android.youtube" &&
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            val channelName = findChannelNameNode(rootNode)

            Thread.sleep(1000)
            if(channelName == null)
                Log.e("yes" , "channelnode is null")

            else {
                val output = "(?<=subscribe to ).*[^.]".toRegex(setOf(RegexOption.IGNORE_CASE)).find(channelName)?.value
                Log.d("yes", "Channel: ${output}")
                val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val targetChannel = prefs.getString("target_channel", null)

                if( output!=null && output !=LastNotifiedChannel ) {
                    showNotification(output)
                    LastNotifiedChannel = output
                }

            }
        }
        if(event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){
            val fu = findURL(rootInActiveWindow)
            Thread.sleep(1000)
            if (fu != null && fu !=LastNotifiedURL && fu!="null") {
                showNotificationUrl(fu)
                LastNotifiedURL = fu
            }
        }

        }
    private fun findURL(node : AccessibilityNodeInfo) : String? {

        for(i in 0 until node.childCount){
            val child = node.getChild(i)?:continue
            child.refresh()
            val resID = if(child.viewIdResourceName !=null) child.viewIdResourceName.toString() else null
            if (resID != null) {
                if(resID.contains("url") && !child.isFocused ) {
                    return child?.text.toString()
                }
            }

        val found = findURL(child)
        if (found != null) return found
        }

        return null
    }

    private fun findChannelNameNode(node: AccessibilityNodeInfo): String? {

    // 2. Traverse and look for contentDescription label
    for (i in 0 until node.childCount) {
        val child = node.getChild(i) ?: continue

        val desc = child.contentDescription?.toString()
        val idRes = child.viewIdResourceName?.toString()

        if (desc?.contains("Subscribe") == true) {
            return desc
        }

        val found = findChannelNameNode(child)
        if (found != null) return found
    }

    return null
}
    private fun showNotification(channelName: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "yt_alerts",
                "YouTube Channel Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "yt_alerts")
            .setContentTitle("You're watching:")
            .setContentText(channelName)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

private fun showNotificationUrl(url: String) {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "url_alerts",
            "URL Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(this, "url_alerts")
        .setContentTitle("You're logged on to :")
        .setContentText(url)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(1, notification)
}
}
