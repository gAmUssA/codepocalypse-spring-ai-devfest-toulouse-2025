package com.airline.loyalty.controller

import com.airline.loyalty.service.AiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class ChatController(private val aiService: AiService) {
    
    private val logger = LoggerFactory.getLogger(ChatController::class.java)
    
    @GetMapping("/")
    fun index(model: Model): String {
        return "chat"
    }
    
    @PostMapping("/chat")
    fun chat(
        @RequestParam query: String,
        model: Model
    ): String {
        // Validate query is not blank
        if (query.isBlank()) {
            model.addAttribute("error", "Please enter a question.")
            return "chat"
        }
        
        // Add original query back to model to preserve user input
        model.addAttribute("query", query)
        
        try {
            // Call aiService.processQuery() and add response to model
            val response = aiService.processQuery(query)
            model.addAttribute("response", response)
        } catch (e: Exception) {
            // Log errors and add user-friendly error messages to model
            logger.error("Error processing query: ${query}", e)
            model.addAttribute("error", "Unable to process your request. Please try again later.")
        }
        
        return "chat"
    }
}
