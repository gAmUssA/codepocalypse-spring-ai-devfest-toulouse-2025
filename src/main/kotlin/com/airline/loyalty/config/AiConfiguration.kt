package com.airline.loyalty.config

import com.airline.loyalty.service.AirlineLoyaltyToolsService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync
class AiConfiguration {
    
    @Bean
    fun chatMemory(): ChatMemory {
        return MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build()
    }
    
    @Bean
    fun airlineLoyaltyToolCallbackProvider(airlineLoyaltyToolsService: AirlineLoyaltyToolsService): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder()
            .toolObjects(airlineLoyaltyToolsService)
            .build()
    }
    
    @Bean
    fun chatClient(
        chatModel: ChatModel, 
        chatMemory: ChatMemory,
        airlineLoyaltyToolCallbackProvider: ToolCallbackProvider
    ): ChatClient {
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                You are a helpful airline loyalty program assistant specializing in Delta SkyMiles 
                and United MileagePlus programs.
                
                You have access to tools that provide detailed information about:
                - Delta SkyMiles Medallion qualification requirements
                - United MileagePlus Premier qualification requirements
                - Comparison between both programs
                
                Use these tools to answer questions accurately with specific details about qualification 
                requirements, benefits, and program rules.
                
                Always be helpful, accurate, and cite information from the tools when available.
            """.trimIndent())
            .defaultAdvisors(
                InputGuardrailAdvisor(chatModel),
                SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .defaultToolCallbacks(airlineLoyaltyToolCallbackProvider.getToolCallbacks().toList())
            .build()
    }
}
