package com.airline.loyalty.config

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse

class SimpleLoggerAdvisor : CallAdvisor {

    private val logger = LoggerFactory.getLogger(SimpleLoggerAdvisor::class.java)

    override fun getName(): String {
        return this::class.simpleName ?: "SimpleLoggerAdvisor"
    }

    override fun getOrder(): Int {
        return 0
    }

    override fun adviseCall(
        chatClientRequest: ChatClientRequest,
        callAdvisorChain: CallAdvisorChain
    ): ChatClientResponse {
        logRequest(chatClientRequest)
        val chatClientResponse = callAdvisorChain.nextCall(chatClientRequest)
        logResponse(chatClientResponse)
        return chatClientResponse
    }

    private fun logRequest(request: ChatClientRequest) {
        logger.debug("=== LLM Request ===")
        logger.debug("Request: {}", request)
    }

    private fun logResponse(chatClientResponse: ChatClientResponse) {
        logger.debug("=== LLM Response ===")
        logger.debug("Response: {}", chatClientResponse)
    }
}
