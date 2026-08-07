package com.example.template

import android.os.Bundle
import android.widget.Toast
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.template.databinding.ActivityMainBinding
import com.template.framework.Framework
import com.template.framework.api.model.auth.DeviceLoginRequest
import com.template.framework.ui.base.BaseActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Runnable screen demonstrating the framework's main integration path.
 *
 * The screen persists a server address, calls the sample login API, stores its token, and controls
 * the shared WebSocket connection. The HTTP and WebSocket actions require a compatible backend.
 *
 * - 中文：演示服务器配置、登录请求、Token 保存以及 WebSocket 连接/断开。
 */
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
    }

    override fun initViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        // 进入时读取已保存的 IP / 端口
        lifecycleScope.launch {
            val ip = Framework.getPreferences().serverIp.first()
            val port = Framework.getPreferences().serverPort.first()
            binding.etIp.setText(ip)
            binding.etPort.setText(port)
        }
    }

    override fun initListener() {
        binding.btnSave.setOnClickListener {
            val ip = binding.etIp.text.toString().trim()
            val port = binding.etPort.text.toString().trim()
            if (ip.isEmpty() || port.isEmpty()) {
                toast(getString(R.string.server_required))
                return@setOnClickListener
            }
            lifecycleScope.launch {
                Framework.getPreferences().saveServerIp(ip)
                Framework.getPreferences().saveServerPort(port)
                Framework.getRepository().clearApiServiceCache()
                toast(getString(R.string.server_saved, ip, port))
            }
        }

        binding.btnLogin.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val repo = Framework.getRepository()
                    val ip = repo.serverIp.first()
                    val port = repo.serverPort.first()
                    val response = repo.login(
                        DeviceLoginRequest(
                            grantType = "device",
                            userId = "demo_user",
                            snNumber = "SN_DEMO_001"
                        )
                    )
                    if (response.isSuccess) {
                        // 保存 token
                        response.data?.accessToken?.let { token ->
                            Framework.getPreferences().saveAccessToken(token)
                        }
                        toast(getString(R.string.login_success, response.code))
                    } else {
                        toast(getString(R.string.login_failed, response.message, response.code))
                    }
                    binding.tvStatus.text = getString(
                        R.string.login_status,
                        ip,
                        port,
                        response.code,
                        response.message
                    )
                } catch (e: Exception) {
                    toast(getString(R.string.request_failed, e.message))
                    binding.tvStatus.text = getString(R.string.exception_status, e.message)
                }
            }
        }

        binding.btnWsConnect.setOnClickListener {
            lifecycleScope.launch {
                val repo = Framework.getRepository()
                repo.connectWebSocket()
                toast(getString(R.string.websocket_connected))
            }
        }

        binding.btnWsDisconnect.setOnClickListener {
            Framework.getRepository().disconnectWebSocket()
            toast(getString(R.string.websocket_disconnected))
        }
    }

    override fun initData() {
        binding.tvTitle.setText(R.string.demo_title)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
