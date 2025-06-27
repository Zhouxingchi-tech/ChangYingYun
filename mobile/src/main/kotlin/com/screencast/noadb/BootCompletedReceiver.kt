package com.screencast.noadb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.content.SharedPreferences

class BootCompletedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootCompletedReceiver"
        private const val PREFS_NAME = "CloudPhonePrefs"
        private const val KEY_AUTO_START = "autoStart"
        private const val KEY_SERVER_URL = "serverUrl"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "设备启动完成")
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean(KEY_AUTO_START, false)
            
            if (autoStart) {
                val serverUrl = prefs.getString(KEY_SERVER_URL, null)
                if (serverUrl != null) {
                    Log.d(TAG, "自动启动云手机服务")
                    startRemoteControlService(context, serverUrl)
                }
            }
        }
    }
    
    private fun startRemoteControlService(context: Context, serverUrl: String) {
        val serviceIntent = Intent(context, RemoteControlService::class.java).apply {
            putExtra("server_url", serverUrl)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
} 