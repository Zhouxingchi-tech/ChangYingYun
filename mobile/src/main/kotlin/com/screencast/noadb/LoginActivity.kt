package com.screencast.noadb

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Random

class LoginActivity : AppCompatActivity() {

    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 隐藏默认ActionBar
        supportActionBar?.hide()

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences("user_info", MODE_PRIVATE)

        // 检查是否已登录
        if (isLoggedIn()) {
            navigateToDeviceList()
            return
        }

        // 初始化视图
        phoneEditText = findViewById(R.id.et_phone)
        passwordEditText = findViewById(R.id.et_password)
        loginButton = findViewById(R.id.btn_login)

        // 设置登录按钮点击事件
        loginButton.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val phone = phoneEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // 简单的输入验证
        if (phone.isEmpty()) {
            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return
        }

        // 模拟登录验证
        if (phone.length != 11) {
            Toast.makeText(this, "请输入正确的手机号", Toast.LENGTH_SHORT).show()
            return
        }

        // 登录成功，保存用户信息
        // 在实际应用中，这里应该调用后端API进行验证
        saveUserInfo(phone)
        
        // 跳转到设备列表页面
        navigateToDeviceList()
    }

    private fun saveUserInfo(phone: String) {
        // 生成随机用户ID
        val userId = generateRandomUserId()
        
        // 默认未实名
        val isVerified = false
        
        // 保存到SharedPreferences
        sharedPreferences.edit().apply {
            putString("phone_number", phone)
            putString("user_id", userId)
            putBoolean("is_verified", isVerified)
            putBoolean("is_logged_in", true)
            apply()
        }
    }
    
    private fun generateRandomUserId(): String {
        // 生成100-999之间的随机数作为用户ID
        return (Random().nextInt(900) + 100).toString()
    }
    
    private fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("is_logged_in", false)
    }
    
    private fun navigateToDeviceList() {
        val intent = Intent(this, DeviceListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
} 