package com.screencast.noadb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import androidx.core.app.NotificationCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.*
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class RemoteControlService : Service() {
    
    companion object {
        private const val TAG = "RemoteControlService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "remote_control_channel"
        private const val CHANNEL_NAME = "Remote Control Service"
        private const val VIRTUAL_DISPLAY_NAME = "screencast_display"
    }
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var socket: Socket? = null
    private var serverUrl: String? = null
    
    // WebRTC
    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localVideoSource: VideoSource? = null
    private var peerConnection: PeerConnection? = null
    private var localSurfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoCapturer: VideoCapturer? = null
    private var dataChannel: DataChannel? = null
    
    // 屏幕尺寸
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var screenDensity: Int = 0
    
    // 线程池
    private val executor = Executors.newSingleThreadExecutor()
    
    // 添加下面的成员变量在类的顶部
    private var videoQuality: Int = 60 // 1-100
    private var autoReconnect: Boolean = true
    private var controlDelay: Int = 200 // 毫秒
    private var captureWidth: Int = 1280
    private var captureHeight: Int = 720
    private var captureFps: Int = 30
    
    // 设备信息
    private var deviceId: String? = null
    private var deviceName: String? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // 获取屏幕尺寸
        val displayMetrics = resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        screenDensity = displayMetrics.densityDpi
        
        // 初始化WebRTC
        initWebRTC()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.hasExtra("server_url")) {
                serverUrl = intent.getStringExtra("server_url")
                connectToSignalingServer()
            }
            
            if (intent.hasExtra("media_projection_result_code") && intent.hasExtra("media_projection_result_data")) {
                val resultCode = intent.getIntExtra("media_projection_result_code", 0)
                val resultData = intent.getParcelableExtra<Intent>("media_projection_result_data")
                
                val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData!!)
                startScreenCapture()
            }
            
            // 获取设备信息
            if (intent.hasExtra("device_id")) {
                deviceId = intent.getStringExtra("device_id")
            }
            
            if (intent.hasExtra("device_name")) {
                deviceName = intent.getStringExtra("device_name")
                // 更新通知内容
                updateNotification()
            }
            
            // 获取视频质量设置
            if (intent.hasExtra("video_quality")) {
                videoQuality = intent.getIntExtra("video_quality", 60)
                Log.d(TAG, "设置视频质量: $videoQuality")
            }
            
            // 获取分辨率设置
            if (intent.hasExtra("resolution")) {
                val resolution = intent.getStringExtra("resolution") ?: "720p"
                setResolution(resolution)
                Log.d(TAG, "设置分辨率: $resolution")
            }
            
            // 获取自动重连设置
            if (intent.hasExtra("auto_reconnect")) {
                autoReconnect = intent.getBooleanExtra("auto_reconnect", true)
                Log.d(TAG, "设置自动重连: $autoReconnect")
            }
            
            // 获取控制延迟设置
            if (intent.hasExtra("control_delay")) {
                controlDelay = intent.getIntExtra("control_delay", 200)
                Log.d(TAG, "设置控制延迟: $controlDelay ms")
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        stopScreenCapture()
        releaseWebRTC()
        socket?.disconnect()
        executor.shutdown()
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
    
    private fun createNotification(): Notification {
        val notificationTitle = if (deviceName != null) "控制: $deviceName" else "云手机服务"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText("正在运行远程控制服务")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    fun setMediaProjection(projection: MediaProjection) {
        mediaProjection = projection
        startScreenCapture()
    }
    
    private fun startScreenCapture() {
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection未初始化")
            return
        }
        
        // 创建屏幕捕获器
        try {
            videoCapturer = createScreenCapturer()
            videoCapturer?.initialize(localSurfaceTextureHelper, this, localVideoSource?.capturerObserver)
            videoCapturer?.startCapture(captureWidth, captureHeight, captureFps)
            
            // 创建视频轨道并添加到PeerConnection
            val localVideoTrack = peerConnectionFactory?.createVideoTrack("screen_track", localVideoSource)
            localVideoTrack?.let {
                peerConnection?.addTrack(it, listOf("stream_id"))
            }
            
            // 创建数据通道用于接收控制命令
            createDataChannel()
            
        } catch (e: Exception) {
            Log.e(TAG, "启动屏幕捕获失败: ${e.message}")
        }
    }
    
    private fun createDataChannel() {
        val init = DataChannel.Init()
        dataChannel = peerConnection?.createDataChannel("control_channel", init)
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}
            
            override fun onStateChange() {
                Log.d(TAG, "数据通道状态改变: ${dataChannel?.state()}")
            }
            
            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = buffer.data
                val bytes = ByteArray(data.remaining())
                data.get(bytes)
                val message = String(bytes)
                handleRemoteControl(message)
            }
        })
    }
    
    private fun stopScreenCapture() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null
            virtualDisplay?.release()
            virtualDisplay = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "停止屏幕捕获失败: ${e.message}")
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
    
    private fun initWebRTC() {
        try {
            // 初始化EGL上下文
            eglBase = EglBase.create()
            
            // 初始化WebRTC
            val options = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)
            
            // 创建编解码器工厂
            val encoderFactory = DefaultVideoEncoderFactory(eglBase?.eglBaseContext, true, true)
            val decoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)
            
            // 创建PeerConnectionFactory
            val factoryOptions = PeerConnectionFactory.Options()
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(factoryOptions)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()
            
            // 创建视频源
            localVideoSource = peerConnectionFactory?.createVideoSource(false)
            localSurfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext)
            
        } catch (e: Exception) {
            Log.e(TAG, "初始化WebRTC失败: ${e.message}")
        }
    }
    
    private fun releaseWebRTC() {
        try {
            dataChannel?.dispose()
            dataChannel = null
            localVideoSource?.dispose()
            localVideoSource = null
            localSurfaceTextureHelper?.dispose()
            localSurfaceTextureHelper = null
            peerConnection?.close()
            peerConnection = null
            peerConnectionFactory?.dispose()
            peerConnectionFactory = null
            eglBase?.release()
            eglBase = null
        } catch (e: Exception) {
            Log.e(TAG, "释放WebRTC资源失败: ${e.message}")
        }
    }
    
    private fun connectToSignalingServer() {
        try {
            Log.d(TAG, "正在连接信令服务器: $serverUrl")
            val options = IO.Options()
            options.reconnection = true
            socket = IO.socket(serverUrl, options)
            
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "已连接到信令服务器")
                
                // 发送设备标识信息
                val deviceInfo = JSONObject().apply {
                    put("deviceId", deviceId ?: "unknown")
                    put("deviceName", deviceName ?: "未命名设备")
                    put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("osVersion", "Android ${Build.VERSION.RELEASE}")
                }
                socket?.emit("register_device", deviceInfo)
                
                sendDeviceInfo()
                createPeerConnection()
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "与信令服务器断开连接")
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "连接信令服务器失败: ${args[0]}")
            }
            
            // WebRTC信令处理
            socket?.on("offer") { args ->
                val offerJson = args[0] as JSONObject
                val sdp = SessionDescription(
                    SessionDescription.Type.OFFER,
                    offerJson.getString("sdp")
                )
                
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d(TAG, "RemoteDescription设置成功，创建Answer")
                        createAnswer()
                    }
                    override fun onCreateFailure(p0: String?) {
                        Log.e(TAG, "创建Answer失败: $p0")
                    }
                    override fun onSetFailure(p0: String?) {
                        Log.e(TAG, "设置RemoteDescription失败: $p0")
                    }
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
            
            socket?.on("remote-control") { args ->
                val commandJson = args[0] as JSONObject
                handleRemoteControl(commandJson.toString())
            }
            
            socket?.connect()
            
        } catch (e: Exception) {
            Log.e(TAG, "连接信令服务器失败: ${e.message}")
        }
    }
    
    private fun createPeerConnection() {
        try {
            val rtcConfig = PeerConnection.RTCConfiguration(listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            ))
            
            // 配置WebRTC支持低延迟
            rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            
            peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState) {
                    Log.d(TAG, "信令状态改变: $state")
                }
                
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    Log.d(TAG, "ICE连接状态改变: $state")
                }
                
                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    Log.d(TAG, "ICE连接接收状态改变: $receiving")
                }
                
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                    Log.d(TAG, "ICE收集状态改变: $state")
                }
                
                override fun onIceCandidate(candidate: IceCandidate) {
                    Log.d(TAG, "发现ICE候选: ${candidate.sdp}")
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
                
                override fun onDataChannel(channel: DataChannel) {
                    Log.d(TAG, "收到远程数据通道: ${channel.label()}")
                    if (dataChannel == null) {
                        dataChannel = channel
                        setupDataChannel(channel)
                    }
                }
                
                override fun onRenegotiationNeeded() {
                    Log.d(TAG, "需要重新协商")
                    createOffer()
                }
                
                override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "创建PeerConnection失败: ${e.message}")
        }
    }
    
    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(TAG, "创建Offer成功")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    
                    override fun onSetSuccess() {
                        Log.d(TAG, "设置LocalDescription成功")
                        val offerJson = JSONObject().apply {
                            put("sdp", sdp.description)
                            put("type", sdp.type.canonicalForm())
                        }
                        socket?.emit("offer", offerJson)
                    }
                    
                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "创建LocalDescription失败: $error")
                    }
                    
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "设置LocalDescription失败: $error")
                    }
                }, sdp)
            }
            
            override fun onSetSuccess() {}
            
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "创建Offer失败: $error")
            }
            
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.d(TAG, "创建Answer成功")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    
                    override fun onSetSuccess() {
                        Log.d(TAG, "设置Answer的LocalDescription成功")
                        val answerJson = JSONObject().apply {
                            put("sdp", sdp.description)
                            put("type", sdp.type.canonicalForm())
                        }
                        socket?.emit("answer", answerJson)
                    }
                    
                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "创建Answer的LocalDescription失败: $error")
                    }
                    
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "设置Answer的LocalDescription失败: $error")
                    }
                }, sdp)
            }
            
            override fun onSetSuccess() {}
            
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "创建Answer失败: $error")
            }
            
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    private fun setupDataChannel(channel: DataChannel) {
        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}
            
            override fun onStateChange() {
                Log.d(TAG, "数据通道状态改变: ${channel.state()}")
            }
            
            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = buffer.data
                val bytes = ByteArray(data.remaining())
                data.get(bytes)
                val message = String(bytes)
                handleRemoteControl(message)
            }
        })
    }
    
    private fun handleRemoteControl(message: String) {
        try {
            Log.d(TAG, "收到远程控制命令: $message")
            
            // 如果设置了控制延迟，则添加延迟
            if (controlDelay > 0) {
                executor.execute {
                    try {
                        Thread.sleep(controlDelay.toLong())
                        processControlCommand(message)
                    } catch (e: InterruptedException) {
                        Log.e(TAG, "控制延迟被中断: ${e.message}")
                    }
                }
            } else {
                processControlCommand(message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理远程控制指令失败: ${e.message}")
        }
    }
    
    private fun processControlCommand(message: String) {
        try {
            val command = JSONObject(message)
            val action = command.getString("action")
            
            when (action) {
                "tap" -> {
                    val x = command.getInt("x")
                    val y = command.getInt("y")
                    executeTap(x, y)
                }
                "swipe" -> {
                    val startX = command.getInt("startX")
                    val startY = command.getInt("startY")
                    val endX = command.getInt("endX")
                    val endY = command.getInt("endY")
                    val duration = command.getInt("duration")
                    executeSwipe(startX, startY, endX, endY, duration)
                }
                "key" -> {
                    val keyCode = command.getInt("keyCode")
                    executeKeyEvent(keyCode)
                }
                "text" -> {
                    val text = command.getString("text")
                    executeInputText(text)
                }
                "keepAlive" -> {
                    sendResponse("keepAliveAck", JSONObject())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理控制命令失败: ${e.message}")
        }
    }
    
    private fun executeTap(x: Int, y: Int) {
        executor.execute {
            try {
                val command = "input tap $x $y"
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                Log.d(TAG, "执行点击: $x, $y")
                sendControlResponse(true, "tap", "点击成功")
            } catch (e: Exception) {
                Log.e(TAG, "执行点击失败: ${e.message}")
                sendControlResponse(false, "tap", "点击失败: ${e.message}")
            }
        }
    }
    
    private fun executeSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Int) {
        executor.execute {
            try {
                val command = "input swipe $startX $startY $endX $endY $duration"
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                Log.d(TAG, "执行滑动")
                sendControlResponse(true, "swipe", "滑动成功")
            } catch (e: Exception) {
                Log.e(TAG, "执行滑动失败: ${e.message}")
                sendControlResponse(false, "swipe", "滑动失败: ${e.message}")
            }
        }
    }
    
    private fun executeKeyEvent(keyCode: Int) {
        executor.execute {
            try {
                val command = "input keyevent $keyCode"
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                Log.d(TAG, "执行按键: $keyCode")
                sendControlResponse(true, "key", "按键成功")
            } catch (e: Exception) {
                Log.e(TAG, "执行按键失败: ${e.message}")
                sendControlResponse(false, "key", "按键失败: ${e.message}")
            }
        }
    }
    
    private fun executeInputText(text: String) {
        executor.execute {
            try {
                val escapedText = text.replace(" ", "%s")
                val command = "input text $escapedText"
                Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                Log.d(TAG, "输入文本: $text")
                sendControlResponse(true, "text", "输入文本成功")
            } catch (e: Exception) {
                Log.e(TAG, "输入文本失败: ${e.message}")
                sendControlResponse(false, "text", "输入文本失败: ${e.message}")
            }
        }
    }
    
    private fun sendControlResponse(success: Boolean, type: String, message: String) {
        try {
            val response = JSONObject().apply {
                put("type", "controlResponse")
                put("action", type)
                put("success", success)
                put("message", message)
            }
            sendResponse("controlResponse", response)
        } catch (e: Exception) {
            Log.e(TAG, "发送控制响应失败: ${e.message}")
        }
    }
    
    private fun sendDeviceInfo() {
        try {
            val deviceInfo = JSONObject().apply {
                put("type", "deviceInfo")
                put("model", Build.MODEL)
                put("manufacturer", Build.MANUFACTURER)
                put("osVersion", Build.VERSION.RELEASE)
                put("screenWidth", screenWidth)
                put("screenHeight", screenHeight)
                put("screenDensity", screenDensity)
                put("deviceId", Build.SERIAL)
            }
            sendResponse("deviceInfo", deviceInfo)
        } catch (e: Exception) {
            Log.e(TAG, "发送设备信息失败: ${e.message}")
        }
    }
    
    private fun sendResponse(event: String, data: JSONObject) {
        if (dataChannel?.state() == DataChannel.State.OPEN) {
            val buffer = ByteBuffer.wrap(data.toString().toByteArray())
            dataChannel?.send(DataChannel.Buffer(buffer, false))
        } else {
            socket?.emit(event, data)
        }
    }
    
    private fun setResolution(resolution: String) {
        when (resolution) {
            "1080p" -> {
                captureWidth = 1920
                captureHeight = 1080
                captureFps = 30
            }
            "720p" -> {
                captureWidth = 1280
                captureHeight = 720
                captureFps = 30
            }
            "480p" -> {
                captureWidth = 854
                captureHeight = 480
                captureFps = 30
            }
            "360p" -> {
                captureWidth = 640
                captureHeight = 360
                captureFps = 30
            }
            "240p" -> {
                captureWidth = 426
                captureHeight = 240
                captureFps = 15
            }
            else -> {
                Log.w(TAG, "未知分辨率: $resolution, 使用默认值720p")
                captureWidth = 1280
                captureHeight = 720
                captureFps = 30
            }
        }
    }
    
    private fun calculateVideoBitrate(quality: Int): Int {
        // 根据质量等级调整比特率，范围从500kbps到8Mbps
        val minBitrate = 500_000  // 500kbps
        val maxBitrate = 8_000_000  // 8Mbps
        
        return minBitrate + (maxBitrate - minBitrate) * quality / 100
    }
} 