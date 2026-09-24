package com.akito.million_egg

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@Composable
fun ClearScreen(
    viewModel: GameViewModel,
    onRestart: () -> Unit
) {
    val progress by viewModel.progress.collectAsState()
    val scrollState = rememberScrollState()
    
    // アニメーション用フラグ
    var startAnimation by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.playGameClearSound()
        startAnimation = true
        // 2.5秒後にボタンを表示する
        delay(2500)
        showButton = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)), // 背景を80%の黒で透過させる
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // 感謝のメッセージ
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(tween(2000)) + expandVertically(tween(2000))
            ) {
                Text(
                    text = "おめでとう!!\n100万回の卵が割れました",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 統計情報
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatRow("総タップ数", "${String.format(Locale.getDefault(), "%,d", progress.tapCount)} 回")
                StatRow("累計ダメージ", "${String.format(Locale.getDefault(), "%,d", progress.totalDamage)} ダメージ")
                
                val startDate = Date(progress.startDateMillis)
                val df = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                StatRow("プレイ開始日", df.format(startDate))
                
                val durationDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - progress.startDateMillis) + 1
                StatRow("達成までの期間", "${durationDays} 日")
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Thank you for Playing!!",
                color = Color(0xFFFFD700), // ゴールドカラー
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // リスタートボタン（遅延してふわっと浮き上がる）
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(tween(1500)) + slideInVertically(tween(1500)) { it / 2 }
            ) {
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Text("トップに戻る")
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 16.sp)
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
