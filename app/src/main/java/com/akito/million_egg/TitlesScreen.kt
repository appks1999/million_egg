package com.akito.million_egg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akito.million_egg.ui.AutoResizedText
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitlesScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val progress by viewModel.progress.collectAsState()
    val totalDamage = progress.totalDamage
    
    val unlockedCount = Titles.count { it.requirement <= totalDamage }
    val achievementPercent = if (Titles.isNotEmpty()) (unlockedCount.toFloat() / Titles.size * 100).toInt() else 0

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("◀", fontSize = 18.sp, color = Color.Black) 
                    }
                    
                    AutoResizedText(
                        text = "アチーブメント",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F2E9)) // 原神風の薄いベージュ背景
        ) {
            // 達成進捗ヘッダー
            AchievementProgressHeader(achievementPercent)

            // 称号リスト
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(Titles) { title ->
                    TitleItem(title, totalDamage >= title.requirement)
                }
            }
        }
    }
}

@Composable
fun AchievementProgressHeader(percent: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アイコン
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF8E82BD)),
                contentAlignment = Alignment.Center
            ) {
                Text("🏆", fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "達成進捗",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFE6A055),
                    trackColor = Color(0xFFD9D9D9)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "以下の目標を達成すると、特別な称号が獲得できます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitleItem(title: Title, isUnlocked: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color.White else Color(0xFFECECEC).copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 称号の紋章アイコン
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .border(2.dp, if (isUnlocked) Color(0xFFE6A055) else Color.LightGray, RoundedCornerShape(25.dp))
                    .background(if (isUnlocked) Color(0xFFFFF9C4) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isUnlocked) "⭐" else "🔒", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                AutoResizedText(
                    text = title.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.Black else Color.Gray,
                    maxLines = 1
                )
                AutoResizedText(
                    text = "累計ダメージ ${String.format(Locale.getDefault(), "%,d", title.requirement)} 達成",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                    maxLines = 1
                )
                
                // ボーナス表示（FlowRowで狭い画面でも安全に折返し）
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (title.powerBonus > 0) {
                        BonusBadge("パワー +${title.powerBonus}")
                    }
                    if (title.criticalBonus > 0.0) {
                        BonusBadge("クリティカル +${(title.criticalBonus * 100).toInt()}%")
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isUnlocked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "達成",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            } else {
                Text(
                    text = "進行中",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun BonusBadge(text: String) {
    Surface(
        color = Color(0xFFE3F2FD),
        shape = RoundedCornerShape(4.dp)
    ) {
        AutoResizedText(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = Color(0xFF1976D2),
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
