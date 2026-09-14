package com.example.messmealmanager.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.messmealmanager.auth.AuthManager
import com.example.messmealmanager.ui.components.NetworkRequiredLayout
import com.example.messmealmanager.util.NetworkMonitor
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun SignInScreen(
    viewModel: SignInViewModel,
    authManager: AuthManager,
    networkMonitor: NetworkMonitor,
    onNavigateToHome: (String) -> Unit
) {
    val signInState by viewModel.signInState.collectAsState()
    val context = LocalContext.current

    // Launcher for the Google Sign-In intent
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.handleSignInResult(account.idToken)
            } catch (e: ApiException) {
                viewModel.handleSignInResult(null)
            }
        } else {
            viewModel.handleSignInResult(null)
        }
    }

    LaunchedEffect(signInState) {
        if (signInState is SignInState.Success) {
            val user = (signInState as SignInState.Success).user
            onNavigateToHome(user.displayName ?: "User")
        }
    }

    NetworkRequiredLayout(networkMonitor = networkMonitor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mess Meal Manager",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            when (signInState) {
                is SignInState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Signing in...")
                }
                is SignInState.Error -> {
                    Text(
                        text = (signInState as SignInState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.resetState() }) {
                        Text("Try Again")
                    }
                }
                else -> {
                    Button(
                        onClick = { launcher.launch(authManager.getSignInIntent()) },
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }
}
