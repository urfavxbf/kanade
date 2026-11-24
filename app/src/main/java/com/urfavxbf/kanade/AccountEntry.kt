package com.urfavxbf.kanade

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.urfavxbf.kanade.ui.LocalAppFont
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
internal fun AccountEntry(
    activity: Activity,
    dark: Boolean,
    content: @Composable (String, String, () -> Unit) -> Unit,
) {
    val prefs = remember { activity.getSharedPreferences("account", Context.MODE_PRIVATE) }
    val auth = remember { FirebaseApp.initializeApp(activity)?.let { FirebaseAuth.getInstance(it) } }
    val manager = remember { CredentialManager.create(activity) }
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf(auth?.currentUser) }
    var guest by remember { mutableStateOf(prefs.getBoolean("guest", false)) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableIntStateOf(0) }
    fun setGuest(value: Boolean) {
        prefs.edit().putBoolean("guest", value).apply()
        guest = value
    }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth?.addAuthStateListener(listener)
        onDispose { auth?.removeAuthStateListener(listener) }
    }
    if (user != null || guest) {
        val signedIn = user != null
        content(
            user?.email ?: user?.displayName ?: stringResource(if (signedIn) R.string.kanade_account else R.string.guest_account),
            stringResource(if (signedIn) R.string.sign_out else R.string.sign_in),
        ) {
            error = 0
            setGuest(false)
            if (signedIn) {
                busy = true
                auth?.signOut()
                scope.launch {
                    try {
                        manager.clearCredentialState(ClearCredentialStateRequest())
                    } catch (_: ClearCredentialException) {
                        // The Firebase session has already been closed.
                    } finally {
                        busy = false
                    }
                }
            }
        }
        return
    }
    val clientId = stringResource(R.string.default_web_client_id)
    val configured = auth != null && clientId.isNotBlank()
    val ink = if (dark) Color.White else Color.Black
    Column(
        Modifier.fillMaxSize().background(if (dark) Color(0xff161616) else Color.White)
            .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(stringResource(R.string.kanade_account), style = TextStyle(fontFamily = LocalAppFont.current, color = ink, fontSize = 32.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(28.dp))
        AccountButton(stringResource(if (busy) R.string.please_wait else R.string.google_sign_in), ink, configured && !busy) {
            busy = true
            error = 0
            scope.launch {
                try {
                    val option = GetSignInWithGoogleOption.Builder(clientId).build()
                    val response = manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
                    val token = GoogleIdTokenCredential.createFrom(response.credential.data).idToken
                    requireNotNull(auth).signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                } catch (_: GetCredentialCancellationException) {
                    // The user dismissed the account picker.
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: NoCredentialException) {
                    error = R.string.no_google_account
                } catch (_: Exception) {
                    error = R.string.account_error
                } finally {
                    busy = false
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        AccountButton(stringResource(R.string.continue_guest), ink, !busy, primary = false) { setGuest(true) }
        val message = if (!configured) R.string.account_unavailable else error
        if (message != 0) {
            Spacer(Modifier.height(16.dp))
            BasicText(stringResource(message), style = TextStyle(fontFamily = LocalAppFont.current, color = ink, fontSize = 15.sp))
        }
    }
}

@Composable
private fun AccountButton(label: String, ink: Color, enabled: Boolean, primary: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val fill = when {
        !enabled -> ink.copy(alpha = 0.08f)
        primary -> ink
        else -> Color.Transparent
    }
    val textColor = when {
        !enabled -> ink.copy(alpha = 0.55f)
        primary && ink == Color.White -> Color.Black
        primary -> Color.White
        else -> ink
    }
    Box(
        Modifier.widthIn(max = 400.dp).fillMaxWidth().heightIn(min = 52.dp)
            .clip(shape).background(fill)
            .border(1.dp, if (enabled && !primary) ink.copy(alpha = 0.30f) else Color.Transparent, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = TextStyle(fontFamily = LocalAppFont.current, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
    }
}
