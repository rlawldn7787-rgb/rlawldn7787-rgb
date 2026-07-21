package com.woohaeng.board

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.woohaeng.board.ui.CaptureScreen
import com.woohaeng.board.ui.LoginScreen
import com.woohaeng.board.ui.RecordDetailScreen
import com.woohaeng.board.ui.RecordsScreen
import com.woohaeng.board.ui.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0F6B4C),
                    secondary = Color(0xFF1A2420),
                    background = Color(0xFFF3F5F2),
                    surface = Color.White
                )
            ) {
                val vm: AppViewModel = viewModel()
                val token by vm.token.collectAsState()
                val nav = rememberNavController()
                val start = if (token.isNullOrBlank()) "login" else "records"

                NavHost(navController = nav, startDestination = start) {
                    composable("login") {
                        LoginScreen(
                            vm = vm,
                            onSuccess = {
                                nav.navigate("records") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("records") {
                        RecordsScreen(
                            vm = vm,
                            onCapture = { nav.navigate("capture") },
                            onOpen = { id -> nav.navigate("detail/$id") },
                            onLogout = {
                                vm.logout()
                                nav.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("capture") {
                        CaptureScreen(
                            vm = vm,
                            onDone = { nav.popBackStack() },
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable(
                        "detail/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType })
                    ) { entry ->
                        val id = entry.arguments?.getInt("id") ?: return@composable
                        RecordDetailScreen(
                            vm = vm,
                            id = id,
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
