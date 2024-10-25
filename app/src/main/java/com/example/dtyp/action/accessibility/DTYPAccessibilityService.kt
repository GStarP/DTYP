package com.example.dtyp.action.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService

class DTYPAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "DTYPAccessibilityService"
        const val SERVICE_PACKAGE_NAME = "com.example.dtyp"
        const val INTENT_FILTER_NAME = "com.example.dtyp.action"
        const val INTENT_KEY_ACTION = "dtyp_action"
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val type = intent.getStringExtra(INTENT_KEY_ACTION)
            Log.d(TAG, "action type: $type")
            if (type != null) {
                when (DTYPActionType.valueOf(type)) {
                    DTYPActionType.SWIPE_UP -> {
                        actionSwipeUp()
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter(INTENT_FILTER_NAME)
        registerReceiver(receiver, filter)
        Log.d(TAG, "registerReceiver")
    }
    override fun onUnbind(intent: Intent?): Boolean {
        unregisterReceiver(receiver)
        Log.d(TAG, "unregisterReceiver")
        return super.onUnbind(intent)
    }
    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        Log.d(TAG, "onAccessibilityEvent")
    }
    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
    }

    private fun actionSwipeUp() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val startX = screenWidth / 2
        val startY = screenHeight * 0.66
        val endX = startX
        val endY = screenHeight * 0.33

        mockSwipe(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat())
    }

    private fun mockSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 200) {
        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))

        val gesture = gestureBuilder.build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Log.d("MyAccessibilityService.mockSwipe", "completed")
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                Log.e("MyAccessibilityService.mockSwipe", "canceled")
            }
        }, null)
    }
}

enum class DTYPActionType {
    SWIPE_UP
}

fun checkAccessibilityPermission(context: Context): Boolean {
    val manager = getSystemService(context, AccessibilityManager::class.java) as AccessibilityManager
    var enabled = false
    // 遍历已启用的无障碍服务，匹配当前服务
    val enabledServices = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    for (enabledService in enabledServices) {
        val enabledServiceInfo = enabledService.resolveInfo.serviceInfo
        if (enabledServiceInfo.packageName == DTYPAccessibilityService.SERVICE_PACKAGE_NAME) {
            enabled = true
            break
        }
    }

    return if (enabled) {
        true
    } else {
        Toast.makeText(context, "请启用 DTYP 的无障碍服务", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
        false
    }
}