package com.yellowbite.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yellowbite.app.data.Settings

@Composable
fun SettingsScreen(settings: Settings, loading: Boolean, configure: (Boolean, String) -> Unit) {
    var offline by rememberSaveable { mutableStateOf(settings.offline) }
    var endpoint by rememberSaveable { mutableStateOf(settings.endpoint) }
    LaunchedEffect(settings.offline, settings.endpoint) { offline = settings.offline; endpoint = settings.endpoint }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()) {
        Column(Modifier.fillMaxWidth().background(Yellow).statusBarsPadding().padding(24.dp)) {
            Text("我的", style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.padding(top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(64.dp).background(Color.White.copy(alpha = .7f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, null, Modifier.size(36.dp)) }
                Column {
                    Text(settings.recipient, style = MaterialTheme.typography.titleLarge)
                    Text("今天也要认真吃饭", fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(20.dp)) {
                Text("常用配送信息", style = MaterialTheme.typography.titleMedium)
                Text(settings.address, modifier = Modifier.padding(top = 12.dp))
                Text("${settings.recipient} · ${settings.phone}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                Text("可在确认订单时修改，提交后自动保存。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(20.dp)) {
                Text("演示模式", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("离线演示", fontWeight = FontWeight.SemiBold); Text("无需联网，安装即可体验", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) }
                    Switch(checked = offline, onCheckedChange = { offline = it }, modifier = Modifier.testTag("offline_switch"))
                }
                if (!offline) OutlinedTextField(endpoint, { endpoint = it }, label = { Text("后端服务地址") },
                    placeholder = { Text("http://10.0.2.2:8000") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).testTag("endpoint"), shape = RoundedCornerShape(12.dp))
                Text(if (offline) "店铺、菜品、购物车和订单均可在本机演示。" else "填写可访问的服务地址，连接后可同步服务器订单。", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                Button(onClick = { configure(offline, endpoint) }, enabled = !loading,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp).testTag("save_settings")) {
                    Text(if (loading) "正在连接…" else "保存并应用")
                }
                Text("当前：${if (settings.offline) "离线演示" else "联网模式"}", fontSize = 11.sp, color = Green, modifier = Modifier.padding(top = 10.dp))
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(20.dp)) {
                Text("关于小黄外卖", style = MaterialTheme.typography.titleMedium)
                Text("从附近好店到一份热饭，体验完整的选餐、下单和模拟支付流程。", color = Muted, lineHeight = 22.sp, modifier = Modifier.padding(top = 12.dp))
                Text("v1.0.0 · Android课程设计演示版", fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}
