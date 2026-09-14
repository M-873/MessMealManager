package com.example.messmealmanager.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.messmealmanager.auth.AuthManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SignInState {
    object Idle : SignInState()
    object Loading : SignInState()
    data class Success(val user: FirebaseUser) : SignInState()
    data class Error(val message: String) : SignInState()
}

class SignInViewModel(
    private val authManager: AuthManager
) : ViewModel() {

    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    init {
        // Check if user is already signed in
        authManager.currentUser?.let { user ->
            _signInState.value = SignInState.Success(user)
        }
    }

    fun handleSignInResult(idToken: String?) {
        if (idToken == null) {
            _signInState.value = SignInState.Error("Google Sign-In failed or was cancelled.")
            return
        }

        _signInState.value = SignInState.Loading
        viewModelScope.launch {
            val result = authManager.signInWithGoogleToken(idToken)
            result.onSuccess { user ->
                _signInState.value = SignInState.Success(user)
            }.onFailure { error ->
                _signInState.value = SignInState.Error(error.message ?: "Authentication failed")
            }
        }
    }

    fun resetState() {
        _signInState.value = SignInState.Idle
    }
}
