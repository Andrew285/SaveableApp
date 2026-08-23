package com.rainyday.saveableapp.data.auth

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Bridges the app's existing Google Sign-In (used for Drive backup) into a Firebase Auth session,
 * so the AI-parsing Cloud Function can verify who's calling it via a Firebase ID token instead of
 * trusting a client-supplied API key.
 */
class FirebaseAuthRepository {
    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Exchanges an already-signed-in Google account (requested with an ID token) for a Firebase session. */
    suspend fun signInWithGoogleAccount(account: GoogleSignInAccount): Result<FirebaseUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                val idToken = requireNotNull(account.idToken) { "Google account is missing an ID token." }
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await().user
                    ?: error("Firebase sign-in returned no user.")
            }
        }

    /** A fresh Firebase ID token to authenticate a Cloud Functions call, or null if not signed in. */
    suspend fun getIdToken(): String? = withContext(Dispatchers.IO) {
        auth.currentUser?.getIdToken(false)?.await()?.token
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Fires immediately with the current uid (or null) and again on every future sign-in/out,
     * regardless of which screen triggered it. Used to start/stop Firestore sync.
     */
    fun addAuthStateListener(onChanged: (uid: String?) -> Unit) {
        auth.addAuthStateListener { firebaseAuth -> onChanged(firebaseAuth.currentUser?.uid) }
    }
}
