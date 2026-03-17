package co.tinode.tindroid.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import co.tinode.tindroid.desktop.AuthState
import co.tinode.tindroid.desktop.TinodeSession
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(session: TinodeSession) {
    val auth by session.auth.collectAsState()
    val scope = rememberCoroutineScope()

    var host by remember { mutableStateOf(session.defaultHost()) }
    var tls by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Tinode 电脑版（Compose Desktop）")
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(0.6f),
            value = host,
            onValueChange = { host = it },
            label = { Text("Host (host:port)") },
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(0.6f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = tls, onCheckedChange = { tls = it })
            Text("Use TLS (wss)")
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(0.6f),
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(0.6f),
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(0.6f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                enabled = auth !is AuthState.LoggingIn,
                onClick = {
                    scope.launch {
                        session.login(host.trim(), tls, username.trim(), password)
                    }
                },
            ) {
                Text("Login")
            }

            if (auth is AuthState.LoggingIn) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        if (auth is AuthState.Error) {
            Text((auth as AuthState.Error).message)
        }
    }
}

