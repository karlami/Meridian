package com.arubanetworks.meridiansamples

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arubanetworks.meridian.campaigns.CampaignBroadcastReceiver
import java.util.*

class CampaignReceiver : CampaignBroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent, title: String, message: String) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
        builder.setContentTitle(title)
        builder.setContentText(message)
        builder.setSmallIcon(R.drawable.ic_launcher)
        builder.setDefaults(Notification.DEFAULT_ALL)
        builder.setContentIntent(contentIntent)
        builder.setAutoCancel(true)
        val nm: NotificationManager? = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm!!.notify("com.arubanetworks.meridiansamples.CampaignReceiver".hashCode(), builder.build())
    }

    override fun getUserInfoForCampaign(context: Context, campaignIdentifier: String): Map<String, String>? {
        val map = HashMap<String, String>()
        map["UserKey1"] = "userData1"
        map["UserKey2"] = "userData2"
        map["UserKey3"] = "userData3"
        return map
    }

    //override fun getPushRegistrationUserInfo(context: Context): Map<String, String>? {
    //val hm = HashMap<String, String>()
    //hm.put("TestKey", "TestVal");
    //return hm;

    //}

    companion object {
        var NOTIFICATION_CHANNEL = "NOTIFICATION_CHANNEL"

        fun createNotificationChannel(context: Context) {
            val notificationManager: NotificationManager? = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL, "Campaign Notifications", NotificationManager.IMPORTANCE_DEFAULT)

                // Configure the notification channel.
                notificationChannel.description = "Campaign Channel"
                notificationChannel.enableLights(true)
                notificationManager?.createNotificationChannel(notificationChannel)
            }
        }
    }
}
