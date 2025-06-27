package com.screencast.noadb

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import org.json.JSONObject

class RemoteControlAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "RemoteControlService"
        const val ACTION_EXECUTE_COMMAND = "com.screencast.noadb.ACTION_EXECUTE_COMMAND"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_PARAMS = "params"
    }
    
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_EXECUTE_COMMAND) {
                val command = intent.getStringExtra(EXTRA_COMMAND) ?: return
                val params = intent.getStringExtra(EXTRA_PARAMS)
                
                handleAccessibilityCommand(command, params)
            }
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter(ACTION_EXECUTE_COMMAND)
        registerReceiver(commandReceiver, filter)
        
        Log.d(TAG, "无障碍服务已启动")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 处理辅助功能事件
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务中断")
    }
    
    override fun onDestroy() {
        try {
            unregisterReceiver(commandReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "注销接收器失败: ${e.message}")
        }
        super.onDestroy()
    }
    
    private fun handleAccessibilityCommand(command: String, paramsJson: String?) {
        Log.d(TAG, "处理命令: $command, 参数: $paramsJson")
        
        try {
            val params = if (paramsJson != null) JSONObject(paramsJson) else null
            
            when (command) {
                "tap" -> {
                    if (params != null) {
                        val x = params.getInt("x")
                        val y = params.getInt("y")
                        performTap(x, y)
                    }
                }
                "swipe" -> {
                    if (params != null) {
                        val startX = params.getInt("startX")
                        val startY = params.getInt("startY")
                        val endX = params.getInt("endX")
                        val endY = params.getInt("endY")
                        val duration = params.getInt("duration")
                        performSwipe(startX, startY, endX, endY, duration.toLong())
                    }
                }
                "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
                "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
                "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                "quickSettings" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                "lockScreen" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    }
                }
                "takeScreenshot" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                    }
                }
                "clickNode" -> {
                    if (params != null) {
                        val viewId = params.optString("viewId")
                        val text = params.optString("text")
                        clickNodeByIdentifier(viewId, text)
                    }
                }
                "setText" -> {
                    if (params != null) {
                        val viewId = params.optString("viewId")
                        val text = params.optString("text")
                        setTextToField(viewId, text)
                    }
                }
                "launchApp" -> {
                    if (params != null) {
                        val packageName = params.getString("package")
                        launchApp(packageName)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理命令失败: ${e.message}")
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.N)
    private fun performTap(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
        
        val gesture = gestureBuilder.build()
        dispatchGesture(gesture, null, null)
    }
    
    @RequiresApi(Build.VERSION_CODES.N)
    private fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long) {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        
        val gesture = gestureBuilder.build()
        dispatchGesture(gesture, null, null)
    }
    
    private fun clickNodeByIdentifier(viewId: String, text: String) {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // 尝试通过ID查找
            if (viewId.isNotEmpty()) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                for (node in nodes) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    node.recycle()
                    return
                }
            }
            
            // 尝试通过文本查找
            if (text.isNotEmpty()) {
                val nodes = rootNode.findAccessibilityNodeInfosByText(text)
                for (node in nodes) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    node.recycle()
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "查找节点失败: ${e.message}")
        } finally {
            rootNode.recycle()
        }
    }
    
    private fun setTextToField(viewId: String, text: String) {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // 尝试通过ID查找
            if (viewId.isNotEmpty()) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                for (node in nodes) {
                    val arguments = Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text
                    )
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    node.recycle()
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "设置文本失败: ${e.message}")
        } finally {
            rootNode.recycle()
        }
    }
    
    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                Log.e(TAG, "找不到应用的启动Intent: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动应用失败: ${e.message}")
        }
    }
} 