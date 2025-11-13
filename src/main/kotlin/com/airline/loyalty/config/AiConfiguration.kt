package com.airline.loyalty.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfiguration {
    
    @Bean
    fun chatMemory(): ChatMemory {
        return MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build()
    }
    
    @Bean
    fun chatClient(chatModel: ChatModel, chatMemory: ChatMemory): ChatClient {
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                You are a helpful airline loyalty program assistant specializing in Delta SkyMiles and United MileagePlus.
                You have access to tools that provide current qualification requirements from official airline sources.
                Use these tools when answering questions about:
                - Status qualification requirements (MQMs, MQDs, PQPs, PQFs, etc.)
                - Elite tier benefits
                - How to earn or maintain status
                
                Always use the tools to get accurate, up-to-date information rather than relying on potentially outdated knowledge.
                
                IMPORTANT: Remember user details shared during the conversation, including:
                - Names and personal information
                - Preferences and travel patterns
                - Previous questions and context
                - Any specific situations or goals they mention
                
                Use this context to provide personalized and contextual responses throughout the conversation.
            """.trimIndent())
            .defaultAdvisors(
                SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build()
    }
}
