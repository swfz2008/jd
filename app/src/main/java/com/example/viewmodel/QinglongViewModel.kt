package com.example.viewmodel

import android.app.Application
import android.util.Log
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.QinglongConfig
import com.example.data.QinglongConfigRepository
import com.example.network.QinglongApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogItem(
    val id: Long = System.nanoTime(),
    val timestamp: String,
    val message: String,
    val isError: Boolean
)

class QinglongViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: QinglongConfigRepository
    
    val configs: StateFlow<List<QinglongConfig>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = QinglongConfigRepository(database.qinglongConfigDao())
        
        // Automatically pre-populate default configurations if they don't exist
        viewModelScope.launch(Dispatchers.IO) {
            val existingConfigs = repository.getAllConfigsList()
            if (existingConfigs.isEmpty()) {
                repository.insert(
                    QinglongConfig(
                        name = "252",
                        url = "http://4995.408008.xyz:5700/",
                        clientId = "ExXyDn-gAD8z",
                        clientSecret = "_mhdMfLtVjv3IsyLut9gmerC"
                    )
                )
                repository.insert(
                    QinglongConfig(
                        name = "45",
                        url = "http://4995.408008.xyz:5701/",
                        clientId = "-z-Lkhlu65P_",
                        clientSecret = "_dMrXo6IJ6Ivr1oYa_d8w_gp5"
                    )
                )
            } else {
                // If configurations already exist, migrate the old URLs to the new ones
                for (config in existingConfigs) {
                    if (config.name == "252" && config.url == "http://192.168.2.252:15700/") {
                        repository.update(config.copy(url = "http://4995.408008.xyz:5700/"))
                    } else if (config.name == "45" && config.url == "http://192.168.2.45:5700/") {
                        repository.update(config.copy(url = "http://4995.408008.xyz:5701/"))
                    }
                }
            }
        }

        configs = repository.allConfigs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private val _currentCookie = MutableStateFlow("")
    val currentCookie: StateFlow<String> = _currentCookie.asStateFlow()

    private val _ptPin = MutableStateFlow("")
    val ptPin: StateFlow<String> = _ptPin.asStateFlow()

    private val _logs = MutableStateFlow<List<LogItem>>(emptyList())
    val logs: StateFlow<List<LogItem>> = _logs.asStateFlow()

    private val _isWebViewLoading = MutableStateFlow(false)
    val isWebViewLoading: StateFlow<Boolean> = _isWebViewLoading.asStateFlow()

    private val _webViewProgress = MutableStateFlow(0f)
    val webViewProgress: StateFlow<Float> = _webViewProgress.asStateFlow()

    fun setWebViewLoading(loading: Boolean) {
        _isWebViewLoading.value = loading
    }

    fun setWebViewProgress(progress: Int) {
        _webViewProgress.value = progress / 100f
    }

    fun updateCookie(newValue: String) {
        _currentCookie.value = newValue
        val parts = newValue.split(";").map { it.trim() }
        for (part in parts) {
            if (part.startsWith("pt_pin=")) {
                val p = part.substringAfter("pt_pin=").trim()
                if (p.isNotEmpty() && p != "null") {
                    _ptPin.value = p
                }
            }
        }
    }

    fun addLog(msg: String, isError: Boolean = false) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = sdf.format(Date())
        viewModelScope.launch(Dispatchers.Main) {
            val newLog = LogItem(timestamp = timeStr, message = msg, isError = isError)
            _logs.value = listOf(newLog) + _logs.value
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun checkAndExtractCookies() {
        try {
            val domains = listOf(
                "https://plogin.m.jd.com",
                "https://m.jd.com",
                "https://jd.com",
                "https://home.m.jd.com"
            )
            val cookieManager = CookieManager.getInstance()
            var foundKey: String? = null
            var foundPin: String? = null
            
            for (domain in domains) {
                val cookies = cookieManager.getCookie(domain) ?: continue
                val parts = cookies.split(";").map { it.trim() }
                for (part in parts) {
                    val keyPair = part.split("=", limit = 2)
                    if (keyPair.size == 2) {
                        val keyName = keyPair[0].trim()
                        val keyValue = keyPair[1].trim()
                        if (keyName == "pt_key" && keyValue.isNotEmpty() && keyValue != "null") {
                            foundKey = keyValue
                        }
                        if (keyName == "pt_pin" && keyValue.isNotEmpty() && keyValue != "null") {
                            foundPin = keyValue
                        }
                    }
                }
                if (foundKey != null && foundPin != null) {
                    break
                }
            }
            
            if (foundKey != null && foundPin != null) {
                val formatted = "pt_key=$foundKey;pt_pin=$foundPin;"
                if (_currentCookie.value != formatted) {
                    _currentCookie.value = formatted
                    _ptPin.value = foundPin
                    addLog("成功自动捕获 Cookie: pt_pin=$foundPin", isError = false)
                }
            }
        } catch (e: Exception) {
            Log.e("QinglongViewModel", "checkAndExtractCookies failed: ${e.message}")
        }
    }

    fun forceExtractCookiesManual() {
        try {
            val domains = listOf(
                "https://plogin.m.jd.com",
                "https://m.jd.com",
                "https://jd.com",
                "https://home.m.jd.com"
            )
            val cookieManager = CookieManager.getInstance()
            var foundKey: String? = null
            var foundPin: String? = null
            
            for (domain in domains) {
                val cookies = cookieManager.getCookie(domain) ?: continue
                val parts = cookies.split(";").map { it.trim() }
                for (part in parts) {
                    val keyPair = part.split("=", limit = 2)
                    if (keyPair.size == 2) {
                        val keyName = keyPair[0].trim()
                        val keyValue = keyPair[1].trim()
                        if (keyName == "pt_key" && keyValue.isNotEmpty() && keyValue != "null") {
                            foundKey = keyValue
                        }
                        if (keyName == "pt_pin" && keyValue.isNotEmpty() && keyValue != "null") {
                            foundPin = keyValue
                        }
                    }
                }
                if (foundKey != null && foundPin != null) {
                    break
                }
            }
            
            if (foundKey != null && foundPin != null) {
                val formatted = "pt_key=$foundKey;pt_pin=$foundPin;"
                _currentCookie.value = formatted
                _ptPin.value = foundPin
                addLog("获取 Cookies 成功: pt_pin=$foundPin", isError = false)
            } else {
                addLog("获取 Cookies 失败: 浏览器中未发现完整的 pt_key 和 pt_pin", isError = true)
            }
        } catch (e: Exception) {
            addLog("手动提取 Cookie 失败: ${e.message}", isError = true)
            Log.e("QinglongViewModel", "forceExtractCookiesManual failed: ${e.message}")
        }
    }

    fun sendToAllQinglong() {
        val cookie = _currentCookie.value
        val pin = _ptPin.value
        
        if (cookie.isBlank() || pin.isBlank()) {
            addLog("未获取到 Cookie", isError = true)
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            val configList = repository.getAllConfigsList()
            if (configList.isEmpty()) {
                addLog("青龙面板配置不完整", isError = true)
                return@launch
            }
            
            withContext(Dispatchers.Main) {
                addLog("开始向 ${configList.size} 个青龙面板同步 Cookie...", isError = false)
            }
            
            var successCount = 0
            var failCount = 0
            
            for (config in configList) {
                withContext(Dispatchers.Main) {
                    addLog("连接 [${config.name}]中...", isError = false)
                }
                val apiService = QinglongApiService()
                val tokenResult = apiService.getToken(config.url, config.clientId, config.clientSecret)
                if (tokenResult.isFailure) {
                    withContext(Dispatchers.Main) {
                        addLog("[${config.name}] 登录授权失败: ${tokenResult.exceptionOrNull()?.message}", isError = true)
                    }
                    failCount++
                    continue
                }
                
                val (tokenType, token) = tokenResult.getOrThrow()
                val sendResult = apiService.sendCookie(config.url, tokenType, token, pin, cookie)
                if (sendResult.isSuccess) {
                    withContext(Dispatchers.Main) {
                        addLog("[${config.name}] 发送成功", isError = false)
                    }
                    successCount++
                } else {
                    withContext(Dispatchers.Main) {
                        addLog("[${config.name}] 同步失败: ${sendResult.exceptionOrNull()?.message}", isError = true)
                    }
                    failCount++
                }
            }
            
            withContext(Dispatchers.Main) {
                addLog("同步任务完成。成功 $successCount 个，失败 $failCount 个", isError = failCount > 0)
            }
        }
    }

    fun addConfig(config: QinglongConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(config)
            withContext(Dispatchers.Main) {
                addLog("已保存配置: ${config.name}", isError = false)
            }
        }
    }

    fun updateConfig(config: QinglongConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(config)
            withContext(Dispatchers.Main) {
                addLog("已更新配置: ${config.name}", isError = false)
            }
        }
    }

    fun deleteConfig(config: QinglongConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(config)
            withContext(Dispatchers.Main) {
                addLog("已删除配置: ${config.name}", isError = false)
            }
        }
    }

    fun testConfigConnection(config: QinglongConfig, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val apiService = QinglongApiService()
            val result = apiService.testConnection(config.url, config.clientId, config.clientSecret)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    onComplete(true, result.getOrThrow())
                } else {
                    onComplete(false, result.exceptionOrNull()?.message ?: "未知错误")
                }
            }
        }
    }
}
