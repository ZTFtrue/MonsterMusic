package com.ztftrue.music.ui.public

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ztftrue.music.Router

@Composable
fun BackButton(
    navController: NavHostController
) {
    fun NavHostController.navigateBack(onIsLastComposable: () -> Unit = {}) {
        if (Router.MainView.route == currentDestination?.navigatorName) {
            onIsLastComposable()
        } else {
            navigateUp()
        }
    }
    // Composition Local
//    val LocalNavigationProvider = staticCompositionLocalOf { ... }
//    LocalNavigationProvider provides navController // setValue
//    val navController = LocalNavigationProvider.current // useValue
    IconButton(onClick = {
        navController.navigateBack { }
    }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",  tint = MaterialTheme.colorScheme.onBackground)
    }
}


