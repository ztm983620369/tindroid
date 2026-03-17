package co.tinode.tindroid.desktop.store

import co.tinode.tinodesdk.Storage
import co.tinode.tinodesdk.LocalData
import co.tinode.tinodesdk.Tinode
import co.tinode.tinodesdk.Topic
import co.tinode.tinodesdk.User
import co.tinode.tinodesdk.model.Drafty
import co.tinode.tinodesdk.model.MsgRange
import co.tinode.tinodesdk.model.MsgServerData
import co.tinode.tinodesdk.model.Subscription
import java.io.Closeable
import java.util.Collections
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Minimal JVM-friendly in-memory Storage implementation.
 *
 * Goal: keep Tinode link complete (subscribe -> receive meta/data -> send/ack) with zero Android deps.
 */
class DesktopStore : Storage {
    private val idGen = AtomicLong(1)

    @Volatile
    private var myUid: String? = null

    @Volatile
    private var hostUri: String? = null

    @Volatile
    private var deviceToken: String? = null

    private val topics = ConcurrentHashMap<String, Topic<*, *, *, *>>()
    private val users = ConcurrentHashMap<String, User<*>>()
    private val messages = ConcurrentHashMap<String, MutableList<DesktopMessage>>()

    override fun getMyUid(): String? = myUid

    override fun setMyUid(uid: String?, hostURI: String?) {
        myUid = uid
        hostUri = hostURI
    }

    override fun updateCredentials(credRequired: Array<out String>?) {
        // Not implemented for desktop MVP.
    }

    override fun deleteAccount(uid: String?) {
        if (uid != null && uid == myUid) {
            logout()
        }
    }

    override fun getServerURI(): String? = hostUri

    override fun getDeviceToken(): String? = deviceToken

    override fun saveDeviceToken(token: String?) {
        deviceToken = token
    }

    override fun logout() {
        topics.clear()
        users.clear()
        messages.clear()
        myUid = null
        hostUri = null
        deviceToken = null
    }

    override fun setTimeAdjustment(adjustment: Long) {
        // Not persisted.
    }

    override fun isReady(): Boolean = true

    override fun topicGetAll(tinode: Tinode?): Array<Topic<*, *, *, *>> {
        return emptyArray()
    }

    override fun topicGet(tinode: Tinode?, name: String?): Topic<*, *, *, *>? {
        if (name == null) return null
        return topics[name]
    }

    override fun topicAdd(topic: Topic<*, *, *, *>?): Long {
        if (topic == null) return -1
        topics[topic.name()] = topic
        if (topic.getLocal() == null) {
            topic.setLocal(SimplePayload(idGen.getAndIncrement()))
        }
        return (topic.getLocal() as? SimplePayload)?.id ?: -1
    }

    override fun topicUpdate(topic: Topic<*, *, *, *>?): Boolean {
        if (topic == null) return false
        topics[topic.name()] = topic
        if (topic.getLocal() == null) {
            topic.setLocal(SimplePayload(idGen.getAndIncrement()))
        }
        return true
    }

    override fun topicDelete(topic: Topic<*, *, *, *>?, hard: Boolean): Boolean {
        if (topic == null) return false
        topics.remove(topic.name())
        messages.remove(topic.name())
        return true
    }

    override fun subAdd(topic: Topic<*, *, *, *>?, sub: Subscription<*, *>?): Long = idGen.getAndIncrement()
    override fun subUpdate(topic: Topic<*, *, *, *>?, sub: Subscription<*, *>?): Boolean = true
    override fun subNew(topic: Topic<*, *, *, *>?, sub: Subscription<*, *>?): Long = idGen.getAndIncrement()
    override fun subDelete(topic: Topic<*, *, *, *>?, sub: Subscription<*, *>?): Boolean = true

    override fun getSubscriptions(topic: Topic<*, *, *, *>?): MutableCollection<Subscription<*, *>> {
        return mutableListOf()
    }

    override fun userGet(uid: String?): User<*>? {
        if (uid == null) return null
        return users[uid]
    }

    override fun userAdd(user: User<*>?): Long {
        if (user == null || user.uid == null) return -1
        users[user.uid] = user
        if (user.local == null) {
            user.local = SimplePayload(idGen.getAndIncrement())
        }
        return (user.local as? SimplePayload)?.id ?: -1
    }

    override fun userUpdate(user: User<*>?): Boolean {
        if (user == null || user.uid == null) return false
        users[user.uid] = user
        if (user.local == null) {
            user.local = SimplePayload(idGen.getAndIncrement())
        }
        return true
    }

