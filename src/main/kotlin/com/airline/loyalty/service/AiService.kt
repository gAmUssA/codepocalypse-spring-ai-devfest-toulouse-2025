package com.airline.loyalty.service

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.stereotype.Service

@Service
class AiService(private val chatClient: ChatClient) {

    private val logger = LoggerFactory.getLogger(AiService::class.java)

    fun processQuery(query: String, conversationId: String): String {
        return try {
            logger.debug("Processing query: $query with conversationId: $conversationId")
            val result = chatClient.prompt()
                .user(query)
                .advisors { a -> a.param(ChatMemory.CONVERSATION_ID, conversationId) }
                .call()
                .content() ?: ""
            logger.debug("Received response from AI model")
            result
        } catch (e: Exception) {
            logger.error("Error processing query with AI model", e)
            throw e
        }
    }
}
