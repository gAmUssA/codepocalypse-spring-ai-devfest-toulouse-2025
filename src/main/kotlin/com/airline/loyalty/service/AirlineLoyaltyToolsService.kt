package com.airline.loyalty.service

import org.slf4j.LoggerFactory
import org.springframework.ai.reader.pdf.PagePdfDocumentReader
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig
import org.springframework.ai.reader.ExtractedTextFormatter
import org.springframework.ai.tool.annotation.Tool
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct

@Service
class AirlineLoyaltyToolsService {

    private val logger = LoggerFactory.getLogger(AirlineLoyaltyToolsService::class.java)
    
    private var deltaContent: String = ""
    private var unitedContent: String = ""

    @PostConstruct
    fun loadPdfContent() {
        logger.info("Loading PDF content for MCP tools...")
        
        try {
            // Load Delta PDF
            val deltaResource = ClassPathResource("pdf/How to Get Medallion Status _ Delta Air Lines.pdf")
            deltaContent = extractPdfContent(deltaResource)
            logger.info("Loaded Delta content: ${deltaContent.length} characters")
            
            // Load United PDF
            val unitedResource = ClassPathResource("pdf/How to Earn Premier Status _ United Airlines.pdf")
            unitedContent = extractPdfContent(unitedResource)
            logger.info("Loaded United content: ${unitedContent.length} characters")
            
            logger.info("Successfully loaded all PDF content for MCP tools")
        } catch (e: Exception) {
            logger.error("Error loading PDF content", e)
            throw e
        }
    }

    private fun extractPdfContent(resource: ClassPathResource): String {
        val config = PdfDocumentReaderConfig.builder()
            .withPageExtractedTextFormatter(
                ExtractedTextFormatter.builder()
                    .withNumberOfTopTextLinesToDelete(0)
                    .build()
            )
            .withPagesPerDocument(1)
            .build()
        
        val pdfReader = PagePdfDocumentReader(resource, config)
        val documents = pdfReader.get()
        
        return documents.joinToString("\n\n") { it.text ?: "" }
    }

    @Tool(description = "Fetches current Delta SkyMiles Medallion qualification requirements and status tier information")
    fun getDeltaMedallionQualification(): String {
        logger.info("MCP Tool called: getDeltaMedallionQualification")
        
        if (deltaContent.isEmpty()) {
            return "Delta SkyMiles Medallion qualification information is currently unavailable."
        }
        
        return """
            Delta SkyMiles Medallion Status Qualification Information:
            
            $deltaContent
            
            Source: https://www.delta.com/us/en/skymiles/medallion-program/qualify-for-status
        """.trimIndent()
    }

    @Tool(description = "Fetches current United MileagePlus Premier qualification requirements and status tier information")
    fun getUnitedPremierQualification(): String {
        logger.info("MCP Tool called: getUnitedPremierQualification")
        
        if (unitedContent.isEmpty()) {
            return "United MileagePlus Premier qualification information is currently unavailable."
        }
        
        return """
            United MileagePlus Premier Status Qualification Information:
            
            $unitedContent
            
            Source: https://www.united.com/en/us/fly/mileageplus/premier/qualify.html
        """.trimIndent()
    }

    @Tool(description = "Compares Delta SkyMiles Medallion and United MileagePlus Premier qualification requirements")
    fun compareAirlinePrograms(): String {
        logger.info("MCP Tool called: compareAirlinePrograms")
        
        if (deltaContent.isEmpty() || unitedContent.isEmpty()) {
            return "Airline program comparison information is currently unavailable."
        }
        
        return """
            Comparison of Delta SkyMiles Medallion and United MileagePlus Premier Programs:
            
            === DELTA SKYMILES MEDALLION ===
            $deltaContent
            
            === UNITED MILEAGEPLUS PREMIER ===
            $unitedContent
            
            Sources:
            - Delta: https://www.delta.com/us/en/skymiles/medallion-program/qualify-for-status
            - United: https://www.united.com/en/us/fly/mileageplus/premier/qualify.html
        """.trimIndent()
    }
}
