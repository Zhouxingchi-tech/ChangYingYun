package com.screencast.noadb

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
        
        // 显示返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "畅速云设置"
    }
    
    // 处理返回按钮
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var sharedPreferences: SharedPreferences
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            
            // 服务器地址配置
            val serverUrlPreference = findPreference<EditTextPreference>("server_url")
            serverUrlPreference?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            
            // 自动启动配置
            val autoStartPreference = findPreference<SwitchPreferenceCompat>("auto_start")
            autoStartPreference?.setOnPreferenceChangeListener { _, newValue ->
                // 将新值保存到MainActivity使用的SharedPreferences中
                val mainPrefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                mainPrefs.edit().putBoolean(MainActivity.KEY_AUTO_START, newValue as Boolean).apply()
                true
            }
            
            // 自动重连配置
            val autoReconnectPreference = findPreference<SwitchPreferenceCompat>("auto_reconnect")
            autoReconnectPreference?.setOnPreferenceChangeListener { _, _ -> 
                true
            }
            
            // 视频质量配置
            val videoQualityPreference = findPreference<EditTextPreference>("video_quality")
            videoQualityPreference?.setOnPreferenceChangeListener { _, newValue ->
                val quality = (newValue as String).toIntOrNull()
                if (quality != null && quality in 1..100) {
                    true
                } else {
                    Toast.makeText(requireContext(), "请输入1-100之间的数值", Toast.LENGTH_SHORT).show()
                    false
                }
            }
            
            // 控制延迟配置
            val controlDelayPreference = findPreference<EditTextPreference>("control_delay")
            controlDelayPreference?.setOnPreferenceChangeListener { _, newValue ->
                val delay = (newValue as String).toIntOrNull()
                if (delay != null && delay >= 0) {
                    true
                } else {
                    Toast.makeText(requireContext(), "请输入不小于0的数值", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }
    }
} 