package com.example

import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.QinglongConfig
import com.example.ui.theme.*
import com.example.viewmodel.LogItem
import com.example.viewmodel.QinglongViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFF7F9FC)
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: QinglongViewModel = viewModel()
) {
    val currentCookie by viewModel.currentCookie.collectAsState()
    val ptPin by viewModel.ptPin.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isWebViewLoading by viewModel.isWebViewLoading.collectAsState()
    val webViewProgress by viewModel.webViewProgress.collectAsState()
    val configs by viewModel.configs.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var showConfigListDialog by remember { mutableStateOf(false) }

    // Start auto cookie Extraction Poller loop
    LaunchedEffect(Unit) {
        viewModel.addLog("青龙京东助手已启动，正在导入配置...", isError = false)
        while (true) {
            viewModel.checkAndExtractCookies()
            delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
    ) {
        // App Custom Bar Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Circular Lightning Logo Box using Faces or Star
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Blue600, shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "App Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "青龙京东助手",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            letterSpacing = (-0.25).sp
                        )
                    )
                }

                // Quick Status & Config Gear Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Net state Badge
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (ptPin.isNotEmpty()) Green50 else Slate100,
                                shape = RoundedCornerShape(50)
                            )
                            .border(
                                width = 1.dp,
                                color = if (ptPin.isNotEmpty()) Green100 else Slate200,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (ptPin.isNotEmpty()) Green600 else Slate400,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (ptPin.isNotEmpty()) "已就绪" else "未登录",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (ptPin.isNotEmpty()) Green600 else Slate600
                            )
                        )
                    }

                    // Settings IconButton
                    IconButton(
                        onClick = { showConfigListDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .testTag("toolbar_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Qinglong Configs",
                            tint = Slate600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = Slate200, thickness = 1.dp)
        }

        // Active web loading bar
        if (isWebViewLoading) {
            LinearProgressIndicator(
                progress = { webViewProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Blue600,
                trackColor = Blue50
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }

        // WebView displaying container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .border(1.dp, Slate200, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Web address subbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Address",
                        tint = Slate400,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "https://plogin.m.jd.com/login/login",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Slate500
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(color = Slate100, thickness = 1.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    @Suppress("DEPRECATION")
                                    databaseEnabled = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    builtInZoomControls = false
                                    displayZoomControls = false
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        viewModel.setWebViewLoading(true)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        viewModel.setWebViewLoading(false)
                                        viewModel.checkAndExtractCookies()
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        viewModel.setWebViewProgress(newProgress)
                                    }
                                }

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                loadUrl("https://plogin.m.jd.com/login/login")
                                webViewInstance = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Interaction Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            HorizontalDivider(color = Slate200, thickness = 1.dp)
            
            Column(modifier = Modifier.padding(16.dp)) {
                // Label Header
                Text(
                    text = "EXTRACTED JD COOKIE (PT_KEY & PT_PIN)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Extracted Cookie Container Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50, shape = RoundedCornerShape(10.dp))
                        .border(1.dp, Slate200, shape = RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val clipboardManager = LocalClipboardManager.current
                    val isCookieAvailable = currentCookie.isNotEmpty()

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isCookieAvailable) currentCookie else "未检测到 Cookie，请在上方页面登录成功后自动获取",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (isCookieAvailable) Slate700 else Slate400,
                                lineHeight = 16.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (isCookieAvailable) {
                                clipboardManager.setText(AnnotatedString(currentCookie))
                                viewModel.addLog("Cookie 已成功复制至剪贴板", isError = false)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (isCookieAvailable) Blue50 else Color.Transparent, shape = RoundedCornerShape(8.dp))
                            .testTag("action_copy_cookies"),
                        enabled = isCookieAvailable
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Copy cookie to clipboard",
                            tint = if (isCookieAvailable) Blue600 else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Interactive Actions Grid: exactly 4 balanced columns (1 row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. 获取 (Extract Manual)
                    MinimalActionButton(
                        icon = Icons.Default.Refresh,
                        label = "获取",
                        backgroundColor = Blue50,
                        contentColor = Blue600,
                        testTag = "action_get_cookies",
                        onClick = { viewModel.forceExtractCookiesManual() },
                        modifier = Modifier.weight(1f)
                    )

                    // 2. 重新登录 (Reset / Clear Cache)
                    MinimalActionButton(
                        icon = Icons.Default.Clear,
                        label = "重新登录",
                        backgroundColor = Red50,
                        contentColor = Red600,
                        testTag = "action_relogin",
                        onClick = {
                            webViewInstance?.let { wb ->
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.removeAllCookies {
                                    cookieManager.flush()
                                }
                                wb.clearCache(true)
                                wb.clearFormData()
                                wb.clearHistory()
                                wb.loadUrl("https://plogin.m.jd.com/login/login")
                                viewModel.updateCookie("")
                                viewModel.addLog("已清空浏览器 Cookie、缓存，重新载入登录页", isError = false)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 3. 发送 (Send to all panel instances)
                    MinimalActionButton(
                        icon = Icons.AutoMirrored.Filled.Send,
                        label = "发送",
                        backgroundColor = Green50,
                        contentColor = Green600,
                        testTag = "action_send",
                        onClick = { viewModel.sendToAllQinglong() },
                        modifier = Modifier.weight(1f)
                    )

                    // 4. 配置 (Open dialog)
                    MinimalActionButton(
                        icon = Icons.Default.Settings,
                        label = "配置",
                        backgroundColor = Slate100,
                        contentColor = Slate600,
                        testTag = "action_config",
                        onClick = { showConfigListDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real Console Logs Console Display Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(Slate900, shape = RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONSOLE LOG",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "清除日志",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = Blue500,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.clearLogs() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (logs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Waiting for actions...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 2.dp),
                                reverseLayout = true
                            ) {
                                items(logs.reversed(), key = { it.id }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 1.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "[${item.timestamp}] ",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Slate400
                                        )
                                        Text(
                                            text = item.message,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            ),
                                            color = if (item.isError) Color(0xFFEF4444) else Color(0xFF60A5FA)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Config list Dialog Manager
    if (showConfigListDialog) {
        ConfigListDialog(
            configs = configs,
            onClose = { showConfigListDialog = false },
            onAdd = { viewModel.addConfig(it) },
            onUpdate = { viewModel.updateConfig(it) },
            onDelete = { viewModel.deleteConfig(it) },
            onTest = { config, cb -> viewModel.testConfigConnection(config, cb) },
            viewModel = viewModel
        )
    }
}

// Minimal action button style block helper
@Composable
fun MinimalActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(backgroundColor, shape = CircleShape)
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Slate700
            )
        )
    }
}

// ConfigListDialog
@Composable
fun ConfigListDialog(
    configs: List<QinglongConfig>,
    onClose: () -> Unit,
    onAdd: (QinglongConfig) -> Unit,
    onUpdate: (QinglongConfig) -> Unit,
    onDelete: (QinglongConfig) -> Unit,
    onTest: (QinglongConfig, (Boolean, String) -> Unit) -> Unit,
    viewModel: QinglongViewModel
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var activeConfigToEdit by remember { mutableStateOf<QinglongConfig?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header of panel configurations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "青龙面板配置管理",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Slate100, shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Config cards scrolls
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (configs.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No configs",
                                modifier = Modifier.size(48.dp),
                                tint = Slate400
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "暂无配置的青龙面板",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Slate600
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "请点击下方的按钮新增一个面板配置",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(configs) { config ->
                                ConfigCard(
                                    config = config,
                                    onEdit = {
                                        activeConfigToEdit = config
                                        showEditDialog = true
                                    },
                                    onDelete = { onDelete(config) },
                                    onTest = { cb -> onTest(config, cb) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons inside Configuration List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            activeConfigToEdit = null
                            showEditDialog = true
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Config", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("新增配置", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate600),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Text("返回主页")
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        ConfigEditDialog(
            config = activeConfigToEdit,
            onDismiss = { showEditDialog = false },
            onSave = { saved ->
                if (saved.id == 0) {
                    onAdd(saved)
                } else {
                    onUpdate(saved)
                }
                showEditDialog = false
            },
            onTest = onTest
        )
    }
}

// ConfigCard list item component
@Composable
fun ConfigCard(
    config: QinglongConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: ((Boolean, String) -> Unit) -> Unit
) {
    var testingResultStr by remember { mutableStateOf("") }
    var testingResultIsError by remember { mutableStateOf(false) }
    var isTestingInProgress by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Config",
                            tint = Blue600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Config",
                            tint = Red600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // URL Details
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "地址: ${config.url}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Slate600)
                )
                Text(
                    text = "Client ID: ${config.clientId}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Slate500)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Testing footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        isTestingInProgress = true
                        testingResultStr = "正在连接并鉴权..."
                        onTest { success, responseMsg ->
                            isTestingInProgress = false
                            testingResultIsError = !success
                            testingResultStr = responseMsg
                        }
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate600),
                    enabled = !isTestingInProgress
                ) {
                    Text(
                        text = if (isTestingInProgress) "测试中..." else "测试连接",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (testingResultStr.isNotEmpty()) {
                    Text(
                        text = testingResultStr,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (testingResultIsError) Red600 else Green600,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Config edit Dialog
@Composable
fun ConfigEditDialog(
    config: QinglongConfig?,
    onDismiss: () -> Unit,
    onSave: (QinglongConfig) -> Unit,
    onTest: (QinglongConfig, (Boolean, String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(config?.name ?: "") }
    var url by remember { mutableStateOf(config?.url ?: "") }
    var clientId by remember { mutableStateOf(config?.clientId ?: "") }
    var clientSecret by remember { mutableStateOf(config?.clientSecret ?: "") }

    var isSecretVisible by remember { mutableStateOf(false) }
    var isTestingInProgress by remember { mutableStateOf(false) }
    var testFeedbackStr by remember { mutableStateOf("") }
    var testFeedbackIsError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (config == null) "添加青龙面板" else "编辑青龙面板",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Style OutlinedTextField with Slate outlines
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("配置名称 (如: 极速云面板)", color = Slate500) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue600,
                        unfocusedBorderColor = Slate200,
                        focusedLabelColor = Blue600,
                        unfocusedLabelColor = Slate500
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("青龙地址 (如: http://192.168.1.100:5700)", color = Slate500) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue600,
                        unfocusedBorderColor = Slate200,
                        focusedLabelColor = Blue600,
                        unfocusedLabelColor = Slate500
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("Client ID", color = Slate500) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue600,
                        unfocusedBorderColor = Slate200,
                        focusedLabelColor = Blue600,
                        unfocusedLabelColor = Slate500
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = clientSecret,
                    onValueChange = { clientSecret = it },
                    label = { Text("Client Secret", color = Slate500) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Blue600,
                        unfocusedBorderColor = Slate200,
                        focusedLabelColor = Blue600,
                        unfocusedLabelColor = Slate500
                    ),
                    singleLine = true,
                    visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                            Icon(
                                imageVector = if (isSecretVisible) Icons.Default.Info else Icons.Default.Lock,
                                contentDescription = if (isSecretVisible) "Hide Secret" else "Show Secret",
                                tint = Slate500
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Connection feedback
                if (testFeedbackStr.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (testFeedbackIsError) Red50 else Green50
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (testFeedbackIsError) Red100 else Green100)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (testFeedbackIsError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = "Testing Feedback Status Icon",
                                tint = if (testFeedbackIsError) Red600 else Green600,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = testFeedbackStr,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (testFeedbackIsError) Red600 else Green600
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (name.isBlank() || url.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
                                testFeedbackIsError = true
                                testFeedbackStr = "请完成所有必填字段。"
                                return@Button
                            }
                            isTestingInProgress = true
                            testFeedbackStr = "正在连接并鉴权..."
                            val tempConfig = QinglongConfig(
                                id = config?.id ?: 0,
                                name = name,
                                url = url,
                                clientId = clientId,
                                clientSecret = clientSecret
                            )
                            onTest(tempConfig) { success, msg ->
                                isTestingInProgress = false
                                testFeedbackIsError = !success
                                testFeedbackStr = msg
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate600),
                        enabled = !isTestingInProgress
                    ) {
                        Text(
                            text = if (isTestingInProgress) "测试中..." else "测试连接",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = {
                            if (name.isBlank() || url.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
                                testFeedbackIsError = true
                                testFeedbackStr = "所有字段都必须填写！"
                                return@Button
                            }
                            onSave(
                                QinglongConfig(
                                    id = config?.id ?: 0,
                                    name = name,
                                    url = url,
                                    clientId = clientId,
                                    clientSecret = clientSecret
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                    ) {
                        Text("保存", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate600),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Text("取消")
                }
            }
        }
    }
}
