package com.example.healthjournal.auth

import android.accounts.Account
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.healthjournal.R
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.services.drive.DriveScopes

class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val TAG = "GoogleAuthManager"

    suspend fun signIn(): GoogleIdTokenCredential? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.google_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            GoogleIdTokenCredential.createFrom(result.credential.data)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            null
        }
    }

    fun requestDriveAuthorization(email: String, onResolutionRequired: (android.app.PendingIntent) -> Unit, onSuccess: (String) -> Unit) {
        val requestedScopes = listOf(Scope(DriveScopes.DRIVE_FILE))
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    result.pendingIntent?.let { onResolutionRequired(it) }
                } else {
                    result.accessToken?.let { onSuccess(it) }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Authorization failed", e)
            }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
        }
    }

    fun getAccount(email: String): Account {
        return Account(email, "com.google")
    }
}
