package com.screencast.noadb

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private var devices: MutableList<DeviceListActivity.DeviceInfo>,
    private var viewMode: Int = DeviceListActivity.VIEW_MODE_GRID_4,
    private val onDeviceClick: (DeviceListActivity.DeviceInfo, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isBatchMode = false
    
    companion object {
        private const val VIEW_TYPE_GRID = 0
        private const val VIEW_TYPE_LIST = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (viewMode == DeviceListActivity.VIEW_MODE_LIST) VIEW_TYPE_LIST else VIEW_TYPE_GRID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LIST -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item_list, parent, false)
                DeviceListViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
                DeviceGridViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val device = devices[position]
        
        when (holder) {
            is DeviceGridViewHolder -> bindGridViewHolder(holder, device, position)
            is DeviceListViewHolder -> bindListViewHolder(holder, device, position)
        }
    }
    
    private fun bindGridViewHolder(holder: DeviceGridViewHolder, device: DeviceListActivity.DeviceInfo, position: Int) {
        holder.deviceName.text = device.name
        
        // 设置剩余天数
        holder.deviceDaysLeft.text = "剩${device.daysLeft}天${if (position == 0) "20小时" else "时"}"
        
        // 根据状态设置不同的背景色和缩略图
        when (device.status) {
            "online" -> {
                holder.deviceStatus.text = "在线"
                holder.deviceStatus.setTextColor(0xFF009900.toInt())
                holder.deviceThumbnail.setBackgroundResource(R.drawable.bg_device_online)
                // 在实际应用中，这里应该加载真实的设备截图
            }
            "offline" -> {
                holder.deviceStatus.text = "离线"
                holder.deviceStatus.setTextColor(0xFF999999.toInt())
                holder.deviceThumbnail.setBackgroundResource(R.drawable.bg_device_offline)
            }
            "maintenance" -> {
                holder.deviceStatus.text = "维护中"
                holder.deviceStatus.setTextColor(0xFFFF6600.toInt())
                holder.deviceThumbnail.setBackgroundResource(R.drawable.bg_device_error)
            }
            else -> {
                holder.deviceStatus.text = "错误"
                holder.deviceStatus.setTextColor(0xFFFF0000.toInt())
                holder.deviceThumbnail.setBackgroundResource(R.drawable.bg_device_error)
            }
        }
        
        // 显示或隐藏选择框
        if (isBatchMode) {
            holder.deviceCheckbox.visibility = View.VISIBLE
            holder.deviceCheckbox.isChecked = device.isSelected
        } else {
            holder.deviceCheckbox.visibility = View.GONE
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener {
            onDeviceClick(device, false)
        }
        
        holder.itemView.setOnLongClickListener {
            onDeviceClick(device, true)
            true
        }
        
        // 设置设置按钮点击事件
        holder.deviceSettings.setOnClickListener {
            // 打开设备信息页面
            val intent = Intent(holder.itemView.context, DeviceInfoActivity::class.java).apply {
                putExtra("DEVICE_ID", device.id)
                putExtra("DEVICE_NAME", device.name)
            }
            holder.itemView.context.startActivity(intent)
        }
        
        // 设置选择框点击事件
        holder.deviceCheckbox.setOnClickListener {
            device.isSelected = holder.deviceCheckbox.isChecked
            notifyItemChanged(position)
        }
    }
    
    private fun bindListViewHolder(holder: DeviceListViewHolder, device: DeviceListActivity.DeviceInfo, position: Int) {
        holder.deviceName.text = device.name
        holder.deviceDaysLeft.text = "剩余${device.daysLeft}天"
        holder.deviceGroup.text = device.group
        
        // 根据状态设置不同的状态文本和颜色
        when (device.status) {
            "online" -> {
                holder.deviceStatus.text = "在线"
                holder.deviceStatus.setTextColor(0xFF009900.toInt())
                holder.deviceStatusIcon.setImageResource(android.R.drawable.presence_online)
            }
            "offline" -> {
                holder.deviceStatus.text = "离线"
                holder.deviceStatus.setTextColor(0xFF999999.toInt())
                holder.deviceStatusIcon.setImageResource(android.R.drawable.presence_offline)
            }
            "maintenance" -> {
                holder.deviceStatus.text = "维护中"
                holder.deviceStatus.setTextColor(0xFFFF6600.toInt())
                holder.deviceStatusIcon.setImageResource(android.R.drawable.presence_busy)
            }
            else -> {
                holder.deviceStatus.text = "错误"
                holder.deviceStatus.setTextColor(0xFFFF0000.toInt())
                holder.deviceStatusIcon.setImageResource(android.R.drawable.presence_busy)
            }
        }
        
        // 显示或隐藏选择框
        if (isBatchMode) {
            holder.deviceCheckbox.visibility = View.VISIBLE
            holder.deviceCheckbox.isChecked = device.isSelected
        } else {
            holder.deviceCheckbox.visibility = View.GONE
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener {
            if (isBatchMode) {
                device.isSelected = !device.isSelected
                notifyItemChanged(position)
            } else {
                onDeviceClick(device, false)
            }
        }
        
        holder.itemView.setOnLongClickListener {
            // 打开设备信息页面
            val intent = Intent(holder.itemView.context, DeviceInfoActivity::class.java).apply {
                putExtra("DEVICE_ID", device.id)
                putExtra("DEVICE_NAME", device.name)
            }
            holder.itemView.context.startActivity(intent)
            true
        }
        
        // 设置选择框点击事件
        holder.deviceCheckbox.setOnClickListener {
            device.isSelected = holder.deviceCheckbox.isChecked
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: MutableList<DeviceListActivity.DeviceInfo>) {
        devices = newDevices
        notifyDataSetChanged()
    }
    
    fun setViewMode(mode: Int) {
        viewMode = mode
    }
    
    fun setBatchMode(batchMode: Boolean) {
        isBatchMode = batchMode
    }

    class DeviceGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.device_name)
        val deviceStatus: TextView = itemView.findViewById(R.id.device_status)
        val deviceDaysLeft: TextView = itemView.findViewById(R.id.device_days_left)
        val deviceSettings: ImageView = itemView.findViewById(R.id.device_settings)
        val deviceThumbnail: ImageView = itemView.findViewById(R.id.device_thumbnail)
        val deviceCheckbox: CheckBox = itemView.findViewById(R.id.device_checkbox)
    }
    
    class DeviceListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.device_name)
        val deviceStatus: TextView = itemView.findViewById(R.id.device_status)
        val deviceStatusIcon: ImageView = itemView.findViewById(R.id.device_status_icon)
        val deviceDaysLeft: TextView = itemView.findViewById(R.id.device_days_left)
        val deviceGroup: TextView = itemView.findViewById(R.id.device_group)
        val deviceCheckbox: CheckBox = itemView.findViewById(R.id.device_checkbox)
    }
} 