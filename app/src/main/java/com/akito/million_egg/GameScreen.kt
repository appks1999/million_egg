package com.akito.million_egg

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.ui.tooling.preview.Preview
import com.akito.million_egg.ui.theme.Million_eggTheme
import java.util.Locale
import kotlin.math.roundToInt

data class GoldPopUp(val id: Long, val amount: Long, val offset: Offset)
data class CriticalPopUp(val id: Long, val offset: Offset)

//デバッグモードを使用したいときはSHOW_DEBUG_MENUをtrueにする
private const val SHOW_DEBUG_MENU = true

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel(),
    onNavigateToTitles: () -> Unit = {},
    onShowRewardedAd: () -> Unit = {}
) {
    val progress by viewModel.progress.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val autoTapCountdown by viewModel.autoTapCountdown.collectAsState()
    val feverCountdown by viewModel.feverCountdown.collectAsState()

    var showAdDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val shakeOffset = remember { Animatable(0f) }

    // ポップアップ管理
    val goldPopups = remember { mutableStateListOf<GoldPopUp>() }
    val criticalPopups = remember { mutableStateListOf<CriticalPopUp>() }
    var nextPopupId by remember { mutableLongStateOf(0L) }

    // ゴールド獲得イベントの監視
    LaunchedEffect(Unit) {
        viewModel.goldEarnedEvent.collectLatest { event ->
            val id = nextPopupId++
            goldPopups.add(GoldPopUp(id, event.amount, event.offset))
            // 1.3秒後に削除
            coroutineScope.launch {
                delay(1300)
                goldPopups.removeAll { it.id == id }
            }
        }
    }

    // クリティカルイベントの監視
    LaunchedEffect(Unit) {
        viewModel.criticalHitEvent.collectLatest { event ->
            val id = nextPopupId++
            criticalPopups.add(CriticalPopUp(id, event.offset))
            // 1秒後に削除
            coroutineScope.launch {
                delay(1000)
                criticalPopups.removeAll { it.id == id }
            }

            coroutineScope.launch {
                repeat(2) { // 回数を大幅に減らす（ガタッという一瞬の衝撃）
                    shakeOffset.animateTo(
                        targetValue = 15f, // 揺れ幅も少し抑える
                        animationSpec = spring(stiffness = Spring.StiffnessHigh)
                    )
                    shakeOffset.animateTo(
                        targetValue = -15f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh)
                    )
                }
                shakeOffset.animateTo(0f)
            }
        }
    }

    val isFever = feverCountdown > 0
    val backgroundColor by animateColorAsState(
        targetValue = if (isFever) Color(0xFFFFE082) else MaterialTheme.colorScheme.background,
        label = "feverBackground"
    )

    // 表示する卵の画像を決定するロジック
    val eggImageRes = when {
        isFever -> R.drawable.egg_g
        progress.remainingTaps <= 0 -> R.drawable.egg_damage10
        progress.remainingTaps <= 30000 -> R.drawable.egg_damage8
        progress.remainingTaps <= 500000 -> R.drawable.egg_damage5
        progress.remainingTaps <= 750000 -> R.drawable.egg_damage3
        progress.remainingTaps <= 900000 -> R.drawable.egg_damage1
        else -> R.drawable.egg_damage0
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ゴールドと情報
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💰 ${String.format(Locale.getDefault(), "%,d", playerState.gold)} G",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB300)
                )

                // 右上のボタン群
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // リワード広告ボタン
                    IconButton(
                        onClick = { showAdDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFFFB300), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "リワード広告",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    // 称号画面への遷移ボタン
                    IconButton(
                        onClick = onNavigateToTitles,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF8E82BD), CircleShape)
                    ) {
                        Text("🏆", fontSize = 20.sp)
                    }
                }
            }

            // カウントまたはフィーバー表示
            Text(
                text = if (isFever) "FEVER TIME!" else "残りタップ数",
                style = MaterialTheme.typography.titleMedium,
                color = if (isFever) Color.Red else MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isFever) "${feverCountdown}s" else String.format(Locale.getDefault(), "%,d", progress.remainingTaps),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isFever -> Color(0xFFFFB300)
                    progress.remainingTaps == 0L -> Color.Green
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            // 称号表示
            Text(
                text = "称号: ${playerState.currentTitle?.name ?: "なし"}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            // 卵画像
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (progress.remainingTaps <= 0L) {
                                showResetDialog = true
                            } else {
                                viewModel.onTap(offset)
                                // バウンスアニメーションの開始
                                coroutineScope.launch {
                                    scale.stop()
                                    scale.animateTo(
                                        targetValue = 0.95f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                                    )
                                    scale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioHighBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = eggImageRes),
                    contentDescription = "卵",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                        }
                )

                // ゴールドポップアップの描画 (卵のBox内に移動して座標を安定させる)
                goldPopups.forEach { popup ->
                    GoldPopUpEffect(popup, xOffset = 0)
                }

                // クリティカルポップアップの描画 (卵のBox内に移動)
                criticalPopups.forEach { popup ->
                    CriticalPopUpEffect(popup, xOffset = 0)
                }
            }


            // ステータス表示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("パワー", (playerState.tapPower + (playerState.currentTitle?.powerBonus ?: 0)).toString())
                StatItem("クリティカル", "${((playerState.criticalRate + (playerState.currentTitle?.criticalBonus ?: 0.0)) * 100).toInt()}%")
                val autoTapText = if (playerState.autoTapInterval > 0) "${playerState.autoTapInterval}秒毎" else "無効"
                StatItem("自動タップ", autoTapText)
            }

            // 自動タップカウントダウン
            if (playerState.autoTapInterval > 0) {
                Text(text = "次回の自動タップまで: ${autoTapCountdown}秒", style = MaterialTheme.typography.bodySmall)
            }

            // アップグレードボタン
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UpgradeButton(
                    label = "パワーアップ",
                    level = playerState.powerLevel,
                    cost = viewModel.getUpgradeCost(playerState.powerLevel),
                    currentGold = playerState.gold,
                    maxLevel = GameViewModel.MAX_POWER_LEVEL,
                    icon = Icons.Default.TouchApp,
                    accentColor = Color(0xFF958DCB),
                    hasFreeUpgrade = playerState.hasFreeUpgrade,
                    onClick = { viewModel.upgradePower() }
                )
                
                UpgradeButton(
                    label = "クリティカル率アップ",
                    level = playerState.criticalLevel,
                    cost = viewModel.getUpgradeCost(playerState.criticalLevel),
                    currentGold = playerState.gold,
                    maxLevel = 100,
                    icon = Icons.Default.Star,
                    accentColor = Color(0xFF5B9BD5),
                    hasFreeUpgrade = playerState.hasFreeUpgrade,
                    onClick = { viewModel.upgradeCritical() }
                )
                UpgradeButton(
                    label = if (playerState.autoTapLevel == 0) "自動タップを解放" else "自動タップ間隔を短縮",
                    level = playerState.autoTapLevel,
                    cost = viewModel.getUpgradeCost(playerState.autoTapLevel),
                    currentGold = playerState.gold,
                    maxLevel = GameViewModel.MAX_AUTO_TAP_LEVEL,
                    icon = Icons.Default.Timer,
                    accentColor = Color(0xFF54B1A3),
                    hasFreeUpgrade = playerState.hasFreeUpgrade,
                    onClick = { viewModel.upgradeAutoTap() }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            
            // 累計情報
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "累計タップ回数: ${String.format(Locale.getDefault(), "%,d", progress.tapCount)}", style = MaterialTheme.typography.bodySmall)
            }

            if (SHOW_DEBUG_MENU) {
                // デバッグ用メニュー
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("--- DEBUG MENU ---", style = MaterialTheme.typography.labelLarge)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.debugAddGold(1000) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) { Text("+1k G", fontSize = 10.sp) }
                    
                    Button(
                        onClick = { viewModel.debugTriggerFever() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) { Text("Fever", fontSize = 10.sp) }

                    Button(
                        onClick = { viewModel.debugTriggerCritical() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) { Text("Crit", fontSize = 10.sp) }
                    
                    Button(
                        onClick = { viewModel.debugAddDamage(5000) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) { Text("+5k Dmg", fontSize = 10.sp) }
                }

                Button(
                    onClick = { viewModel.debugAddDamage(progress.remainingTaps - 1) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("DEBUG: Almost Clear (Remain 1)", color = Color.White)
                }

                Button(
                    onClick = { viewModel.resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("全データリセット (TEST ONLY)", color = Color.White)
                }
            }
        }

        // リワード広告確認ダイアログ
        if (showAdDialog) {
            AlertDialog(
                onDismissRequest = { showAdDialog = false },
                title = { Text("無料パワーアップ") },
                text = { Text("動画広告を見て、パワーアップを無料にする") },
                confirmButton = {
                    TextButton(onClick = {
                        showAdDialog = false
                        onShowRewardedAd()
                    }) {
                        Text("広告を見る")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdDialog = false }) {
                        Text("キャンセル")
                    }
                }
            )
        }

        // カウント0時のリセット選択ダイアログ
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("ゲームのリセット") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("卵はすでに割れています。\nリセット方法を選択してください。")

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.startNewGamePlus()
                                showResetDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("強くてニューゲーム", color = Color.White)
                        }

                        Button(
                            onClick = {
                                viewModel.resetGame()
                                showResetDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Text("完全リセット", color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { showResetDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("キャンセル")
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun CriticalPopUpEffect(popup: CriticalPopUp, xOffset: Int) {
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(0.5f) }
    val yOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            // 素早く大きく表示して消える
            scale.animateTo(1.2f, animationSpec = tween(durationMillis = 200)) // 1.5f -> 1.2f
            scale.animateTo(1.0f, animationSpec = tween(durationMillis = 100)) // 1.2f -> 1.0f
            delay(400)
            alpha.animateTo(0f, animationSpec = tween(durationMillis = 300))
        }
        launch {
            // 高さをゴールドと変える (-150fで少し高めへ)
            yOffset.animateTo(-150f, animationSpec = tween(durationMillis = 1000, easing = FastOutLinearInEasing))
        }
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    popup.offset.x.roundToInt() - 150 + xOffset, // 補正を -50 -> -150 に強化（左へ寄せる）
                    (popup.offset.y + yOffset.value).roundToInt() - 200 // 補正を -100 -> -200 に強化（上へ寄せる）
                )
            }
            .graphicsLayer {
                this.alpha = alpha.value
                this.scaleX = scale.value
                this.scaleY = scale.value
            }
    ) {
        Text(
            text = "クリティカル!!",
            color = Color.Red,
            fontSize = 20.sp, // 28sp -> 20sp
            fontWeight = FontWeight.Black,
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black,
                    offset = Offset(3f, 3f),
                    blurRadius = 8f
                )
            )
        )
    }
}

