package com.airline.loyalty.service

import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.reader.pdf.PagePdfDocumentReader
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig
import org.springframework.ai.reader.ExtractedTextFormatter
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class DocumentLoaderService {

    private val logger = LoggerFactory.getLogger(DocumentLoaderService::class.java)

    @Autowired
    private lateinit var vectorStore: VectorStore

    private val pdfDocuments = listOf(
        PdfDocument(
            "pdf/How to Get Medallion Status _ Delta Air Lines.pdf",
            "https://www.delta.com/us/en/skymiles/medallion-program/qualify-for-status",
            "Delta SkyMiles Medallion Status Qualification"
        ),
        PdfDocument(
            "pdf/How to Earn Premier Status _ United Airlines.pdf",
            "https://www.united.com/en/us/fly/mileageplus/premier/qualify.html",
            "United MileagePlus Premier Status Qualification"
        )
    )

    @EventListener(ApplicationReadyEvent::class)
    fun loadDocumentsOnStartup() {
        logger.info("Starting to load PDF documents into vector store...")
        
        val futures = pdfDocuments.map { pdfDoc ->
            loadDocumentAsync(pdfDoc)
        }
        
        // Wait for all documents to be loaded
        CompletableFuture.allOf(*futures.toTypedArray()).thenRun {
            logger.info("All PDF documents loaded successfully into vector store")
        }.exceptionally { ex ->
            logger.error("Error loading documents", ex)
            null
        }
    }

    @Async
    fun loadDocumentAsync(pdfDocument: PdfDocument): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            try {
                logger.info("Loading document: ${pdfDocument.title}")
                
                val resource = ClassPathResource(pdfDocument.path)
                
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
                
                // Add metadata to documents
                documents.forEach { doc ->
                    doc.metadata["source"] = pdfDocument.sourceUrl
                    doc.metadata["title"] = pdfDocument.title
                }
                
                // Split documents into smaller chunks for better retrieval
                val textSplitter = TokenTextSplitter()
                val splitDocuments = textSplitter.apply(documents)
                
                logger.info("Loaded ${documents.size} pages from ${pdfDocument.title}, split into ${splitDocuments.size} chunks")
                
                // Add to vector store
                vectorStore.add(splitDocuments)
                
                logger.info("Successfully added ${pdfDocument.title} to vector store")
            } catch (e: Exception) {
                logger.error("Error loading document: ${pdfDocument.title}", e)
                throw e
            }
        }
    }

    data class PdfDocument(
        val path: String,
        val sourceUrl: String,
        val title: String
    )
}
