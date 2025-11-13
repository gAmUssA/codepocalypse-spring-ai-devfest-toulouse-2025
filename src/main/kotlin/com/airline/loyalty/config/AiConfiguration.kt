package com.airline.loyalty.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync
class AiConfiguration {
    
    @Bean
    fun vectorStore(embeddingModel: EmbeddingModel): VectorStore {
        return SimpleVectorStore.builder(embeddingModel).build()
    }
    
    @Bean
    fun chatMemory(): ChatMemory {
        return MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build()
    }
    
    @Bean
    fun chatClient(chatModel: ChatModel, chatMemory: ChatMemory, vectorStore: VectorStore): ChatClient {
        return ChatClient.builder(chatModel)
            .defaultSystem("""
                You are a helpful airline loyalty program assistant specializing in Delta SkyMiles 
                and United MileagePlus programs.
                
                Use the provided context from official airline websites to answer questions accurately.
                If the information is in the context, provide specific details about qualification 
                requirements, benefits, and program rules.
                
                If the answer is not in the provided context, acknowledge this and provide general 
                guidance or suggest checking the official airline website.
                
                Always be helpful, accurate, and cite information from the context when available.
            """.trimIndent())
            .defaultAdvisors(
                SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(
                        SearchRequest.builder()
                            .topK(3)
                            .similarityThreshold(0.4)
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
