package com.example.alpha_chat_native.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.alpha_chat_native.data.local.dao.MessageDao
import com.example.alpha_chat_native.data.models.SendMessageRequest
import com.example.alpha_chat_native.data.remote.AlphaChatApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

import com.example.alpha_chat_native.data.repository.ChatRepository
import com.example.alpha_chat_native.data.models.Message

@HiltWorker
class MessageSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageDao: MessageDao,
    private val api: AlphaChatApi,
    private val chatRepository: ChatRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pendingMessages = messageDao.getPendingMessages()
            if (pendingMessages.isEmpty()) {
                return@withContext Result.success()
            }

            var hasFailures = false

            for (msgItem in pendingMessages) {
                val request = SendMessageRequest(
                    content = msgItem.content,
                    messageType = msgItem.messageType,
                    codeLanguage = msgItem.codeLanguage
                )

                // Determine if it was a channel message or direct message
                // In AlphaChat, DM uses 'receiver', channel uses 'conversation' but wait - 
                // Wait, MessageEntity has receiver and conversation fields.
                // If receiver is empty, it's a channel message. If conversation is empty, but receiver is not, it's DM.
                // Let's check repository implementation to be sure.
                val isChannel = msgItem.receiver.isEmpty()

                try {
                    if (isChannel) {
                        val response = api.sendChannelMessage(msgItem.conversation, request)
                        if (response.success) {
                            val newId = response.message?.id ?: msgItem.id
                            messageDao.updateSyncStatus(oldId = msgItem.id, newId = newId, status = "SENT")
                            
                            if (response.message != null) {
                                val msg = Message(
                                    id = newId,
                                    sender = response.message.sender,
                                    receiver = "",
                                    conversation = msgItem.conversation,
                                    content = msgItem.content,
                                    messageType = msgItem.messageType,
                                    syncStatus = "SENT",
                                    createdAt = response.message.createdAt
                                )
                                chatRepository.notifyMessageSynced(msgItem.id, msg)
                            }
                            Timber.d("Successfully synced offline channel message: \${msgItem.id}")
                        } else {
                            hasFailures = true
                            Timber.w("Failed to sync channel message: \${msgItem.id}")
                            break // Stop to preserve chronological order
                        }
                    } else {
                        val response = api.sendDirectMessage(msgItem.receiver, request)
                        if (response.success) {
                            val newId = response.messageData?.id ?: msgItem.id
                            messageDao.updateSyncStatus(oldId = msgItem.id, newId = newId, status = "SENT")
                            
                            if (response.messageData != null) {
                                chatRepository.notifyMessageSynced(msgItem.id, response.messageData)
                            }
                            Timber.d("Successfully synced offline DM: \${msgItem.id}")
                        } else {
                            hasFailures = true
                            Timber.w("Failed to sync DM: \${msgItem.id}")
                            break // Stop to preserve chronological order
                        }
                    }
                } catch (e: Exception) {
                    hasFailures = true
                    Timber.e(e, "Exception syncing message: \${msgItem.id}")
                    break // Network error, stop entirely
                }
            }

            if (hasFailures) {
                return@withContext Result.retry()
            } else {
                return@withContext Result.success()
            }
        } catch (e: Exception) {
            Timber.e(e, "Offline sync worker failed")
            return@withContext Result.retry()
        }
    }
}
