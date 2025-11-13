package com.airline.loyalty.controller

import com.airline.loyalty.service.AiService
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Controller
class ChatController(private val aiService: AiService) {
    
    private val logger = LoggerFactory.getLogger(ChatController::class.java)
    
    companion object {
        private const val CONVERSATION_ID_KEY = "conversationId"
    }
    
    @GetMapping("/")
    fun index(model: Model, session: HttpSession): String {
        // Initialize conversation ID if not present
        if (session.getAttribute(CONVERSATION_ID_KEY) == null) {
            val conversationId = UUID.randomUUID().toString()
            session.setAttribute(CONVERSATION_ID_KEY, conversationId)
            logger.debug("Created new conversation ID: $conversationId")
        }
        
        val conversationId = session.getAttribute(CONVERSATION_ID_KEY) as String
        model.addAttribute("conversationId", conversationId)
        model.addAttribute("hasConversation", true)
        
        return "chat"
    }
    
    @PostMapping("/chat")
    fun chat(
        @RequestParam query: String,
        model: Model,
        session: HttpSession
    ): String {
        // Validate query is not blank
        if (query.isBlank()) {
            model.addAttribute("error", "Please enter a question.")
            return "chat"
        }
        
        // Get or create conversation ID
        var conversationId = session.getAttribute(CONVERSATION_ID_KEY) as? String
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString()
            session.setAttribute(CONVERSATION_ID_KEY, conversationId)
            logger.debug("Created new conversation ID: $conversationId")
        }
        
        // Add original query back to model to preserve user input
        model.addAttribute("query", query)
        model.addAttribute("conversationId", conversationId)
        model.addAttribute("hasConversation", true)
        
        try {
            // Call aiService.processQuery() with conversationId and add response to model
            val response = aiService.processQuery(query, conversationId)
            model.addAttribute("response", response)
        } catch (e: Exception) {
            // Log errors and add user-friendly error messages to model
            logger.error("Error processing query: ${query}", e)
            model.addAttribute("error", "Unable to process your request. Please try again later.")
        }
        
        return "chat"
    }
    
    @PostMapping("/new-conversation")
    fun newConversation(session: HttpSession): String {
        // Create a new conversation ID
        val conversationId = UUID.randomUUID().toString()
        session.setAttribute(CONVERSATION_ID_KEY, conversationId)
        logger.debug("Started new conversation with ID: $conversationId")
        
        // Redirect to home page
        return "redirect:/"
    }
}
