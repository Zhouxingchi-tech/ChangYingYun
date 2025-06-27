package com.screencast.noadb

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.*
import java.net.NetworkInterface
import java.net.URISyntaxException

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 100
        private const val REQUEST_CODE_SCREEN_CAPTURE = 101
        private const val REQUEST_CODE_ACCESSIBILITY_SETTINGS = 102
        
        const val PREFS_NAME = "CloudPhonePrefs"
        const val KEY_AUTO_START = "autoStart"
        const val KEY_SERVER_URL = "serverUrl"
        const val KEY_AUTO_RECONNECT = "autoReconnect"
        const val KEY_VIDEO_QUALITY = "videoQuality"
        const val KEY_RESOLUTION = "resolution"
        const val KEY_CONTROL_DELAY = "controlDelay"
    }
    
    private lateinit var connectButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var serverAddressEditText: EditText
    private lateinit var deviceInfoTextView: TextView
    private lateinit var deviceNameTextView: TextView
    private lateinit var autoStartCheckBox: CheckBox
    
    private var socket: Socket? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    
    // WebRTC
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localVideoSource: VideoSource? = null
    private var peerConnection: PeerConnection? = null
    private var localSurfaceTextureHelper: SurfaceTextureHelper? = null
    
    private lateinit var prefs: SharedPreferences
    
    // 当前控制的设备信息
    private var deviceId: String? = null
    private var deviceName: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        connectButton = findViewById(R.id.connect_button)
        statusTextView = findViewById(R.id.status_text)
        serverAddressEditText = findViewById(R.id.server_address)
        deviceInfoTextView = findViewById(R.id.device_info)
        deviceNameTextView = findViewById(R.id.device_name)
        autoStartCheckBox = findViewById(R.id.auto_start_checkbox)
        
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 获取设备信息
        deviceId = intent.getStringExtra("DEVICE_ID")
        deviceName = intent.getStringExtra("DEVICE_NAME")
        
        // 设置标题
        if (deviceName != null) {
            title = "控制: $deviceName"
            deviceNameTextView.text = "设备: $deviceName"
            deviceNameTextView.visibility = TextView.VISIBLE
        } else {
            deviceNameTextView.visibility = TextView.GONE
        }
        
        // 加载保存的设置
        loadSettings()
        
        // 显示设备信息
        displayLocalIpAddress()
        displayDeviceInfo()
        
        connectButton.setOnClickListener {
            if (checkPermissions()) {
                saveSettings()
                startScreenCapture()
            }
        }
        
        // 检查无障碍服务是否已启用
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "请先启用无障碍服务", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        
        // 如果是从设备列表点击进入，并且已配置服务器地址，自动连接
        if (deviceId != null && autoStartCheckBox.isChecked && serverAddressEditText.text.isNotEmpty()) {
            if (checkPermissions()) {
                startScreenCapture()
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_app_manager -> {
                startActivity(Intent(this, AppManagerActivity::class.java))
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showAboutDialog() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        
        val aboutMessage = """
            畅速云
            版本: ${packageInfo.versionName} (${packageInfo.versionCode})
            
            高性能免ADB远程控制解决方案
            支持百里之外远程控制手机
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("关于畅速云")
            .setMessage(aboutMessage)
            .setPositiveButton("确定", null)
            .show()
    }
    
    private fun loadSettings() {
        serverAddressEditText.setText(prefs.getString(KEY_SERVER_URL, ""))
        autoStartCheckBox.isChecked = prefs.getBoolean(KEY_AUTO_START, false)
    }
    
    private fun saveSettings() {
        val editor = prefs.edit()
        editor.putString(KEY_SERVER_URL, serverAddressEditText.text.toString())
        editor.putBoolean(KEY_AUTO_START, autoStartCheckBox.isChecked)
        editor.apply()
    }
    
    private fun displayDeviceInfo() {
        val deviceInfo = StringBuilder()
        deviceInfo.append("设备: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        deviceInfo.append("Android: ${Build.VERSION.RELEASE}\n")
        deviceInfo.append("设备ID: ${getDeviceId()}")
        
        deviceInfoTextView.text = deviceInfo.toString()
    }
    
    private fun getDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }
    
    private fun displayLocalIpAddress() {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val inetAddresses = networkInterface.inetAddresses
                while (inetAddresses.hasMoreElements()) {
                    val address = inetAddresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress.indexOf(':') < 0) {
                        Log.d(TAG, "本机IP地址: ${address.hostAddress}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取IP地址失败: ${e.message}")
        }
    }
    
    private fun checkPermissions(): Boolean {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SYSTEM_ALERT_WINDOW
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.SYSTEM_ALERT_WINDOW)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
            return false
        }
        
        return true
    }
    
    private fun startScreenCapture() {
        val captureIntent = mediaProjectionManager?.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK) {
            startRemoteControlService(resultCode, data)
        } else if (requestCode == REQUEST_CODE_ACCESSIBILITY_SETTINGS) {
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "无障碍服务已启用", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "请启用无障碍服务", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        
        if (accessibilityEnabled == 1) {
            val serviceName = packageName + "/" + RemoteControlAccessibilityService::class.java.canonicalName
            val serviceEnabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            return serviceEnabled != null && serviceEnabled.contains(serviceName)
        }
        
        return false
    }
    
    private fun startRemoteControlService(resultCode: Int, data: Intent?) {
        try {
            val serverAddress = serverAddressEditText.text.toString()
            if (serverAddress.isBlank()) {
                Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show()
                return
            }
            
            val serviceIntent = Intent(this, RemoteControlService::class.java).apply {
                putExtra("server_url", serverAddress)
                putExtra("media_projection_result_code", resultCode)
                putExtra("media_projection_result_data", data)
                
                // 传递额外的设置参数
                putExtra("video_quality", prefs.getString(KEY_VIDEO_QUALITY, "60")?.toIntOrNull() ?: 60)
                putExtra("resolution", prefs.getString(KEY_RESOLUTION, "720p"))
                putExtra("auto_reconnect", prefs.getBoolean(KEY_AUTO_RECONNECT, true))
                putExtra("control_delay", prefs.getString(KEY_CONTROL_DELAY, "200")?.toIntOrNull() ?: 200)
                
                // 传递设备ID和名称
                putExtra("device_id", deviceId)
                putExtra("device_name", deviceName)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            statusTextView.text = "畅速云服务已启动"
            connectButton.text = "已连接"
            connectButton.isEnabled = false
            
            // 最小化应用
            moveTaskToBack(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "启动远程控制服务失败: ${e.message}")
            Toast.makeText(this, "启动服务失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startScreenCapture()
            } else {
                Toast.makeText(this, "需要所有权限才能运行", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun initWebRTC() {
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(this)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)
        
        val options = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
            EglBase.create().eglBaseContext, true, true
        )
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(
            EglBase.create().eglBaseContext
        )
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }
    
    private fun connectToSignalingServer() {
        try {
            val serverAddress = serverAddressEditText.text.toString()
            socket = IO.socket(serverAddress)
            
            socket?.on(Socket.EVENT_CONNECT) {
                runOnUiThread {
                    statusTextView.text = "已连接到服务器"
                    createPeerConnection()
                }
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) {
                runOnUiThread {
                    statusTextView.text = "已断开连接"
                }
            }
            
            socket?.on("offer") { args ->
                val offer = args[0] as JSONObject
                val sdp = SessionDescription(
                    SessionDescription.Type.OFFER,
                    offer.getString("sdp")
                )
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        createAnswer()
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            
            socket?.on("ice-candidate") { args ->
                val candidateJson = args[0] as JSONObject
                val candidate = IceCandidate(
                    candidateJson.getString("sdpMid"),
                    candidateJson.getInt("sdpMLineIndex"),
                    candidateJson.getString("candidate")
                )
                peerConnection?.addIceCandidate(candidate)
            }
            
            socket?.connect()
            
        } catch (e: URISyntaxException) {
            Log.e(TAG, "连接信令服务器错误: ${e.message}")
            Toast.makeText(this, "连接服务器失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        )
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidate(candidate: IceCandidate) {
                val json = JSONObject().apply {
                    put("candidate", candidate.sdp)
                    put("sdpMid", candidate.sdpMid)
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                }
                socket?.emit("ice-candidate", json)
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: DataChannel) {}
            override fun onRenegotiationNeeded() {
                createOffer()
            }
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}
        })
    }
    
    private fun createOffer() {
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        val json = JSONObject().apply {
                            put("sdp", sdp.description)
                            put("type", sdp.type.canonicalForm())
                        }
                        socket?.emit("offer", json)
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) {}
            override fun onSetFailure(error: String) {}
        }, MediaConstraints())
    }
    
    private fun createAnswer() {
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        val json = JSONObject().apply {
                            put("sdp", sdp.description)
                            put("type", sdp.type.canonicalForm())
                        }
                        socket?.emit("answer", json)
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) {}
            override fun onSetFailure(error: String) {}
        }, MediaConstraints())
    }
    
    private fun startStreaming() {
        statusTextView.text = "正在投屏..."
        
        // 创建视频源
        localVideoSource = peerConnectionFactory?.createVideoSource(false)
        localSurfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", EglBase.create().eglBaseContext)
        
        // 创建屏幕捕获视频轨道
        val screenCapturer = createScreenCapturer()
        screenCapturer.initialize(localSurfaceTextureHelper, this, localVideoSource?.capturerObserver)
        screenCapturer.startCapture(1280, 720, 30)
        
        val localVideoTrack = peerConnectionFactory?.createVideoTrack("screen_track", localVideoSource)
        
        // 添加轨道到PeerConnection
        localVideoTrack?.let {
            peerConnection?.addTrack(it, listOf("stream_id"))
        }
    }
    
    private fun createScreenCapturer(): VideoCapturer {
        return ScreenCapturerAndroid(
            mediaProjection, object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.e(TAG, "用户停止了屏幕共享")
                }
            }
        )
    }
    
    override fun onDestroy() {
        socket?.disconnect()
        localVideoSource?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        super.onDestroy()
    }
} 