@Composable
fun GoldPopUpEffect(popup: GoldPopUp, xOffset: Int) {
    val alpha = remember { Animatable(1f) }
    val yOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(0f, animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing))
        }
        launch {
            // 垂直移動距離を少し抑える
            yOffset.animateTo(-80f, animationSpec = tween(durationMillis = 1300, easing = LinearOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    popup.offset.x.roundToInt() - 100 + xOffset, // 補正を -50 -> -100 に強化
                    (popup.offset.y + yOffset.value).roundToInt() - 180 // 補正を -120 -> -180 に強化
                )
            }
            .graphicsLayer { this.alpha = alpha.value }
    ) {
        Text(
            text = "+${popup.amount}G",
            color = Color(0xFFFFB300),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UpgradeButton(
    label: String,
    level: Int,
    cost: Long,
    currentGold: Long,
    icon: ImageVector,
    accentColor: Color,
    hasFreeUpgrade: Boolean,
    maxLevel: Int = Int.MAX_VALUE,
    onClick: () -> Unit
) {
    val isMax = level >= maxLevel
    val canAfford = currentGold >= cost || hasFreeUpgrade
    
    Surface(
        onClick = onClick,
        enabled = !isMax && canAfford,
        shape = CircleShape,
        color = Color(0xFFF5F6FA), // 非常に薄いグレーの背景
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 左側のアイコンバッジ
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // テキストとレベルバッジ
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A4A4A)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "Lv.$level",
                        fontSize = 10.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 右側のコスト表示
                Surface(
                    color = if (canAfford) Color(0xFFE8EAF6) else Color(0xFFEEEEEE),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isMax) "MAX" else if (hasFreeUpgrade) "無料" else "${String.format(Locale.getDefault(), "%,d", cost)} G",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMax) Color.Gray else if (canAfford) Color(0xFF4A4A4A) else Color.LightGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    Million_eggTheme {
        GameScreen()
    }
}
