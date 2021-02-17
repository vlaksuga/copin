package com.copincomics.copinapp

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
        Log.d(TAG, "onMessageReceived: data = ${msg.rawData} ")

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = getString(R.string.default_notification_channel_id)
        val commonChannel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(commonChannel)
        Log.d(TAG, "createNotificationChannel: Created")

        val intent = Intent(applicationContext, EntryActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if(msg.data["link"] != null) {
            intent.putExtra("link", msg.data["link"])
        }

        val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT
        )

        val notificationBuilder = channelId.let {
            NotificationCompat.Builder(applicationContext, it)
                    .setSmallIcon(R.mipmap.icon_circle)
                    .setContentTitle(msg.notification!!.title)
                    .setContentText(msg.notification!!.body)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
        }

        if (notificationBuilder != null) {
            notificationManager.notify(0, notificationBuilder.build())
        }
    }


}