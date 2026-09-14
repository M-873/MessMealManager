package com.example.messmealmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.messmealmanager.auth.AuthManager
import com.example.messmealmanager.data.FirestoreRepository
import com.example.messmealmanager.ui.components.NetworkRequiredLayout
import com.example.messmealmanager.ui.screens.access.ManageAccessScreen
import com.example.messmealmanager.ui.screens.access.ManageAccessViewModel
import com.example.messmealmanager.ui.screens.auth.SignInScreen
import com.example.messmealmanager.ui.screens.auth.SignInViewModel
import com.example.messmealmanager.ui.screens.home.HomeScreen
import com.example.messmealmanager.ui.screens.home.HomeViewModel
import com.example.messmealmanager.ui.screens.sheet.SheetDetailScreen
import com.example.messmealmanager.ui.screens.sheet.SheetDetailViewModel
import com.example.messmealmanager.ui.theme.MessMealManagerTheme
import com.example.messmealmanager.util.NetworkMonitor

import com.example.messmealmanager.ui.screens.profile.ProfileScreen
import com.example.messmealmanager.ui.screens.profile.ProfileViewModel

class MainActivity : ComponentActivity() {

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var authManager: AuthManager

    private val signInViewModel: SignInViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SignInViewModel(authManager) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        networkMonitor = NetworkMonitor(applicationContext)
        firestoreRepository = FirestoreRepository()
        authManager = AuthManager(this, firestoreRepository)

        setContent {
            MessMealManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        networkMonitor = networkMonitor,
                        authManager = authManager,
                        firestoreRepository = firestoreRepository,
                        signInViewModel = signInViewModel
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.unregister()
    }
}

@Composable
fun AppNavigation(
    networkMonitor: NetworkMonitor,
    authManager: AuthManager,
    firestoreRepository: FirestoreRepository,
    signInViewModel: SignInViewModel
) {
    val navController = rememberNavController()
    val startDestination = if (authManager.currentUser != null) "home" else "sign_in"

    NetworkRequiredLayout(networkMonitor = networkMonitor) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable("sign_in") {
                SignInScreen(
                    viewModel = signInViewModel,
                    authManager = authManager,
                    networkMonitor = networkMonitor,
                    onNavigateToHome = {
                        navController.navigate("home") {
                            popUpTo("sign_in") { inclusive = true }
                        }
                    }
                )
            }

            composable("home") {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return HomeViewModel(firestoreRepository, authManager) as T
                        }
                    }
                )

                HomeScreen(
                    viewModel = homeViewModel,
                    onSheetClick = { sheet ->
                        navController.navigate("sheet_detail/${sheet.id}")
                    },
                    onProfileClick = {
                        navController.navigate("profile")
                    },
                    onSignOut = {
                        signInViewModel.resetState()
                        navController.navigate("sign_in") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable("profile") {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ProfileViewModel(firestoreRepository, authManager) as T
                        }
                    }
                )

                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSignOut = {
                        signInViewModel.resetState()
                        navController.navigate("sign_in") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = "sheet_detail/{sheetId}",
                arguments = listOf(navArgument("sheetId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sheetId = backStackEntry.arguments?.getString("sheetId") ?: ""
                val sheetDetailViewModel: SheetDetailViewModel = viewModel(
                    key = "sheet_detail_$sheetId",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SheetDetailViewModel(sheetId, firestoreRepository, authManager) as T
                        }
                    }
                )

                SheetDetailScreen(
                    viewModel = sheetDetailViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToManageAccess = { targetSheetId ->
                        navController.navigate("manage_access/$targetSheetId")
                    }
                )
            }

            composable(
                route = "manage_access/{sheetId}",
                arguments = listOf(navArgument("sheetId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sheetId = backStackEntry.arguments?.getString("sheetId") ?: ""
                val manageAccessViewModel: ManageAccessViewModel = viewModel(
                    key = "manage_access_$sheetId",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ManageAccessViewModel(sheetId, firestoreRepository, authManager) as T
                        }
                    }
                )

                ManageAccessScreen(
                    viewModel = manageAccessViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
