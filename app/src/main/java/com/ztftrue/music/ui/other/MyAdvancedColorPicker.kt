package com.ztftrue.music.ui.other

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
fun MyAdvancedColorPicker(onColorChanged: (ColorEnvelope) -> Unit) {
    val controller = rememberColorPickerController()

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HsvColorPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            controller = controller,
            onColorChanged = onColorChanged
        )

        Spacer(modifier = Modifier.height(20.dp))

        BrightnessSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp),
            controller = controller
        )

        Spacer(modifier = Modifier.height(20.dp))

        AlphaSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp),
            controller = controller
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "#${controller.selectedColor.value}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RectangleShape)
                .background(controller.selectedColor.value) // 使用包含 Alpha 的颜色
        )
    }
}