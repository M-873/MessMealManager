package com.example.messmealmanager.auth

import android.content.Context
import android.content.Intent
import com.example.messmealmanager.data.FirestoreRepository
import com.example.messmealmanager.model.Member
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository
) {
    private val auth = FirebaseAuth.getInstance()
    
    // NOTE: You must replace "YOUR_WEB_CLIENT_ID" with the actual Web client ID 
    // from your google-services.json (client -> oauth_client with type 3) or Firebase Console.
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("348932764632-ndj0u85jcgri3hb0evpqmrq801qsd2ep.apps.googleusercontent.com")
        .requestEmail()
        .build()

    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Call this with the ID token received from the Google Sign-In intent result.
     */
    suspend fun signInWithGoogleToken(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("User is null after sign in")
            
            // Save or update user in Firestore
            saveOrUpdateUserToFirestore(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveOrUpdateUserToFirestore(user: FirebaseUser) {
        val existingMember = firestoreRepository.getMember(user.uid)
        val nameToUse = existingMember?.name?.ifBlank { user.displayName } ?: (user.displayName ?: "User")
        val member = Member(
            userId = user.uid,
            name = nameToUse,
            photoUrl = user.photoUrl?.toString() ?: existingMember?.photoUrl ?: "",
            email = user.email ?: existingMember?.email ?: ""
        )
        
        firestoreRepository.addOrUpdateMember(member)
    }
    
    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }
}
