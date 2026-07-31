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
 * 唯一示例 Activity
 *
 * 演示 [Framework] 提供的核心能力：
 * - DataStore 读写（IP / 端口）
 * - Repository 调用 ApiService 发起请求
 * - Token 自动注入（TokenInterceptor）
 * - 401 自动处理（AuthErrorInterceptor）
 * - WebSocket 连接与断开
 *
 * 运行前请在下方输入 IP 与端口，点击「保存设置」后点击「调用 Login」。
 *
 * @author Shiwei Wang
 * @date 2026-02
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
                toast("请填写 IP 与端口")
                return@setOnClickListener
            }
            lifecycleScope.launch {
                Framework.getPreferences().saveServerIp(ip)
                Framework.getPreferences().saveServerPort(port)
                Framework.getRepository().clearApiServiceCache()
                toast("已保存：$ip:$port")
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
                        toast("登录成功：code=${response.code}")
                    } else {
                        toast("登录失败：${response.message} (code=${response.code})")
                    }
                    binding.tvStatus.text = "Server: $ip:$port\nLogin result: ${response.code} ${response.message}"
                } catch (e: Exception) {
                    toast("请求异常：${e.message}")
                    binding.tvStatus.text = "Exception: ${e.message}"
                }
            }
        }

        binding.btnWsConnect.setOnClickListener {
            lifecycleScope.launch {
                val repo = Framework.getRepository()
                repo.connectWebSocket()
                toast("WebSocket 已连接")
            }
        }

        binding.btnWsDisconnect.setOnClickListener {
            Framework.getRepository().disconnectWebSocket()
            toast("WebSocket 已断开")
        }
    }

    override fun initData() {
        binding.tvTitle.text = "Android 框架模板 - Demo"
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