    override fun msgReceived(topic: Topic<*, *, *, *>?, sub: Subscription<*, *>?, msg: MsgServerData?): Storage.Message? {
        if (topic == null || msg == null) return null

        val dbId = idGen.getAndIncrement()
        val mine = myUid != null && myUid == msg.from
        val head = when (val raw = msg.head) {
            is MutableMap<*, *> -> raw as? MutableMap<String, Any?>
            is Map<*, *> -> (raw as? Map<String, Any?>)?.toMutableMap()
            else -> null
        }
        val stored = DesktopMessage(
            messageDbId = dbId,
            topicName = msg.topic,
            fromUid = msg.from,
            messageTimestamp = msg.ts,
            messageSeqId = msg.seq,
            messageStatus = DesktopMessage.STATUS_SYNCED,
            messageHead = head,
            messageContent = msg.content ?: Drafty(),
            mine = mine,
        )
        addMessage(stored)
        return stored
    }

    override fun msgSend(topic: Topic<*, *, *, *>?, data: Drafty, head: MutableMap<String, Any?>?): Storage.Message {
        val t = topic ?: throw IllegalArgumentException("topic is null")
        val dbId = idGen.getAndIncrement()
        val stored = DesktopMessage(
            messageDbId = dbId,
            topicName = t.name(),
            fromUid = myUid,
            messageTimestamp = Date(),
            messageSeqId = 0,
            messageStatus = DesktopMessage.STATUS_QUEUED,
            messageHead = head,
            messageContent = data,
            mine = true,
        )
        addMessage(stored)
        return stored
    }

    override fun msgDraft(topic: Topic<*, *, *, *>?, data: Drafty, head: MutableMap<String, Any?>?): Storage.Message {
        val t = topic ?: throw IllegalArgumentException("topic is null")
        val dbId = idGen.getAndIncrement()
        val stored = DesktopMessage(
            messageDbId = dbId,
            topicName = t.name(),
            fromUid = myUid,
            messageTimestamp = Date(),
            messageSeqId = 0,
            messageStatus = DesktopMessage.STATUS_DRAFT,
            messageHead = head,
            messageContent = data,
            mine = true,
        )
        addMessage(stored)
        return stored
    }

    override fun msgDraftUpdate(topic: Topic<*, *, *, *>?, dbMessageId: Long, data: Drafty): Boolean =
        updateMessage(dbMessageId) { it.messageContent = data; true }

    override fun msgReady(topic: Topic<*, *, *, *>?, dbMessageId: Long, data: Drafty?): Boolean =
        updateMessage(dbMessageId) {
            if (data != null) {
                it.messageContent = data
            }
            it.messageStatus = DesktopMessage.STATUS_QUEUED
            true
        }

    override fun msgSyncing(topic: Topic<*, *, *, *>?, dbMessageId: Long, sync: Boolean): Boolean =
        updateMessage(dbMessageId) {
            it.messageStatus = if (sync) DesktopMessage.STATUS_SENDING else DesktopMessage.STATUS_FAILED
            true
        }

    override fun msgFailed(topic: Topic<*, *, *, *>?, dbMessageId: Long): Boolean =
        updateMessage(dbMessageId) { it.messageStatus = DesktopMessage.STATUS_FAILED; true }

    override fun msgPruneFailed(topic: Topic<*, *, *, *>?): Boolean = true

    override fun msgDiscard(topic: Topic<*, *, *, *>?, dbMessageId: Long): Boolean {
        val t = topic ?: return false
        val list = messages[t.name()] ?: return false
        synchronized(list) {
            list.removeIf { it.messageDbId == dbMessageId }
        }
        return true
    }

    override fun msgDiscardSeq(topic: Topic<*, *, *, *>?, seq: Int): Boolean {
        val t = topic ?: return false
        val list = messages[t.name()] ?: return false
        synchronized(list) {
            list.removeIf { it.messageSeqId == seq }
        }
        return true
    }

    override fun msgDelivered(topic: Topic<*, *, *, *>?, dbMessageId: Long, timestamp: Date?, seq: Int): Boolean =
        updateMessage(dbMessageId) {
            it.messageTimestamp = timestamp
            it.messageSeqId = seq
            it.messageStatus = DesktopMessage.STATUS_SYNCED
            true
        }

    override fun msgMarkToDelete(topic: Topic<*, *, *, *>?, fromId: Int, toId: Int, markAsHard: Boolean): Boolean = true
    override fun msgMarkToDelete(topic: Topic<*, *, *, *>?, ranges: Array<out MsgRange>?, markAsHard: Boolean): Boolean = true
    override fun msgDelete(topic: Topic<*, *, *, *>?, delId: Int, fromId: Int, toId: Int): Boolean = true
    override fun msgDelete(topic: Topic<*, *, *, *>?, delId: Int, ranges: Array<out MsgRange>?): Boolean = true
    override fun msgRecvByRemote(sub: Subscription<*, *>?, recv: Int): Boolean = true
    override fun msgReadByRemote(sub: Subscription<*, *>?, read: Int): Boolean = true

