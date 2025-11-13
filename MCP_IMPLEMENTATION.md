# MCP Server Implementation - Airline Loyalty Assistant

## Overview

This implementation replaces the RAG (Retrieval-Augmented Generation) approach with a Model Context Protocol (MCP) server using Spring AI. The MCP server provides airline qualification information through tools that the AI assistant can call.

## Architecture Changes

### Before (RAG-based)
- PDF documents loaded into VectorStore
- QuestionAnswerAdvisor retrieved relevant chunks
- EmbeddingModel used for similarity search
- DocumentLoaderService managed document loading

### After (MCP-based)
- PDF documents loaded into memory by AirlineLoyaltyToolsService
- Three MCP tools expose airline information
- AI model directly calls tools when needed
- Simpler, more deterministic approach

## Implementation Details

### 1. MCP Tools Service (`AirlineLoyaltyToolsService.kt`)

Three tools are exposed via `@Tool` annotations:

#### `getDeltaMedallionQualification()`
- **Description**: Fetches current Delta SkyMiles Medallion qualification requirements and status tier information
- **Returns**: Complete Delta qualification information from PDF

#### `getUnitedPremierQualification()`
- **Description**: Fetches current United MileagePlus Premier qualification requirements and status tier information
- **Returns**: Complete United qualification information from PDF

#### `compareAirlinePrograms()`
- **Description**: Compares Delta SkyMiles Medallion and United MileagePlus Premier qualification requirements
- **Returns**: Side-by-side comparison of both programs

### 2. Configuration Changes

#### `build.gradle.kts`
- Removed: `spring-ai-advisors-vector-store`
- Added: `spring-ai-starter-mcp-server-webmvc`

#### `application.yml`
- Added MCP server configuration:
  ```yaml
  spring:
    ai:
      mcp:
        server:
          protocol: STREAMABLE
  ```
- Updated model from `gpt-5` to `gpt-4o`
- Removed embedding configuration

#### `AiConfiguration.kt`
- Removed: VectorStore bean, QuestionAnswerAdvisor
- Added: ToolCallbackProvider bean using MethodToolCallbackProvider
- Updated: ChatClient explicitly registers tools via `.defaultToolCallbacks()`
- Updated system prompt to mention available tools

**Key Implementation Details**:
```kotlin
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
        .defaultToolCallbacks(airlineLoyaltyToolCallbackProvider.getToolCallbacks().toList())
        .build()
}
```

**Important**: Tools annotated with `@Tool` must be explicitly registered with ChatClient using `MethodToolCallbackProvider` and `.defaultToolCallbacks()`. They are not automatically discovered by ChatClient.

### 3. Removed Files
- `DocumentLoaderService.kt` - Replaced by AirlineLoyaltyToolsService
- `VectorStoreDebugController.kt` - No longer needed without VectorStore

## How It Works

1. **Startup**: AirlineLoyaltyToolsService loads PDF content into memory using `@PostConstruct`
2. **Tool Discovery**: Spring AI MCP auto-configuration discovers `@Tool` annotated methods
3. **User Query**: User asks a question through the web interface
4. **AI Decision**: The AI model decides which tool(s) to call based on the query
5. **Tool Execution**: MCP server executes the appropriate tool method
6. **Response**: AI model uses tool results to formulate a response

## Testing

### Example Test Queries

1. **Delta-specific query**:
   - "What are the requirements to earn Delta Silver Medallion status?"
   - Expected: AI calls `getDeltaMedallionQualification()` tool

2. **United-specific query**:
   - "How many PQPs do I need for United Gold?"
   - Expected: AI calls `getUnitedPremierQualification()` tool

3. **Comparison query**:
   - "Compare the qualification requirements between Delta and United"
   - Expected: AI calls `compareAirlinePrograms()` tool

4. **Terminology query**:
   - "What's the difference between MQMs and PQFs?"
   - Expected: AI may call Delta tool to explain terminology

5. **Credit card query**:
   - "Can I use a credit card to help qualify for status?"
   - Expected: AI calls relevant tool(s) to find credit card information

### Running the Application

```bash
# Set OpenAI API key
export SPRING_AI_OPENAI_API_KEY=your-api-key-here

# Build the application
./gradlew clean build

# Run the application
./gradlew bootRun

# Access the web interface
open http://localhost:8080
```

### Monitoring Tool Usage

Tool calls are logged in the application logs:
```
INFO  c.a.l.s.AirlineLoyaltyToolsService - MCP Tool called: getDeltaMedallionQualification
INFO  c.a.l.s.AirlineLoyaltyToolsService - MCP Tool called: getUnitedPremierQualification
INFO  c.a.l.s.AirlineLoyaltyToolsService - MCP Tool called: compareAirlinePrograms
```

## Benefits of MCP Approach

1. **Deterministic**: Tools always return complete, consistent information
2. **Simpler**: No embedding models or vector stores needed
3. **Transparent**: Easy to see which tools are called via logging
4. **Maintainable**: Tool logic is straightforward and testable
5. **Efficient**: No similarity search overhead

## MCP Server Endpoints

The MCP server is automatically exposed by Spring AI at:
- Protocol: STREAMABLE (HTTP-based)
- Auto-configured by `spring-ai-starter-mcp-server-webmvc`

## Future Enhancements

1. Add more granular tools (e.g., specific tier information)
2. Implement caching for tool results
3. Add tool parameter support for filtering
4. Create UI indicators showing which tools were used
5. Add metrics for tool usage tracking
