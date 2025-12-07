package com.ztftrue.music.ui.play

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.ztftrue.music.ImageSource
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.play.MediaCommands
import com.ztftrue.music.ui.play.Drop.Companion.generateRandomChars
import kotlinx.coroutines.delay
import org.jaudiotagger.tag.FieldKey

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CoverView(musicViewModel: MusicViewModel) {
    val listState = rememberLazyListState()
    val musicVisualizationEnable = remember { musicViewModel.musicVisualizationEnable }
    val showOtherMessage = remember { mutableStateOf(true) }
    val magnitudes by musicViewModel.visualizationData.observeAsState(initial = emptyList())
    val drops = remember { mutableStateListOf<MutableList<Drop>>() }
    val columnSpacing = 80f // 列之间的间隔
    val dropHeight = 80f // 字符的高度间隔
    val textSizeSet = 60f
    val canvasHeight = remember { mutableFloatStateOf(0f) }
    val canvasWidth = remember { mutableFloatStateOf(0f) }
    val initDrops = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var shouldRun by remember { mutableStateOf(false) }
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GREEN
        textSize = textSizeSet
        typeface = android.graphics.Typeface.MONOSPACE
    }
    val imageModel: ImageSource by musicViewModel.currentMusicCover




    LaunchedEffect(musicViewModel.currentPlay.value) {
        initDrops.value = false
    }
    LaunchedEffect(
        canvasHeight.floatValue,
        canvasWidth.floatValue,
        initDrops.value, musicVisualizationEnable.value
    ) {
        if (!musicVisualizationEnable.value) {
            initDrops.value = false
            drops.clear()
            return@LaunchedEffect
        }
        if (initDrops.value) return@LaunchedEffect
        if (canvasHeight.floatValue > 0 && canvasWidth.floatValue > 0) {
            initDrops.value = true
            //每列有多少行
            val columnRowDropsCount = (canvasHeight.floatValue / dropHeight).toInt() + 10
            //有多少列
            val columnDropsCount = (canvasHeight.floatValue / columnSpacing).toInt()
            drops.clear()
            repeat(columnDropsCount) { column ->
                drops.add(mutableStateListOf())
                repeat(columnRowDropsCount) { row ->
                    drops[column].add(
                        Drop(
                            column * columnSpacing,
                            (-(row + 1)) * dropHeight,
                            color = if (row == columnRowDropsCount - 1) {
                                Color.White
                            } else {
                                Color.Green.copy(alpha = ((columnRowDropsCount - row) / columnRowDropsCount.toFloat()))
                            }
                        )
                    )
                }
            }
        }

    }
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    musicViewModel.browser?.sendCustomCommand(
                        MediaCommands.COMMAND_VISUALIZATION_CONNECTED,
                        Bundle()
                    )
                    shouldRun = true
                }

                Lifecycle.Event.ON_STOP -> {
                    musicViewModel.browser?.sendCustomCommand(
                        MediaCommands.COMMAND_VISUALIZATION_DISCONNECTED,
                        Bundle()
                    )
                    shouldRun = false
                }

                else -> Unit
            }
        }

        lifecycle.addObserver(observer)
        // Remove the observer when the Composable leaves the Composition
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(musicViewModel.playStatus.value, musicVisualizationEnable.value, shouldRun) {
        while (musicViewModel.playStatus.value && musicVisualizationEnable.value && shouldRun) {
            val screenBottom = canvasHeight.floatValue + dropHeight
            val loopLimit = minOf(magnitudes.size, drops.size)
            for (i in 0 until loopLimit) {
                val magnitude = magnitudes[i]
                val columnDrops = drops[i]
                val speed = magnitude + 1
                // 1. 更新所有雨滴位置
                // 使用索引遍历比 iterator 更快，且不产生 Garbage
                for (drop in columnDrops) {
                    drop.update(speed)
                }
                // 2. 检查并回收出界的雨滴
                // 假设：列表顺序代表垂直顺序，index 0 是最下面的雨滴。
                // 只需要检查头部元素，不需要遍历整个列表去找谁出界了。
                while (columnDrops.isNotEmpty() && columnDrops[0].y > screenBottom) {
                    // 移除头部 (最下方的雨滴)
                    val recycledDrop = columnDrops.removeAt(0)

                    // 重置状态
                    recycledDrop.char = generateRandomChars()

                    // 获取当前最上方雨滴的 Y 坐标 (现在是 List 的最后一个元素)
                    val tailY = if (columnDrops.isNotEmpty()) columnDrops.last().y else 0f

                    // 将回收的雨滴放置在最上方
                    recycledDrop.y = tailY - dropHeight

                    // 加到尾部
                    columnDrops.add(recycledDrop)
                }
            }
            delay(10) // 控制更新频率
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        items(1) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)// Force the canvas to be a square
            ) {
                if (!musicVisualizationEnable.value || musicViewModel.showMusicCover.value) {
                    key(musicViewModel.currentPlay.value) {
                        AsyncImage(
                            model = imageModel.asModel(),
                            contentDescription = stringResource(R.string.cover),
                            modifier = Modifier
                                .size(minOf(maxWidth, maxHeight))
                                .aspectRatio(1f)
                                .combinedClickable(
                                    onLongClick = {
                                        showOtherMessage.value = !showOtherMessage.value
                                    },
                                    onClick = {

                                    }
                                )
                        )
                    }
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(minOf(maxWidth, maxHeight))
                                .background(color = Color.Black)
                    ) {

                    }
                }
                Canvas(
                    modifier = Modifier
                        .size(minOf(maxWidth, maxHeight))
                        .graphicsLayer {
                            clip = true // 禁用裁剪
                        }

                        .padding(start = 4.dp, end = 4.dp)
                ) {
                    canvasHeight.floatValue = size.height
                    canvasWidth.floatValue = size.width
//                        val barWidth =
//                            size.width / magnitudes.size
//                        val maxBarHeight = size.width / 2
                    drops.forEach { columnDrops ->
                        columnDrops.forEach { drop ->
                            drawContext.canvas.nativeCanvas.apply {
                                textPaint.color = drop.color.toArgb()
                                drawText(
                                    drop.char,
                                    drop.x,
                                    drop.y,
                                    textPaint
                                )
                            }
                        }
                    }
                }
            }

        }
        item {
            if (showOtherMessage.value) {
                Column(Modifier.padding(15.dp)) {
                    musicViewModel.currentPlay.value?.let { it1 ->
                        Text(
                            text = it1.path, modifier =
                                Modifier
                                    .padding(0.dp)
                                    .horizontalScroll(rememberScrollState(0))
                                    .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                        Text(
                            text = stringResource(R.string.artist, it1.artist), modifier =
                                Modifier
                                    .padding(0.dp)
                                    .height(30.dp)
                                    .horizontalScroll(rememberScrollState(0))
                                    .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                        Text(
                            text = stringResource(R.string.album, it1.album), modifier =
                                Modifier
                                    .padding(top = 10.dp)
                                    .horizontalScroll(rememberScrollState(0))
                                    .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                        Text(
                            text = stringResource(
                                R.string.comment,
                                musicViewModel.tags[FieldKey.COMMENT.name] ?: ""
                            ), modifier =
                                Modifier
                                    .padding(top = 10.dp)
                                    .horizontalScroll(rememberScrollState(0))
                                    .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                        Text(
                            text = stringResource(
                                R.string.year_tracks,
                                musicViewModel.tags[FieldKey.YEAR.name] ?: ""
                            ), modifier =
                                Modifier
                                    .padding(top = 10.dp)
                                    .horizontalScroll(rememberScrollState(0))
                                    .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )

                        Text(
                            text = stringResource(
                                R.string.sample_rate_hz_format,
                                musicViewModel.currentInputFormat["SampleRate"] ?: ""
                            ),
                            modifier =
                                Modifier
                                    .padding(top = 10.dp)
                                    .horizontalScroll(rememberScrollState(0))
                                    .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                        Text(
                            text = stringResource(
                                R.string.bitrate_format,
                                musicViewModel.currentInputFormat["Bitrate"] ?: ""
                            ),
                            modifier =
                                Modifier
                                    .padding(top = 10.dp)
                                    .horizontalScroll(rememberScrollState(0))
                                    .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                        Text(
                            text = stringResource(
                                R.string.channel_count_format,
                                musicViewModel.currentInputFormat["ChannelCount"] ?: ""
                            ),
                            modifier =
                                Modifier
                                    .padding(top = 10.dp)
                                    .horizontalScroll(rememberScrollState(0))
                                    .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                        Text(
                            text = stringResource(
                                R.string.codec_format,
                                musicViewModel.currentInputFormat["Codec"] ?: ""
                            ),
                            modifier =
                                Modifier
                                    .padding(top = 10.dp)
                                    .horizontalScroll(rememberScrollState(0))
                                    .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onBackground,// Set the text color here
                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                        )
                    }
                }
            }
        }
    }
}


// 雨滴类
class Drop(
    var x: Float,
    y: Float,
    var color: Color = Color.Green,
    var char: String = generateRandomChars()
) {
    //    var x   by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)

    //    var color by mutableStateOf(color)
//    var char by mutableStateOf(char)
    fun update(speed: Float) {
//        char = generateRandomChars()
        y += speed // 下落速度
    }

    companion object {
        fun generateRandomChars(): String {
            val matrixChars = listOf(
                'あ', 'い', 'う', 'え', 'お',
                'か', 'き', 'く', 'け', 'こ',
                'さ', 'し', 'す', 'せ', 'そ',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
                'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                'α', 'β', 'γ', 'δ', 'ε', 'ζ', 'η', 'θ', 'ι', 'κ', 'λ', 'μ', 'ν', 'ξ', 'ο', 'π',
                'ρ', 'σ', 'τ', 'υ', 'φ', 'χ', 'ψ', 'ω', 'Ω', 'Σ', 'Δ',
                '#', '%', '@', '*', '&', '!', '?', '+', '-', '=', '/', '|', '\\', ':', ';', '<', '>'
            )
            return matrixChars.random().toString()
        }
    }
}