    override fun msgIsCached(topic: Topic<*, *, *, *>?, ranges: Array<out MsgRange>?): Array<MsgRange>? = null

    override fun getCachedMessagesRange(topic: Topic<*, *, *, *>?): MsgRange? {
        val t = topic ?: return null
        val list = messages[t.name()] ?: return null
        var min = Int.MAX_VALUE
        var max = 0
        synchronized(list) {
            for (m in list) {
                if (m.messageSeqId <= 0) continue
                if (m.messageSeqId < min) min = m.messageSeqId
                if (m.messageSeqId > max) max = m.messageSeqId
            }
        }
        if (max <= 0) return null
        if (min == Int.MAX_VALUE) min = 1
        // inclusive-exclusive [low, hi)
        return MsgRange(min, max + 1)
    }

    override fun getMissingRanges(topic: Topic<*, *, *, *>?, startFrom: Int, pageSize: Int, newer: Boolean): Array<MsgRange> =
        emptyArray()

    override fun setRead(topic: Topic<*, *, *, *>?, read: Int): Boolean = true
    override fun setRecv(topic: Topic<*, *, *, *>?, recv: Int): Boolean = true

    override fun <T : Storage.Message?> getMessageById(dbMessageId: Long): T? {
        val msg = findById(dbMessageId) ?: return null
        @Suppress("UNCHECKED_CAST")
        return msg as T
    }

    override fun <T : Storage.Message?> getMessagePreviewById(dbMessageId: Long): T? = getMessageById(dbMessageId)

    override fun getAllMsgVersions(topic: Topic<*, *, *, *>?, seq: Int, limit: Int): IntArray = intArrayOf()

    override fun <T> getLatestMessagePreviews(): T where T : MutableIterator<Storage.Message>, T : Closeable {
        val previews = mutableListOf<Storage.Message>()
        for ((_, list) in messages) {
            synchronized(list) {
                list.maxByOrNull { it.messageTimestamp?.time ?: 0L }?.let { previews.add(it) }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return CloseableIterator(previews.iterator()) as T
    }

    override fun <T> getQueuedMessages(topic: Topic<*, *, *, *>?): T where T : MutableIterator<Storage.Message>, T : Closeable {
        val t = topic ?: run {
            @Suppress("UNCHECKED_CAST")
            return CloseableIterator(mutableListOf<Storage.Message>().iterator()) as T
        }
        val list = messages[t.name()] ?: mutableListOf()
        val queued = mutableListOf<Storage.Message>()
        synchronized(list) {
            for (m in list) {
                if (m.messageStatus == DesktopMessage.STATUS_QUEUED || m.messageStatus == DesktopMessage.STATUS_SENDING) {
                    queued.add(m)
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return CloseableIterator(queued.iterator()) as T
    }

    override fun getQueuedMessageDeletes(topic: Topic<*, *, *, *>?, hard: Boolean): Array<MsgRange> = emptyArray()

    override fun <T : Storage.Message?> getMessageBySeq(topic: Topic<*, *, *, *>?, seq: Int): T? {
        val t = topic ?: return null
        val list = messages[t.name()] ?: return null
        val found = synchronized(list) { list.firstOrNull { it.messageSeqId == seq } } ?: return null
        @Suppress("UNCHECKED_CAST")
        return found as T
    }

    fun getMessages(topicName: String): List<DesktopMessage> {
        val list = messages[topicName] ?: return emptyList()
        synchronized(list) {
            return list.toList()
        }
    }

    private fun addMessage(msg: DesktopMessage) {
        val list = messages.computeIfAbsent(msg.topicName) { Collections.synchronizedList(mutableListOf()) }
        synchronized(list) {
            list.add(msg)
        }
    }

    private fun updateMessage(dbMessageId: Long, updater: (DesktopMessage) -> Boolean): Boolean {
        val msg = findById(dbMessageId) ?: return false
        return updater(msg)
    }

    private fun findById(dbMessageId: Long): DesktopMessage? {
        for ((_, list) in messages) {
            synchronized(list) {
                val found = list.firstOrNull { it.messageDbId == dbMessageId }
                if (found != null) return found
            }
        }
        return null
    }

    private fun Topic<*, *, *, *>.name(): String = this.name

}

private data class SimplePayload(val id: Long) : LocalData.Payload

private class CloseableIterator<T>(private val it: MutableIterator<T>) : MutableIterator<T>, Closeable {
    override fun hasNext(): Boolean = it.hasNext()
    override fun next(): T = it.next()
    override fun remove() = it.remove()
    override fun close() {}
}
