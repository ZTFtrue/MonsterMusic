package com.ztftrue.music.ui.public

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ztftrue.music.R
import com.ztftrue.music.play.PlayUtils
import com.ztftrue.music.sqlData.MusicDatabase
import com.ztftrue.music.sqlData.model.SortFiledData
import com.ztftrue.music.utils.Utils.toPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reusable sort popup for Main tabs, album/artist tracks, and folder views.
 * Loads and saves sort configuration asynchronously without blocking the UI thread.
 */
@Composable
fun SortSelectionPopup(
    sortType: String,
    sortFiledOptions: Map<String, String>,
    onDismissRequest: () -> Unit,
    onSortConfirmed: (filedSelected: String, methodSelected: String, filedCode: String) -> Unit
) {
    val context = LocalContext.current
    var filedSelected by remember { mutableStateOf("") }
    var methodSelected by remember { mutableStateOf("") }

    LaunchedEffect(sortType) {
        withContext(Dispatchers.IO) {
            val db = MusicDatabase.getDatabase(context).SortFiledDao()
            val sortData = db.findSortByType(sortType)
            if (sortData != null) {
                withContext(Dispatchers.Main) {
                    filedSelected = sortData.filedName
                    methodSelected = sortData.methodName
                }
            }
        }
    }

    Popup(
        alignment = Alignment.TopEnd,
        properties = PopupProperties(),
        offset = IntOffset(-10.dp.toPx(), 50.dp.toPx()),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(top = 5.dp)
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            LazyColumn(
                contentPadding = PaddingValues(5.dp),
                modifier = Modifier
            ) {
                // Default option
                item {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .wrapContentSize()
                            .clickable {
                                filedSelected = ""
                                methodSelected = ""
                            }
                            .padding(all = Dp(value = 8F))
                    ) {
                        Text(
                            text = stringResource(R.string.default_set),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        RadioButton(
                            selected = filedSelected.isEmpty(),
                            modifier = Modifier.wrapContentSize(),
                            onClick = {
                                filedSelected = ""
                                methodSelected = ""
                            }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .height(1.dp)
                            .wrapContentSize(align = Alignment.BottomCenter)
                            .width(140.dp)
                            .background(color = MaterialTheme.colorScheme.onBackground)
                    )
                }

                // Field options
                val filedKeys = sortFiledOptions.keys.toList()
                items(filedKeys.size) { i ->
                    val key = filedKeys[i]
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .wrapContentSize()
                            .clickable {
                                filedSelected = key
                                if (methodSelected.isBlank()) {
                                    methodSelected = PlayUtils.methodMap.entries.first().key
                                }
                            }
                            .padding(all = Dp(value = 8F))
                    ) {
                        Text(
                            text = key,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        RadioButton(
                            selected = (key == filedSelected),
                            modifier = Modifier.wrapContentSize(),
                            onClick = {
                                filedSelected = key
                                if (methodSelected.isBlank()) {
                                    methodSelected = PlayUtils.methodMap.entries.first().key
                                }
                            }
                        )
                    }
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier
                            .height(1.dp)
                            .wrapContentSize(align = Alignment.BottomCenter)
                            .width(140.dp)
                            .background(color = MaterialTheme.colorScheme.onBackground)
                    )
                }

                // Method options (Ascending / Descending)
                val methodKeys = PlayUtils.methodMap.keys.toList()
                items(methodKeys.size) { i ->
                    val key = methodKeys[i]
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(all = Dp(value = 8F))
                            .clickable {
                                methodSelected = key
                                if (filedSelected.isBlank()) {
                                    filedSelected = sortFiledOptions.entries.first().key
                                }
                            }
                    ) {
                        Text(
                            text = key,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        RadioButton(
                            selected = (key == methodSelected),
                            modifier = Modifier.wrapContentSize(),
                            onClick = {
                                methodSelected = key
                                if (filedSelected.isBlank()) {
                                    filedSelected = sortFiledOptions.entries.first().key
                                }
                            }
                        )
                    }
                }
            }

            // Bottom action buttons
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .wrapContentSize()
                    .width(120.dp)
            ) {
                IconButton(
                    modifier = Modifier.width(50.dp),
                    onClick = onDismissRequest
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_display_sort_popup),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    modifier = Modifier.width(50.dp),
                    onClick = {
                        val capturedField = filedSelected
                        val capturedMethod = methodSelected
                        val filedCode = sortFiledOptions[capturedField] ?: ""
                        onSortConfirmed(capturedField, capturedMethod, filedCode)
                        onDismissRequest()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Confirm",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
