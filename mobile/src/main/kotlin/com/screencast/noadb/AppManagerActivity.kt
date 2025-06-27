package com.screencast.noadb

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AppManagerActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var appAdapter: AppAdapter
    private var allApps = mutableListOf<AppInfo>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_manager)
        
        // 设置标题栏返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "应用管理"
        
        recyclerView = findViewById(R.id.app_list)
        searchView = findViewById(R.id.search_view)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // 加载应用列表
        loadInstalledApps()
        
        // 设置搜索功能
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                appAdapter.filter.filter(newText)
                return true
            }
        })
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun loadInstalledApps() {
        val packageManager = packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        allApps.clear()
        for (packageInfo in packages) {
            // 过滤掉系统应用
            if (packageInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                val appName = packageManager.getApplicationLabel(packageInfo).toString()
                val packageName = packageInfo.packageName
                val icon = packageManager.getApplicationIcon(packageInfo)
                
                allApps.add(AppInfo(appName, packageName, icon))
            }
        }
        
        // 按应用名称排序
        allApps.sortBy { it.appName }
        
        appAdapter = AppAdapter(allApps)
        recyclerView.adapter = appAdapter
    }
    
    // 应用信息数据类
    data class AppInfo(
        val appName: String,
        val packageName: String,
        val icon: Drawable
    )
    
    // 应用列表适配器
    inner class AppAdapter(private val apps: MutableList<AppInfo>) : 
            RecyclerView.Adapter<AppAdapter.ViewHolder>(), Filterable {
        
        private var filteredApps = apps
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.app_item, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = filteredApps[position]
            holder.appNameTextView.text = app.appName
            holder.packageNameTextView.text = app.packageName
            holder.iconImageView.setImageDrawable(app.icon)
            
            // 点击打开应用详情
            holder.itemView.setOnClickListener {
                showAppOptions(app)
            }
        }
        
        override fun getItemCount(): Int = filteredApps.size
        
        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString()?.toLowerCase(Locale.getDefault()) ?: ""
                    
                    val filteredList = if (query.isEmpty()) {
                        apps
                    } else {
                        apps.filter {
                            it.appName.toLowerCase(Locale.getDefault()).contains(query) ||
                            it.packageName.toLowerCase(Locale.getDefault()).contains(query)
                        }
                    }
                    
                    val results = FilterResults()
                    results.values = filteredList
                    return results
                }
                
                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    filteredApps = (results?.values as? List<AppInfo>)?.toMutableList() ?: mutableListOf()
                    notifyDataSetChanged()
                }
            }
        }
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val iconImageView: ImageView = itemView.findViewById(R.id.app_icon)
            val appNameTextView: TextView = itemView.findViewById(R.id.app_name)
            val packageNameTextView: TextView = itemView.findViewById(R.id.package_name)
        }
    }
    
    private fun showAppOptions(app: AppInfo) {
        val options = arrayOf("打开应用", "应用信息", "卸载应用")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchApp(app.packageName)
                    1 -> openAppInfo(app.packageName)
                    2 -> uninstallApp(app.packageName)
                }
            }
            .show()
    }
    
    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "无法启动应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "启动应用失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "打开应用信息失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "卸载应用失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 