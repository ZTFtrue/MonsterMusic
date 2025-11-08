package com.ztftrue.music.ui.public

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ztftrue.music.Router

@Composable
fun BackButton(
    navController: SnapshotStateList<Any>
) {
    fun navigateBack(onIsLastComposable: () -> Unit = {}) {
        if (Router.MainView == navController.last()) {
            onIsLastComposable()
        } else {
            navController.removeLastOrNull()
        }
    }
    // Composition Local
//    val LocalNavigationProvider = staticCompositionLocalOf { ... }
//    LocalNavigationProvider provides navController // setValue
//    val navController = LocalNavigationProvider.current // useValue
    IconButton(onClick = {
        navigateBack { }
    }) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}


