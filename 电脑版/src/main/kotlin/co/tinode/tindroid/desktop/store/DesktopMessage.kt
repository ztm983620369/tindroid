package co.tinode.tindroid.desktop.store

import co.tinode.tinodesdk.Storage
import co.tinode.tinodesdk.model.Drafty
import java.util.Date

class DesktopMessage(
    val messageDbId: Long,
    val topicName: String,
    val fromUid: String?,
    var messageTimestamp: Date?,
    var messageSeqId: Int,
    var messageStatus: Int,
    var messageHead: MutableMap<String, Any?>?,
    var messageContent: Drafty,
    private val mine: Boolean,
) : Storage.Message {
    override fun getTopic(): String = topicName
    override fun getHead(): MutableMap<String, Any?>? = messageHead
    override fun getHeader(key: String): Any? = messageHead?.get(key)
    override fun getStringHeader(key: String): String? = getHeader(key) as? String
    override fun getIntHeader(key: String): Int? = getHeader(key) as? Int
    override fun getContent(): Drafty = messageContent
    override fun setContent(content: Drafty) {
        messageContent = content
    }
    override fun getDbId(): Long = messageDbId
    override fun getSeqId(): Int = messageSeqId
    override fun getStatus(): Int = messageStatus

    override fun isMine(): Boolean = mine
    override fun isPending(): Boolean = messageStatus < STATUS_SYNCED
    override fun isReady(): Boolean = messageStatus == STATUS_QUEUED
    override fun isDeleted(): Boolean = messageStatus == STATUS_DELETED_HARD || messageStatus == STATUS_DELETED_SOFT
    override fun isDeleted(hard: Boolean): Boolean =
        if (hard) messageStatus == STATUS_DELETED_HARD else messageStatus == STATUS_DELETED_SOFT
    override fun isSynced(): Boolean = messageStatus == STATUS_SYNCED

    companion object {
        const val STATUS_DRAFT = 10
        const val STATUS_QUEUED = 20
        const val STATUS_SENDING = 30
        const val STATUS_FAILED = 40
        const val STATUS_SYNCED = 50
        const val STATUS_DELETED_HARD = 60
        const val STATUS_DELETED_SOFT = 70
    }
}
