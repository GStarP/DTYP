package com.example.dtyp.input

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dtyp.MainActivity
import com.example.dtyp.R

const val TAG = "DTYP"

class InputService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "InputService.onStartCommand")
        startForeground(1, createNotification())
        startRecord()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecord()
    }

    private fun startRecord() {
        Log.d(TAG, "InputService.startRecord")
    }

    private fun stopRecord() {
        Log.d(TAG, "InputService.stopRecord")
    }

    private fun createNotification(): Notification {
        Log.d(TAG, "InputService.createNotification")

        var channelId = "InputServiceNotification"
        // API 26 及以上需要自行创建 NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "InputService.createNotificationChannel")
            val channel = NotificationChannel(channelId, "DTYP Input Service", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // 这个 Intent 用于在点击通知时回到应用
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("DTYP 正在监听您的麦克风")
            .setContentText("点击返回应用")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            // 降低优先级，不要让用户明显感知到有一个通知
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}