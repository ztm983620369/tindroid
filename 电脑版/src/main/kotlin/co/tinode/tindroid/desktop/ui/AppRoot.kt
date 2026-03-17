package co.tinode.tindroid.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import co.tinode.tindroid.desktop.AuthState
import co.tinode.tindroid.desktop.TinodeSession

@Composable
fun AppRoot(session: TinodeSession) {
    val auth by session.auth.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (auth) {
            is AuthState.LoggedIn -> ChatScreen(session)
            else -> LoginScreen(session)
        }
    }
}

