package com.ztftrue.music.ui.play

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ztftrue.music.MusicViewModel
import com.ztftrue.music.R
import com.ztftrue.music.ui.play.Drop.Companion.generateRandomChars
import kotlinx.coroutines.delay
import org.jaudiotagger.tag.FieldKey

@OptIn(ExperimentalFoundationApi::class)
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun CoverView(musicViewModel: MusicViewModel) {
    val listState = rememberLazyListState()
    var coverPaint by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val musicVisualizationEnable = remember { musicViewModel.musicVisualizationEnable }
    val showOtherMessage = remember { mutableStateOf(false) }

    val drops = remember { mutableStateListOf<MutableList<Drop>>() }
    val columnSpacing = 80f // 列之间的间隔
    val dropHeight = 80f // 字符的高度间隔
    val textSizeSet = 60f
    val density = LocalDensity.current.density
    val canvasHeight = remember { mutableFloatStateOf(0f) }
    val canvasWidth = remember { mutableFloatStateOf(0f) }
    val initDrops = remember { mutableStateOf(false) }
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GREEN
        textSize = textSizeSet
        typeface = android.graphics.Typeface.MONOSPACE // 指定为等宽字体
    }
    LaunchedEffect(musicViewModel.currentPlay.value) {
        coverPaint = musicViewModel.getCurrentMusicCover(context)
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
                                Color.Green.copy(alpha = ((columnRowDropsCount - row) / columnRowDropsCount.toFloat()).toFloat())
                            }
                        )
                    )
                }
            }
        }

    }

    // 动画更新
    LaunchedEffect(Unit, musicViewModel.playStatus.value, musicVisualizationEnable.value) {
        while (musicViewModel.playStatus.value && musicVisualizationEnable.value) {
            if (!musicViewModel.playStatus.value) break
//            drops.forEach { columnDrops ->
//                val cDrops = ArrayList<Int>()
//                // 更新每一列的雨滴
//                columnDrops.forEachIndexed { index3, drop ->
//                    drop.update(1f)
//                    // 检查是否超出屏幕
//                    if (drop.y > canvasHeight.floatValue + dropHeight) {
//                        cDrops.add(index3)
//                        drop.char = generateRandomChars()
//                    }
//                }
//
//                cDrops.forEach { index3 ->
//                    val d = columnDrops.removeAt(index3)
//                    d.y = columnDrops[columnDrops.size - 1].y - dropHeight
//                    columnDrops.add(d)
//                }
//            }
            musicViewModel.musicVisualizationData.forEachIndexed { index, magnitude ->
                drops.forEachIndexed { index2, columnDrops ->
                    // 更新每一列的雨滴
                    val cDrops = ArrayList<Int>()
                    columnDrops.forEachIndexed { index3, drop ->
                        if (index == index2) {
                            // TODO Use log function to calculate the speed
                            drop.update((magnitude+1))
                            // 检查是否超出屏幕
                            if (drop.y > canvasHeight.floatValue + dropHeight) {
                                cDrops.add(index3)
                                drop.char = generateRandomChars()
                            }
                        }
                    }
                    cDrops.forEach { index3 ->
                        val d = columnDrops.removeAt(index3)
                        d.y = columnDrops[columnDrops.size - 1].y - dropHeight
                        columnDrops.add(d)
                    }
                    //                                        val d=  columnDrops.removeAt(index3)
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
                val sizeB = minOf(maxWidth, maxHeight)
                Box(modifier = Modifier.size(sizeB)) {
                    if (!musicVisualizationEnable.value || musicViewModel.showMusicCover.value) {
                        key(musicViewModel.currentPlay.value) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    coverPaint ?: R.drawable.songs_thumbnail_cover
                                ), contentDescription = stringResource(R.string.cover),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .aspectRatio(1f)
                                    .background(color = Color.Black)
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
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = Color.Black)
                        ) {

                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .graphicsLayer {
                                clip = true // 禁用裁剪
                            }
                            .fillMaxSize()
                            .padding(start = 4.dp, end = 4.dp)
                    ) {
                        canvasHeight.floatValue = size.height
                        canvasWidth.floatValue = size.width
//                        val barWidth =
//                            size.width / magnitudes.size
//                        val maxBarHeight = size.width / 2
                        // 绘制每列的雨滴
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