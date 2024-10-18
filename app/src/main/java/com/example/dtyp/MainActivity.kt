package com.example.dtyp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dtyp.input.InputService
import com.example.dtyp.ui.theme.DTYPTheme
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern

const val TAG = "DTYP"

class MainActivity : ComponentActivity() {
    // ViewModel 在 MainActivity 中初始化，然后一路传下去
    private val store = MainStore()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DTYPTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomePage(store)
                }
            }
        }
    }
}

@Composable
fun HomePage(store: MainStore) {
    val context = LocalContext.current
    val isServiceRunning by store.isServiceRunning

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            if (isServiceRunning) {
                stopService(context, store)
            } else {
                startService(context, store)
            }
        }) {
            Text(if(isServiceRunning) "停止服务" else "启动服务")
        }
    }
}

fun startService(context: Context, store: MainStore) {
    if (store.isServiceRunning.value) return
    if (!checkOverlayPermission(context) || !checkRecordAudioPermission(context)) return

    startInputService(context)
    startFloatingWindowService(context, store)
    navToDesktop(context)
    store.isServiceRunning.value = true
}

fun stopService(context: Context, store: MainStore) {
    if (!store.isServiceRunning.value) return

    stopInputService(context)
    stopFloatingWindowService()
    navToApp(context)
    store.isServiceRunning.value = false
}

fun startInputService(context: Context) {
    Log.d(TAG, "startInputService")
    val intent = Intent(context, InputService::class.java)
    // 兼容方法：在 API 26 以上等价于 context.startForegroundService
    // 在旧设备上等价于 context.startService
    ContextCompat.startForegroundService(context, intent)
}

fun stopInputService(context: Context) {
    Log.d(TAG, "stopInputService")
    val intent = Intent(context, InputService::class.java)
    context.stopService(intent)
}

const val FloatingWindowTag = "fwt"
// 显示悬浮窗
fun startFloatingWindowService(context: Context, store: MainStore) {
    Log.d(TAG, "startFloatingWindowService")
    EasyFloat.with(context)
        .setLayout(R.layout.floating_window_layout) {
            it.findViewById<TextView>(R.id.exit).setOnClickListener {
                stopService(context, store)
            }
        }
        .setTag(FloatingWindowTag)
        .setShowPattern(ShowPattern.ALL_TIME)
        .setSidePattern(SidePattern.RESULT_SIDE)
        .setGravity(Gravity.RIGHT, 0, 256)
        .setDragEnable(true)
        .show()
}
// 关闭悬浮窗
fun stopFloatingWindowService() {
    Log.d(TAG, "stopFloatingWindowService")
    EasyFloat.dismiss(FloatingWindowTag)
}

// 检查悬浮窗权限
fun checkOverlayPermission(context: Context): Boolean {
    Log.d(TAG, "checkOverlayPermission")
    // API 23 以上，显示悬浮窗需要权限
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
        // 如果没有权限，拉起设置页引导用户授予
        Toast.makeText(context, "请允许 DTYP 显示在其它应用上层", Toast.LENGTH_SHORT).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        false
    } else {
        true
    }
}

// 检查录音权限
fun checkRecordAudioPermission(context: Context): Boolean {
    Log.d(TAG, "checkRecordAudioPermission")
    return if (ActivityCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "请允许 DTYP 访问您的麦克风",Toast.LENGTH_SHORT).show()
        val activity = context as? MainActivity
        if (activity != null) {
            Log.d(TAG, "requestPermissions: RECORD_AUDIO")
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
        false
    } else {
        true
    }
}

// 跳转桌面
fun navToDesktop(context: Context) {
    Log.d(TAG, "navToDesktop")
    val homeIntent = Intent(Intent.ACTION_MAIN)
    homeIntent.addCategory(Intent.CATEGORY_HOME)
    homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(homeIntent)
}

// 跳转本应用
fun navToApp(context: Context) {
    Log.d(TAG, "navToApp")
    val packageManager = context.packageManager
    val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
    launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
    context.startActivity(launchIntent)
}