package com.screencast.noadb

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RentDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rent_device)
        
        // 设置返回按钮
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "租赁新设备"
        }
        
        // 设置设备类型选择器
        val deviceTypeSpinner = findViewById<Spinner>(R.id.spinner_device_type)
        val deviceTypes = arrayOf("Android 设备", "iOS 设备", "旧版 Android")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceTypeSpinner.adapter = adapter
        
        // 设置租赁时长选择
        val rentDurationGroup = findViewById<RadioGroup>(R.id.radio_group_duration)
        
        // 提交按钮
        val rentButton = findViewById<Button>(R.id.btn_rent_device)
        rentButton.setOnClickListener {
            val selectedDeviceType = deviceTypeSpinner.selectedItem.toString()
            
            val selectedDurationId = rentDurationGroup.checkedRadioButtonId
            val duration = when (selectedDurationId) {
                R.id.radio_1_month -> "1个月"
                R.id.radio_3_months -> "3个月"
                R.id.radio_6_months -> "6个月"
                R.id.radio_12_months -> "12个月"
                else -> "未选择"
            }
            
            // 在实际应用中，这里应该调用API进行设备租赁
            Toast.makeText(
                this,
                "正在租赁 $selectedDeviceType，时长: $duration",
                Toast.LENGTH_SHORT
            ).show()
            
            // 模拟租赁成功后返回
            finish()
        }
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