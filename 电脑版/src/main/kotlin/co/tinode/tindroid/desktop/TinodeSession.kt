package co.tinode.tindroid.desktop

import co.tinode.tindroid.desktop.store.DesktopStore
import co.tinode.tinodesdk.Tinode
import co.tinode.tinodesdk.Topic
import co.tinode.tinodesdk.model.PrivateType
import co.tinode.tinodesdk.model.TheCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.Date

class TinodeSession {
    // Copied from Android client (tindroid/app/src/main/java/co/tinode/tindroid/Cache.java)
    private val apiKey = "AQAAAAABAADogSXzdb1qdpLotw8wPFH-"
    private val appName = "TindroidDesktop"

    val store = DesktopStore()

    val tinode: Tinode = Tinode(appName, apiKey, store, null).apply {
        setDefaultTypeOfMetaPacket(TheCard::class.java, PrivateType::class.java)
        setMeTypeOfMetaPacket(TheCard::class.java)
        setFndTypeOfMetaPacket(TheCard::class.java)
        setLanguage(System.getProperty("user.language") ?: "en")
    }

    private val _auth = MutableStateFlow<AuthState>(AuthState.LoggedOut)
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _chats = MutableStateFlow<List<ChatItem>>(emptyList())
    val chats: StateFlow<List<ChatItem>> = _chats.asStateFlow()

    private val _selectedTopic = MutableStateFlow<String?>(null)
    val selectedTopic: StateFlow<String?> = _selectedTopic.asStateFlow()

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val listener = object : Tinode.EventListener {
        override fun onConnect(code: Int, reason: String, params: MutableMap<String, Any?>?) {
            _connection.update { ConnectionState.Connected(reason) }
        }

        override fun onDisconnect(byServer: Boolean, code: Int, reason: String) {
            _connection.update { ConnectionState.Disconnected(code, reason, byServer) }
            _auth.update { AuthState.LoggedOut }
            rebuildChatList()
        }

        override fun onLogin(code: Int, text: String) {
            if (code in 200..299) {
                _auth.update { AuthState.LoggedIn(tinode.myId, tinode.authToken) }
                rebuildChatList()
            } else {
                _auth.update { AuthState.Error("Login failed: $code $text") }
            }
        }

        override fun onMetaMessage(meta: co.tinode.tinodesdk.model.MsgServerMeta<*, *, *, *>) {
            rebuildChatList()
            refreshSelectedMessages()
        }

        override fun onDataMessage(data: co.tinode.tinodesdk.model.MsgServerData) {
            rebuildChatList()
            refreshSelectedMessages()
        }
    }

    init {
        tinode.addListener(listener)
    }

    fun defaultHost(): String = "119.45.226.7:6060"

    suspend fun login(host: String, tls: Boolean, username: String, password: String) {
        _auth.update { AuthState.LoggingIn }
        _connection.update { ConnectionState.Connecting(host, tls) }

        withContext(Dispatchers.IO) {
            tinode.connect(host, tls, false).getResult()
            tinode.loginBasic(username, password).getResult()

            val me = tinode.getOrCreateMeTopic<TheCard>()
            val query = me.metaGetBuilder.withDesc().withSub().withTags().build()
            me.subscribe(null, query).getResult()
        }
    }

    fun logout() {
        try {
            tinode.logout()
        } catch (_: Exception) {
        } finally {
            store.logout()
            _selectedTopic.update { null }
            _messages.update { emptyList() }
            _chats.update { emptyList() }
            _auth.update { AuthState.LoggedOut }
        }
    }

    fun shutdown() {
        try {
            tinode.maybeDisconnect(false)
        } catch (_: Exception) {
        }
    }

    suspend fun selectTopic(topicName: String) {
        _selectedTopic.update { topicName }
        refreshSelectedMessages()

        val topic = tinode.getTopic(topicName) ?: return
        if (!topic.isAttached) {
            withContext(Dispatchers.IO) {
                // Fetch description + last messages.
                val query = topic.metaGetBuilder
                    .withDesc()
                    .withLaterData(50)
                    .withSub()
                    .build()
                topic.subscribe(null, query).getResult()
            }
        }
    }

    suspend fun sendMessage(text: String) {
        val topicName = _selectedTopic.value ?: return
        val topic = tinode.getTopic(topicName) ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        withContext(Dispatchers.IO) {
            try {
                topic.publish(trimmed).getResult()
            } catch (e: Exception) {
                _auth.update { AuthState.Error(e.message ?: e.javaClass.name) }
            }
        }
    }

    private fun rebuildChatList() {
        val topics = tinode.getTopics().filter { t -> t.isP2PType || t.isGrpType }
        val items = topics.map { t ->
            val title = topicTitle(t)
            val last = tinode.getLastMessage(t.name)
            val preview = last?.content?.toString() ?: ""
            val ts = t.touched ?: t.updated ?: t.created
            val unread = (t.seq - t.read).coerceAtLeast(0)
            ChatItem(
                topicName = t.name,
                title = title,
                preview = preview,
                touched = ts,
                unread = unread,
                pinnedRank = t.pinnedRank,
            )
        }.sortedWith(
            compareByDescending<ChatItem> { it.pinnedRank }.thenByDescending { it.touched?.time ?: 0L }
        )

        _chats.update { items }
    }

    private fun refreshSelectedMessages() {
        val topicName = _selectedTopic.value ?: run {
            _messages.update { emptyList() }
            return
        }

        val list = store.getMessages(topicName).map { msg ->
            UiMessage(
                topic = msg.topicName,
                from = msg.fromUid,
                ts = msg.messageTimestamp ?: Date(0),
                text = msg.messageContent.toString(),
                isMine = msg.isMine,
                seq = msg.messageSeqId,
                status = msg.messageStatus,
            )
        }.sortedWith(compareBy<UiMessage> { it.seq }.thenBy { it.ts.time })

        _messages.update { list }
    }

    private fun topicTitle(topic: Topic<*, *, *, *>): String {
        return if (topic.isP2PType) {
            val user: co.tinode.tinodesdk.User<TheCard>? = tinode.getUser(topic.name)
            val pub = user?.pub as? TheCard
            pub?.fn?.takeIf { it.isNotBlank() } ?: topic.name
        } else {
            val pub = topic.pub as? TheCard
            pub?.fn?.takeIf { it.isNotBlank() } ?: topic.name
        }
    }
}

data class ChatItem(
    val topicName: String,
    val title: String,
    val preview: String,
    val touched: Date?,
    val unread: Int,
    val pinnedRank: Int,
)

data class UiMessage(
    val topic: String,
    val from: String?,
    val ts: Date,
    val text: String,
    val isMine: Boolean,
    val seq: Int,
    val status: Int,
)

sealed class AuthState {
    data object LoggedOut : AuthState()
    data object LoggingIn : AuthState()
    data class LoggedIn(val uid: String, val token: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data class Connecting(val host: String, val tls: Boolean) : ConnectionState()
    data class Connected(val reason: String) : ConnectionState()
    data class Disconnected(val code: Int, val reason: String, val byServer: Boolean) : ConnectionState()
}
