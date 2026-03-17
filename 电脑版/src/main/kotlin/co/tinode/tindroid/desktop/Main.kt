package co.tinode.tindroid.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.tinode.tindroid.desktop.ui.AppRoot

fun main() = application {
    val session = remember { TinodeSession() }

    Window(
        onCloseRequest = {
            session.shutdown()
            exitApplication()
        },
        title = "Tinode 电脑版",
    ) {
        MaterialTheme {
            AppRoot(session)
        }
    }
}

