package com.screencast.noadb

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class DeviceListActivity : AppCompatActivity() {
    
    companion object {
        const val VIEW_MODE_SINGLE = 1
        const val VIEW_MODE_GRID_4 = 4
        const val VIEW_MODE_GRID_9 = 9
        const val VIEW_MODE_GRID_16 = 16
        const val VIEW_MODE_GRID_36 = 36
        const val VIEW_MODE_LIST = 0
        
        const val GROUP_ALL = "all"
        const val GROUP_EXPIRING = "expiring"
        const val GROUP_MAINTENANCE = "maintenance"
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var emptyView: TextView
    private lateinit var addDeviceButton: Button
    private lateinit var batchOperationButton: Button
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var titleTextView: TextView
    
    private var currentViewMode = VIEW_MODE_GRID_4 // 默认4宫格展示
    private var currentGroup = GROUP_ALL // 默认显示所有设备
    private var deviceList = mutableListOf<DeviceInfo>()
    private var isInBatchMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        
        recyclerView = findViewById(R.id.device_recycler_view)
        emptyView = findViewById(R.id.empty_view)
        addDeviceButton = findViewById(R.id.btn_add_device)
        batchOperationButton = findViewById(R.id.btn_batch_operation)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        titleTextView = findViewById(R.id.title_text)
        
        // 设置标题
        titleTextView.text = "畅映云手机"
        
        setupRecyclerView()
        setupBottomNavigation()
        loadDevices()
        
        addDeviceButton.setOnClickListener {
            // 打开添加设备页面
            startActivity(Intent(this, RentDeviceActivity::class.java))
        }
        
        batchOperationButton.setOnClickListener {
            showBatchOperationMenu(it)
        }
    }
    
    private fun setupRecyclerView() {
        if (currentViewMode == VIEW_MODE_LIST) {
            recyclerView.layoutManager = LinearLayoutManager(this)
        } else {
            recyclerView.layoutManager = GridLayoutManager(this, getColumnCount())
        }
        
        deviceAdapter = DeviceAdapter(deviceList, currentViewMode) { device, isLongClick -> 
            if (isLongClick) {
                toggleBatchMode()
                return@DeviceAdapter
            }
            
            if (isInBatchMode) {
                device.isSelected = !device.isSelected
                deviceAdapter.notifyDataSetChanged()
                updateBatchOperationButton()
            } else {
                // 设备点击事件处理
                openDeviceControl(device)
            }
        }
        recyclerView.adapter = deviceAdapter
    }
    
    private fun getColumnCount(): Int {
        return when(currentViewMode) {
            VIEW_MODE_SINGLE -> 1
            VIEW_MODE_GRID_4 -> 2
            VIEW_MODE_GRID_9 -> 3
            VIEW_MODE_GRID_16 -> 4
            VIEW_MODE_GRID_36 -> 6
            else -> 4 // 默认4列
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_cloud_phone -> {
                    // 已经在云手机页面
                    true
                }
                R.id.nav_discover -> {
                    // 打开发现页面
                    Toast.makeText(this, "发现功能即将上线", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_me -> {
                    // 打开我的页面
                    startActivity(Intent(this, UserProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadDevices() {
        // 这里应该从API加载设备列表，这里先使用模拟数据
        deviceList.clear()
        
        // 根据当前分组过滤设备
        val allDevices = getSimulatedDevices()
        
        deviceList.addAll(when (currentGroup) {
            GROUP_EXPIRING -> allDevices.filter { it.daysLeft <= 3 }
            GROUP_MAINTENANCE -> allDevices.filter { it.status == "maintenance" }
            else -> allDevices
        })
        
        deviceAdapter.updateDevices(deviceList)
        
        // 显示或隐藏空视图
        if (deviceList.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun getSimulatedDevices(): List<DeviceInfo> {
        val devices = mutableListOf<DeviceInfo>()
        
        // 添加模拟数据
        devices.add(DeviceInfo("健QQ小号", "1001", "online", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("健微信大号", "1002", "online", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("刘QQ1号", "1003", "online", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("刘QQ2号", "1004", "online", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("刘QQ3号", "1005", "online", 2, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("刘微信大号", "1006", "online", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("刘微信小号", "1007", "online", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("宝QQ1号", "1008", "offline", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("宝QQ2号", "1009", "offline", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("宝QQ3号", "1010", "maintenance", 46, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("潘QQ1号", "1011", "online", 1, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("潘QQ2号", "1012", "online", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("潘QQ3号", "1013", "offline", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("潘微信大号", "1014", "maintenance", 26, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("潘微信小号", "1015", "online", 3, false, "默认分组", "MEIZU 17"))
        devices.add(DeviceInfo("永久钓鱼号", "1016", "online", 104, false, "默认分组", "MEIZU 17"))
        
        return devices
    }
    
    private fun openDeviceControl(device: DeviceInfo) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("DEVICE_ID", device.id)
            putExtra("DEVICE_NAME", device.name)
        }
        startActivity(intent)
    }
    
    private fun openDeviceInfo(device: DeviceInfo) {
        val intent = Intent(this, DeviceInfoActivity::class.java).apply {
            putExtra("DEVICE_ID", device.id)
            putExtra("DEVICE_NAME", device.name)
            putExtra("DEVICE_MODEL", device.model)
        }
        startActivity(intent)
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.device_list_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_view_mode -> {
                showViewModeOptions()
                true
            }
            R.id.action_group_filter -> {
                showGroupOptions()
                true
            }
            R.id.action_batch -> {
                toggleBatchMode()
                true
            }
            R.id.action_refresh -> {
                loadDevices()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showViewModeOptions() {
        val view = findViewById<View>(R.id.action_view_mode) ?: return
        
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.view_mode_menu, popupMenu.menu)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_single_view -> {
                    changeViewMode(VIEW_MODE_SINGLE)
                    true
                }
                R.id.action_4_grid -> {
                    changeViewMode(VIEW_MODE_GRID_4)
                    true
                }
                R.id.action_9_grid -> {
                    changeViewMode(VIEW_MODE_GRID_9)
                    true
                }
                R.id.action_16_grid -> {
                    changeViewMode(VIEW_MODE_GRID_16)
                    true
                }
                R.id.action_36_grid -> {
                    changeViewMode(VIEW_MODE_GRID_36)
                    true
                }
                R.id.action_list_view -> {
                    changeViewMode(VIEW_MODE_LIST)
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    private fun showGroupOptions() {
        val view = findViewById<View>(R.id.action_group_filter) ?: return
        
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.group_menu, popupMenu.menu)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_all_devices -> {
                    changeGroup(GROUP_ALL)
                    true
                }
                R.id.action_expiring_devices -> {
                    changeGroup(GROUP_EXPIRING)
                    true
                }
                R.id.action_maintenance_devices -> {
                    changeGroup(GROUP_MAINTENANCE)
                    true
                }
                R.id.action_manage_groups -> {
                    showGroupManagement()
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    private fun showBatchOperationMenu(view: View) {
        if (!isInBatchMode) {
            toggleBatchMode()
            return
        }
        
        val selectedCount = deviceList.count { it.isSelected }
        if (selectedCount == 0) {
            Toast.makeText(this, "请先选择设备", Toast.LENGTH_SHORT).show()
            return
        }
        
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.batch_operation_menu, popupMenu.menu)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_batch_add -> {
                    showBatchAddDialog()
                    true
                }
                R.id.action_batch_renew -> {
                    showBatchRenewDialog()
                    true
                }
                R.id.action_group_management -> {
                    showGroupManagementDialog()
                    true
                }
                R.id.action_batch_restart -> {
                    performBatchRestart()
                    true
                }
                R.id.action_batch_restore -> {
                    performBatchRestore()
                    true
                }
                R.id.action_batch_change -> {
                    showChangeDeviceDialog()
                    true
                }
                R.id.action_batch_transfer -> {
                    showTransferDialog()
                    true
                }
                R.id.action_batch_upload -> {
                    showUploadAppDialog()
                    true
                }
                R.id.action_batch_clone -> {
                    showCloneDialog()
                    true
                }
                R.id.action_batch_other -> {
                    showOtherOperationsDialog()
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    private fun showBatchAddDialog() {
        startActivity(Intent(this, RentDeviceActivity::class.java))
    }
    
    private fun showBatchRenewDialog() {
        val selectedDevices = deviceList.filter { it.isSelected }
        val message = "您选择了${selectedDevices.size}台设备进行续费"
        
        AlertDialog.Builder(this)
            .setTitle("批量续费")
            .setMessage(message)
            .setPositiveButton("确定") { _, _ ->
                Toast.makeText(this, "续费成功", Toast.LENGTH_SHORT).show()
                exitBatchMode()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showGroupManagementDialog() {
        // 分组管理对话框
        Toast.makeText(this, "分组管理功能即将上线", Toast.LENGTH_SHORT).show()
    }
    
    private fun performBatchRestart() {
        val selectedDevices = deviceList.filter { it.isSelected }
        Toast.makeText(this, "正在重启${selectedDevices.size}台设备", Toast.LENGTH_SHORT).show()
        exitBatchMode()
    }
    
    private fun performBatchRestore() {
        val selectedDevices = deviceList.filter { it.isSelected }
        Toast.makeText(this, "正在恢复${selectedDevices.size}台设备", Toast.LENGTH_SHORT).show()
        exitBatchMode()
    }
    
    private fun showChangeDeviceDialog() {
        Toast.makeText(this, "更换设备功能即将上线", Toast.LENGTH_SHORT).show()
    }
    
    private fun showTransferDialog() {
        Toast.makeText(this, "转移设备功能即将上线", Toast.LENGTH_SHORT).show()
    }
    
    private fun showUploadAppDialog() {
        Toast.makeText(this, "上传应用功能即将上线", Toast.LENGTH_SHORT).show()
    }
    
    private fun showCloneDialog() {
        Toast.makeText(this, "复制设备功能即将上线", Toast.LENGTH_SHORT).show()
    }
    
    private fun showOtherOperationsDialog() {
        Toast.makeText(this, "更多操作功能即将上线", Toast.LENGTH_SHORT).show()
    }
    
    private fun showGroupManagement() {
        Toast.makeText(this, "分组管理功能即将上线", Toast.LENGTH_SHORT).show()
    }
    
    private fun changeViewMode(mode: Int) {
        currentViewMode = mode
        deviceAdapter.setViewMode(mode)
        
        if (mode == VIEW_MODE_LIST) {
            recyclerView.layoutManager = LinearLayoutManager(this)
        } else {
            (recyclerView.layoutManager as? GridLayoutManager)?.spanCount = getColumnCount()
                ?: run { recyclerView.layoutManager = GridLayoutManager(this, getColumnCount()) }
        }
        
        recyclerView.adapter?.notifyDataSetChanged()
    }
    
    private fun changeGroup(group: String) {
        currentGroup = group
        loadDevices()
        
        // 更新标题
        val title = when (group) {
            GROUP_EXPIRING -> "到期设备"
            GROUP_MAINTENANCE -> "维护设备"
            else -> "全部设备"
        }
        titleTextView.text = title
    }
    
    private fun toggleBatchMode() {
        isInBatchMode = !isInBatchMode
        
        if (isInBatchMode) {
            // 进入批量模式
            batchOperationButton.visibility = View.VISIBLE
            batchOperationButton.text = "批量操作 (0)"
            
            // 清除所有选择
            deviceList.forEach { it.isSelected = false }
            
        } else {
            // 退出批量模式
            batchOperationButton.visibility = View.GONE
            
            // 清除所有选择
            deviceList.forEach { it.isSelected = false }
        }
        
        deviceAdapter.setBatchMode(isInBatchMode)
        deviceAdapter.notifyDataSetChanged()
    }
    
    private fun exitBatchMode() {
        if (isInBatchMode) {
            toggleBatchMode()
        }
    }
    
    private fun updateBatchOperationButton() {
        val selectedCount = deviceList.count { it.isSelected }
        batchOperationButton.text = "批量操作 ($selectedCount)"
    }
    
    data class DeviceInfo(
        val name: String,
        val id: String,
        val status: String, // online, offline, maintenance
        val daysLeft: Int,   // 剩余天数
        var isSelected: Boolean = false,
        var group: String = "默认分组",
        var model: String = "MEIZU 17"
    )
} 