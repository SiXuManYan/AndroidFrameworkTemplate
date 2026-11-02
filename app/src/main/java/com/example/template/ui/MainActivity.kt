package com.example.template.ui

import android.os.Bundle
import android.widget.Toast
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.template.BuildConfig
import com.example.template.R
import com.example.template.data.api.ExampleApi
import com.example.template.data.api.ExampleItem
import com.example.template.databinding.ActivityMainBinding
import com.template.framework.Framework
import com.template.framework.api.NetworkModule
import com.template.framework.network.HttpConfig
import com.template.framework.ui.base.BaseActivity
import com.template.framework.websocket.WebSocketConfig
import com.template.framework.websocket.WebSocketManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 示例默认只展示本地列表；HTTP 与 WebSocket 各自在按钮操作时初始化。 */
class MainActivity : BaseActivity<ActivityMainBinding>() {
    private val itemAdapter = ExampleItemAdapter()
    private var webSocket: WebSocketManager? = null
    private var socketObserver: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }

    override fun initViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        binding.tvTitle.setText(R.string.demo_title)
        binding.exampleList.layoutManager = LinearLayoutManager(this)
        binding.exampleList.adapter = itemAdapter
        itemAdapter.submitList((1L..3L).map { ExampleItem(it, getString(R.string.example_item, it)) })
        itemAdapter.onItemClick = { item, _, _ ->
            // 新对象与新列表触发差分，不原地修改已提交的条目。
            itemAdapter.submitList(itemAdapter.currentList.map {
                if (it.id == item.id) it.copy(title = getString(R.string.example_item_updated, item.id)) else it
            })
        }
    }

    override fun initData() {
        lifecycleScope.launch {
            val preferences = Framework.getPreferences()
            binding.etHttpUrl.setText(preferences.httpBaseUrl.first().ifBlank { getString(R.string.http_url_hint) })
            binding.etWebsocketUrl.setText(preferences.webSocketUrl.first().ifBlank { getString(R.string.websocket_url_hint) })
        }
    }

    override fun initListener() {
        binding.btnSave.setOnClickListener {
            val httpUrl = binding.etHttpUrl.text.toString().trim()
            val socketUrl = binding.etWebsocketUrl.text.toString().trim()
            lifecycleScope.launch {
                try {
                    val preferences = Framework.getPreferences()
                    preferences.saveHttpBaseUrl(httpUrl)
                    preferences.saveWebSocketUrl(socketUrl)
                    toast(getString(R.string.server_saved))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    toast(getString(R.string.request_failed, e.javaClass.simpleName))
                }
            }
        }

        binding.btnRequest.setOnClickListener {
            val url = binding.etHttpUrl.text.toString().trim()
            val requestButton = binding.btnRequest
            lifecycleScope.launch {
                requestButton.isEnabled = false
                try {
                    // 只初始化 HTTP；示例域名无真实后端，请替换后再请求。
                    Framework.initHttp(HttpConfig(baseUrl = url, debug = BuildConfig.DEBUG))
                    val api = NetworkModule.getRetrofit().create(ExampleApi::class.java)
                    val response = api.getItems()
                    binding.tvStatus.text = getString(R.string.http_status, response.code)
                    if (response.isSuccess) itemAdapter.submitList(response.data.orEmpty())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    binding.tvStatus.text = getString(R.string.request_failed, e.javaClass.simpleName)
                } finally {
                    requestButton.isEnabled = true
                }
            }
        }

        binding.btnWsConnect.setOnClickListener {
            try {
                val config = WebSocketConfig(url = binding.etWebsocketUrl.text.toString().trim())
                socketObserver?.cancel()
                webSocket = Framework.initWebSocket(config)
                val manager = checkNotNull(webSocket)
                socketObserver = lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        launch {
                            manager.connectionState.collect { state ->
                                val status = when (state) {
                                    WebSocketManager.ConnectionState.Disconnected -> getString(R.string.websocket_disconnected)
                                    WebSocketManager.ConnectionState.Connecting -> getString(R.string.websocket_connecting)
                                    WebSocketManager.ConnectionState.Connected -> getString(R.string.websocket_connected)
                                    is WebSocketManager.ConnectionState.Error -> getString(R.string.request_failed, state.message)
                                }
                                binding.tvStatus.text = status
                            }
                        }
                        launch {
                            manager.messageFlow.collect { binding.tvStatus.text = getString(R.string.websocket_message, it) }
                        }
                    }
                }
                manager.connect()
            } catch (e: Exception) {
                binding.tvStatus.text = getString(R.string.request_failed, e.javaClass.simpleName)
            }
        }

        binding.btnWsDisconnect.setOnClickListener { webSocket?.disconnect() }
    }

    override fun onDestroy() {
        socketObserver?.cancel()
        webSocket?.release()
        binding.exampleList.adapter = null
        super.onDestroy()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
