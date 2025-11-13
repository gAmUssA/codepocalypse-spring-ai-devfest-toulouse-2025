package com.airline.loyalty.controller

import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/debug")
class VectorStoreDebugController(private val vectorStore: VectorStore) {
    
    private val logger = LoggerFactory.getLogger(VectorStoreDebugController::class.java)
    
    @GetMapping
    fun debugPage(model: Model): String {
        logger.debug("Accessing vector store debug page")
        return "debug"
    }
    
    @PostMapping("/search")
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "5") topK: Int,
        @RequestParam(defaultValue = "0.0") threshold: Double,
        model: Model
    ): String {
        logger.debug("Debug search: query='$query', topK=$topK, threshold=$threshold")
        
        try {
            val searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(threshold)
                .build()
            
            val results = vectorStore.similaritySearch(searchRequest)
            
            val enrichedResults = results.map { doc ->
                DocumentResult(
                    content = doc.text?.take(500) ?: "No content",
                    metadata = doc.metadata.mapValues { it.value?.toString() ?: "N/A" },
                    score = (doc.metadata["distance"] as? Number)?.toString() ?: "N/A"
                )
            }
            
            model.addAttribute("query", query)
            model.addAttribute("topK", topK)
            model.addAttribute("threshold", threshold)
            model.addAttribute("results", enrichedResults)
            model.addAttribute("resultCount", results.size)
            
            logger.info("Found ${results.size} documents for query: $query")
        } catch (e: Exception) {
            logger.error("Error searching vector store", e)
            model.addAttribute("error", "Error searching: ${e.message}")
        }
        
        return "debug"
    }
    
    @PostMapping("/stats")
    fun getStats(model: Model): String {
        logger.debug("Fetching vector store statistics")
        
        try {
            // Perform a broad search to estimate document count
            val searchRequest = SearchRequest.builder()
                .query("airline loyalty program status qualification")
                .topK(100)
                .similarityThreshold(0.0)
                .build()
            
            val allDocs = vectorStore.similaritySearch(searchRequest)
            
            // Collect statistics
            val sources = allDocs.mapNotNull { it.metadata["source"] as? String }.distinct()
            val titles = allDocs.mapNotNull { it.metadata["title"] as? String }.distinct()
            
            model.addAttribute("totalDocuments", allDocs.size)
            model.addAttribute("uniqueSources", sources.size)
            model.addAttribute("sources", sources)
            model.addAttribute("titles", titles)
            model.addAttribute("showStats", true)
            
            logger.info("Vector store stats: ${allDocs.size} documents, ${sources.size} sources")
        } catch (e: Exception) {
            logger.error("Error fetching stats", e)
            model.addAttribute("error", "Error fetching stats: ${e.message}")
        }
        
        return "debug"
    }
    
    data class DocumentResult(
        val content: String,
        val metadata: Map<String, String>,
        val score: String
    )
}
