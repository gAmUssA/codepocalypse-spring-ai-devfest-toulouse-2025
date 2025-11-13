package com.airline.loyalty.service

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

@Service
class AiService(chatModel: ChatModel) {

    private val logger = LoggerFactory.getLogger(AiService::class.java)

    private val chatClient: ChatClient = ChatClient.builder(chatModel)
        .defaultSystem("""
            You are a helpful airline loyalty program assistant specializing in Delta SkyMiles and United MileagePlus.
            You have access to tools that provide current qualification requirements from official airline sources.
            Use these tools when answering questions about:
            - Status qualification requirements (MQMs, MQDs, PQPs, PQFs, etc.)
            - Elite tier benefits
            - How to earn or maintain status
            
            Always use the tools to get accurate, up-to-date information rather than relying on potentially outdated knowledge.
        """.trimIndent())
        .build()

    fun processQuery(query: String): String {
        return try {
            logger.debug("Processing query: $query")
            val result = chatClient.prompt()
                .user(query)
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
