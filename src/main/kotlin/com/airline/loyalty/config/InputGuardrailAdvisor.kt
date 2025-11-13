package com.airline.loyalty.config

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.advisor.api.CallAdvisor
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.core.Ordered

class InputGuardrailAdvisor(private val chatModel: ChatModel) : CallAdvisor {

    private val logger = LoggerFactory.getLogger(InputGuardrailAdvisor::class.java)

    companion object {
        private const val VALIDATION_SYSTEM_PROMPT = """
            You are a content validator. Your task is to determine if a user's question 
            is related to airline loyalty programs.
            
            Airline loyalty topics include:
            - Frequent flyer programs (Delta SkyMiles, United MileagePlus, etc.)
            - Status tiers and qualification requirements
            - Earning and redeeming miles or points
            - Elite benefits and perks
            - Award flights and upgrades
            - Airline alliances and partnerships
            
            Respond with ONLY "YES" if the question is about airline loyalty programs.
            Respond with ONLY "NO" if it is not.
            
            Do not provide any explanation, just YES or NO.
        """

        private const val REJECTION_MESSAGE = """
            I'm specialized in airline loyalty programs. 
            Please ask questions about frequent flyer programs, 
            status tiers, miles, or airline rewards.
        """
    }

    override fun getName(): String {
        return "InputGuardrailAdvisor"
    }

    override fun getOrder(): Int {
        return Ordered.HIGHEST_PRECEDENCE
    }

    override fun adviseCall(
        chatClientRequest: ChatClientRequest,
        callAdvisorChain: CallAdvisorChain
    ): ChatClientResponse {
        // Extract user message from the request
        val userMessage = chatClientRequest.prompt().getUserMessage()?.getText() ?: ""
        
        logger.info("Guardrail: Validating query: $userMessage")
        
        // Validate the query
        val isValid = validateQuery(userMessage)
        
        if (!isValid) {
            logger.warn("Guardrail: ✗ Query rejected - not related to airline loyalty programs")
            // Throw exception with rejection message
            throw IllegalArgumentException(REJECTION_MESSAGE.trimIndent())
        }
        
        logger.info("Guardrail: ✓ Query passed validation")
        
        // Continue with the chain if validation passes
        return callAdvisorChain.nextCall(chatClientRequest)
    }

    private fun validateQuery(userMessage: String): Boolean {
        return try {
            // Create validation prompt with temperature 0 for deterministic results
            val validationPrompt = Prompt(
                "$VALIDATION_SYSTEM_PROMPT\n\nUser question: $userMessage",
                OllamaOptions.builder()
                    .temperature(0.0)
                    .build()
            )
            
            // Call the model for validation
            val response = chatModel.call(validationPrompt)
            val validationResult = response.results[0].output.text?.trim()?.uppercase() ?: "NO"
            
            logger.debug("Guardrail: Validation result: $validationResult")
            
            // Return true if the response is YES
            validationResult == "YES"
        } catch (e: Exception) {
            // Fail open: if validation fails, allow the query to proceed
            logger.error("Guardrail: Error during validation, failing open", e)
            true
        }
    }
}
