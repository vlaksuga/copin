package com.example.copinwebapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessageService : FirebaseMessagingService() {

    companion object {
        const val TAG = "TAG : MyFirebaseMessageService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: token = $token")
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        Log.d(TAG, "onMessageReceived: From = ${msg.from}")
        val link = msg.data["link"]
        if (msg.data.isNotEmpty()) {
            val intent = Intent(applicationContext, EntryActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("link", link)

            val pendingIntent = PendingIntent.getActivity(
                    applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT
            )
            var defaultChannelId =
                    applicationContext.getString(R.string.default_notification_channel_id)
            val eventChannelId =
                    applicationContext.getString(R.string.event_notification_channel_id)

            msg.notification?.let {
                defaultChannelId = it.channelId.toString()

            }
            val notificationBuilder = NotificationCompat.Builder(applicationContext, defaultChannelId)
                    .setSmallIcon(R.mipmap.icon_circle)
                    .setContentTitle(msg.data["title"])
                    .setContentText(msg.data["body"])
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val defaultChannel = NotificationChannel(defaultChannelId, defaultChannelId, NotificationManager.IMPORTANCE_HIGH)
            val eventChannel = NotificationChannel(eventChannelId, eventChannelId, NotificationManager.IMPORTANCE_DEFAULT)

            notificationManager.apply {
                createNotificationChannels(listOf(defaultChannel, eventChannel))
                notify(0, notificationBuilder.build())
            }

        } else {
            Log.d(TAG, "onMessageReceived: Payload is empty")
        }
    }


}