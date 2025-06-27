package com.screencast.noadb

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DeviceInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_info)
        
        // 设置返回按钮
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "设备信息"
        }
        
        // 获取设备信息
        val deviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        val deviceName = intent.getStringExtra("DEVICE_NAME") ?: "云手机"
        
        // 设置设备名称
        findViewById<TextView>(R.id.device_name).text = deviceName
        
        // 加载设备信息
        loadDeviceInfo(deviceId)
    }
    
    private fun loadDeviceInfo(deviceId: String) {
        // 这里应该从API获取设备信息，这里使用模拟数据
        
        // 设置处理器信息
        findViewById<TextView>(R.id.processor_name).text = "骁龙 865"
        
        // 设置相机信息
        findViewById<TextView>(R.id.camera_info).text = "64+12+8+5MP"
        
        // 设置运行内存
        findViewById<TextView>(R.id.ram_info).text = "8GB"
        
        // 设置存储空间
        findViewById<TextView>(R.id.storage_info).text = "128GB"
        
        // 设置系统版本
        findViewById<TextView>(R.id.system_version).text = "Flyme 9.0.0.0A"
        
        // 设置Android版本
        findViewById<TextView>(R.id.android_version).text = "11"
        
        // 设置设备型号
        findViewById<TextView>(R.id.device_model).text = "MEIZU 17"
        findViewById<TextView>(R.id.model_name).text = "meizu 17"
        
        // 设置设备编号
        findViewById<TextView>(R.id.device_number).text = "M081Q"
        
        // 设置序列号
        findViewById<TextView>(R.id.serial_number).text = "Z81QAEZF2225X"
        
        // 设置IMEI信息
        findViewById<TextView>(R.id.imei1).text = "861647040460517"
        findViewById<TextView>(R.id.imei2).text = "861647040460525"
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 