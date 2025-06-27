package com.screencast.noadb

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UserProfileActivity : AppCompatActivity() {

    private lateinit var phoneTextView: TextView
    private lateinit var userIdTextView: TextView
    private lateinit var usernameLabel: TextView
    private lateinit var contactServiceButton: Button
    private lateinit var buyDeviceLayout: LinearLayout
    private lateinit var renewDeviceLayout: LinearLayout
    private lateinit var purchaseHistoryLayout: LinearLayout
    private lateinit var transferDeviceLayout: LinearLayout
    private lateinit var activationCodeLayout: LinearLayout
    private lateinit var cloudDiskLayout: LinearLayout
    private lateinit var changePasswordLayout: LinearLayout
    private lateinit var realNameAuthLayout: LinearLayout
    private lateinit var settingsLayout: LinearLayout
    private lateinit var navCloudPhone: LinearLayout
    private lateinit var navDiscover: LinearLayout
    private lateinit var navMe: LinearLayout
    
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // 隐藏默认ActionBar
        supportActionBar?.hide()

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences("user_info", MODE_PRIVATE)
        
        // 初始化视图
        initViews()
        
        // 设置点击事件
        setupClickListeners()
        
        // 加载用户信息
        loadUserInfo()
    }
    
    private fun initViews() {
        phoneTextView = findViewById(R.id.tv_phone)
        userIdTextView = findViewById(R.id.tv_user_id)
        usernameLabel = findViewById(R.id.tv_username_label)
        contactServiceButton = findViewById(R.id.btn_contact_service)
        
        buyDeviceLayout = findViewById(R.id.layout_buy_device)
        renewDeviceLayout = findViewById(R.id.layout_renew_device)
        purchaseHistoryLayout = findViewById(R.id.layout_purchase_history)
        transferDeviceLayout = findViewById(R.id.layout_transfer_device)
        activationCodeLayout = findViewById(R.id.layout_activation_code)
        cloudDiskLayout = findViewById(R.id.layout_cloud_disk)
        changePasswordLayout = findViewById(R.id.layout_change_password)
        realNameAuthLayout = findViewById(R.id.layout_real_name_auth)
        settingsLayout = findViewById(R.id.layout_settings)
        
        navCloudPhone = findViewById(R.id.nav_cloud_phone)
        navDiscover = findViewById(R.id.nav_discover)
        navMe = findViewById(R.id.nav_me)
    }
    
    private fun setupClickListeners() {
        // 联系客服按钮
        contactServiceButton.setOnClickListener {
            Toast.makeText(this, "正在连接客服...", Toast.LENGTH_SHORT).show()
        }
        
        // 购买设备
        buyDeviceLayout.setOnClickListener {
            startActivity(Intent(this, RentDeviceActivity::class.java))
        }
        
        // 续费设备
        renewDeviceLayout.setOnClickListener {
            Toast.makeText(this, "续费设备功能即将上线", Toast.LENGTH_SHORT).show()
        }
        
        // 购买记录
        purchaseHistoryLayout.setOnClickListener {
            Toast.makeText(this, "购买记录功能即将上线", Toast.LENGTH_SHORT).show()
        }
        
        // 转移云手机
        transferDeviceLayout.setOnClickListener {
            Toast.makeText(this, "转移云手机功能即将上线", Toast.LENGTH_SHORT).show()
        }
        
        // 激活码
        activationCodeLayout.setOnClickListener {
            Toast.makeText(this, "激活码功能即将上线", Toast.LENGTH_SHORT).show()
        }
        
        // 云盘
        cloudDiskLayout.setOnClickListener {
            Toast.makeText(this, "云盘功能即将上线", Toast.LENGTH_SHORT).show()
        }
        
        // 修改密码
        changePasswordLayout.setOnClickListener {
            Toast.makeText(this, "修改密码功能即将上线", Toast.LENGTH_SHORT).show()
        }
        
        // 实名认证
        realNameAuthLayout.setOnClickListener {
            // 如果未实名，跳转到实名认证页面
            if (!isUserVerified()) {
                Toast.makeText(this, "跳转到实名认证页面", Toast.LENGTH_SHORT).show()
                // 实际应用中应该跳转到实名认证页面
                // startActivity(Intent(this, RealNameAuthActivity::class.java))
            } else {
                Toast.makeText(this, "您已完成实名认证", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 设置
        settingsLayout.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // 底部导航栏
        navCloudPhone.setOnClickListener {
            startActivity(Intent(this, DeviceListActivity::class.java))
            finish()
        }
        
        navDiscover.setOnClickListener {
            Toast.makeText(this, "发现功能即将上线", Toast.LENGTH_SHORT).show()
        }
        
        // 已在我的页面，无需操作
        navMe.setOnClickListener {
            // 已在当前页面
        }
    }
    
    private fun loadUserInfo() {
        // 从SharedPreferences或API获取用户信息
        val phoneNumber = getUserPhoneNumber()
        val userId = getUserId()
        val isVerified = isUserVerified()
        
        // 设置用户信息
        phoneTextView.text = phoneNumber
        userIdTextView.text = "ID: $userId"
        
        // 设置实名状态
        if (isVerified) {
            usernameLabel.text = "已实名"
        } else {
            usernameLabel.text = "未实名"
        }
    }
    
    private fun getUserPhoneNumber(): String {
        // 实际应用中，这里应该从API或SharedPreferences获取用户手机号
        // 这里使用SharedPreferences示例，实际项目中可能从登录后的用户信息中获取
        return sharedPreferences.getString("phone_number", "未登录") ?: "未登录"
    }
    
    private fun getUserId(): String {
        // 实际应用中，这里应该从API或SharedPreferences获取用户ID
        return sharedPreferences.getString("user_id", "000") ?: "000"
    }
    
    private fun isUserVerified(): Boolean {
        // 实际应用中，这里应该从API或SharedPreferences获取用户实名状态
        return sharedPreferences.getBoolean("is_verified", false)
    }
    
    override fun onResume() {
        super.onResume()
        // 每次页面重新显示时刷新用户信息，确保数据是最新的
        loadUserInfo()
    }
    
    // 模拟登录后保存用户信息的方法，实际项目中应该在登录成功后调用
    private fun saveUserInfo(phoneNumber: String, userId: String, isVerified: Boolean) {
        sharedPreferences.edit().apply {
            putString("phone_number", phoneNumber)
            putString("user_id", userId)
            putBoolean("is_verified", isVerified)
            apply()
        }
    }
    
    // 用于测试的方法，实际项目中可以删除
    private fun testSaveUserInfo() {
        // 保存测试数据
        saveUserInfo("13231022975", "291", false)
        // 刷新UI
        loadUserInfo()
    }
} 