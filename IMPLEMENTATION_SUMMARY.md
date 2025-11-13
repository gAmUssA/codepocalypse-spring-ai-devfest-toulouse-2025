# Implementation Summary: Local Mistral Model with Input Guardrails

## Overview
This implementation configures a local Mistral model (qwen3-coder:30b) running via Ollama and implements input guardrails to restrict queries to airline loyalty topics only.

## Changes Made

### 1. Configuration Changes (`application.yml`)
**File**: `src/main/resources/application.yml`

Updated Spring AI configuration to use local Ollama server:
- **api-key**: Set to "not-needed" (placeholder for local model)
- **base-url**: Set to `http://localhost:11434/v1` (Ollama OpenAI-compatible endpoint)
- **model**: Changed from `gpt-5` to `qwen3-coder:30b`
- **temperature**: Kept at 1 for normal responses

```yaml
spring:
  ai:
    openai:
      api-key: not-needed
      base-url: http://localhost:11434/v1
      chat:
        options:
          model: qwen3-coder:30b
          temperature: 1
```

### 2. Input Guardrail Advisor (`InputGuardrailAdvisor.kt`)
**File**: `src/main/kotlin/com/airline/loyalty/config/InputGuardrailAdvisor.kt`

Created a new advisor that implements input validation:

**Key Features**:
- **Order**: `Ordered.HIGHEST_PRECEDENCE` - runs before all other advisors
- **Validation**: Uses LLM to determine if query is airline loyalty related
- **Temperature**: Uses 0.0 for validation to ensure deterministic results
- **Fail-Safe**: Fails open (allows query) if validation encounters errors
- **Rejection**: Throws `IllegalArgumentException` with user-friendly message

**Validation Topics**:
- Frequent flyer programs (Delta SkyMiles, United MileagePlus, etc.)
- Status tiers and qualification requirements
- Earning and redeeming miles or points
- Elite benefits and perks
- Award flights and upgrades
- Airline alliances and partnerships

**Rejection Message**:
```
I'm specialized in airline loyalty programs. 
Please ask questions about frequent flyer programs, 
status tiers, miles, or airline rewards.
```

### 3. Advisor Registration (`AiConfiguration.kt`)
**File**: `src/main/kotlin/com/airline/loyalty/config/AiConfiguration.kt`

Registered the `InputGuardrailAdvisor` in the ChatClient configuration:
- Added as the first advisor in the chain (before `SimpleLoggerAdvisor`)
- Instantiated with `chatModel` parameter for validation calls

```kotlin
.defaultAdvisors(
    InputGuardrailAdvisor(chatModel),
    SimpleLoggerAdvisor(),
    MessageChatMemoryAdvisor.builder(chatMemory).build()
)
```

### 4. Exception Handling (`AiService.kt`)
**File**: `src/main/kotlin/com/airline/loyalty/service/AiService.kt`

Updated `processQuery()` method to handle guardrail rejections:
- Catches `IllegalArgumentException` from guardrail
- Returns rejection message to user gracefully
- Logs rejection at INFO level (not ERROR)
- Other exceptions still propagate as before

## How It Works

### Request Flow
1. User submits a query
2. **InputGuardrailAdvisor** (HIGHEST_PRECEDENCE) intercepts the request
3. Advisor calls the LLM with validation prompt (temperature=0.0)
4. LLM responds with "YES" or "NO"
5. If "NO": Throws `IllegalArgumentException` with rejection message
6. If "YES": Continues to next advisor in chain
7. **SimpleLoggerAdvisor** logs the request/response
8. **MessageChatMemoryAdvisor** manages conversation history
9. Main model processes the query and returns response

### Validation Process
```
User Query → InputGuardrailAdvisor → LLM Validation (temp=0.0)
                                            ↓
                                    YES or NO?
                                            ↓
                        YES: Continue → Process Query → Response
                        NO: Reject → Return Rejection Message
```

## Testing Instructions

### Prerequisites
1. **Install qwen3-coder:30b**:
   ```bash
   ollama pull qwen3-coder:30b
   ```

2. **Start Ollama** (if not already running):
   ```bash
   ollama serve
   ```

3. **Verify model is available**:
   ```bash
   ollama list
   curl http://localhost:11434/api/tags
   ```

### Start the Application
```bash
./gradlew bootRun
```

**Expected Logs**:
- Application starts on port 8080
- No errors about missing API keys
- Guardrail advisor is registered

### Test Cases

#### Valid Queries (Should Pass ✓)
These queries should be processed normally:

1. "What are the requirements for Delta Gold Medallion?"
2. "How do I earn United Premier status?"
3. "Can I use miles to upgrade my flight?"
4. "What benefits do Delta Platinum members get?"
5. "How many miles do I need for a free flight?"
6. "What's the difference between SkyMiles and MileagePlus?"

**Expected Behavior**:
- Log: `Guardrail: ✓ Query passed validation`
- Response: Detailed answer about airline loyalty programs

#### Invalid Queries (Should Be Rejected ✗)
These queries should be rejected by the guardrail:

1. "What's the weather like today?"
2. "Tell me a joke"
3. "How do I cook pasta?"
4. "What's the capital of France?"
5. "Write me a poem"
6. "What's the best pizza in New York?"

**Expected Behavior**:
- Log: `Guardrail: ✗ Query rejected - not related to airline loyalty programs`
- Response: 
  ```
  I'm specialized in airline loyalty programs. 
  Please ask questions about frequent flyer programs, 
  status tiers, miles, or airline rewards.
  ```

### Testing via Web Interface
1. Open browser to `http://localhost:8080`
2. Enter test queries in the chat interface
3. Observe responses and check logs

### Testing via API
```bash
# Valid query
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "How do I qualify for Delta Silver Medallion?", "conversationId": "test-123"}'

# Invalid query
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is the weather today?", "conversationId": "test-123"}'
```

## Architecture Decisions

### Why Exception-Based Rejection?
- Clean separation of concerns
- Leverages existing exception handling in service layer
- Prevents response from being generated unnecessarily
- Clear signal that request was rejected, not failed

### Why Fail Open?
- If validation LLM fails, allow query to proceed
- Prevents service outage due to validation issues
- Can be changed to fail closed by returning `false` in catch block

### Why Same Model for Validation?
- Simplifies configuration
- Ensures consistency in understanding
- Can be changed to use separate model if needed

### Why Temperature 0.0 for Validation?
- Ensures deterministic YES/NO responses
- Reduces variability in validation decisions
- More reliable guardrail behavior

## Performance Considerations

### Latency Impact
- Each query requires TWO LLM calls:
  1. Validation call (temperature=0.0)
  2. Main query call (temperature=1.0)
- Expected additional latency: 1-3 seconds per query
- Local model may be slower than cloud APIs

### Optimization Options
1. **Keyword-based pre-filter**: Check for obvious airline terms before LLM validation
2. **Caching**: Cache validation results for similar queries
3. **Separate validation model**: Use faster/smaller model for validation
4. **Async validation**: Validate in parallel with other operations

## Troubleshooting

### Issue: "Connection refused" to localhost:11434
**Solution**: Ensure Ollama is running: `ollama serve`

### Issue: Model not found
**Solution**: Pull the model: `ollama pull qwen3-coder:30b`

### Issue: All queries rejected
**Solution**: 
- Check validation prompt is correct
- Verify model is responding with YES/NO
- Check logs for validation results
- Try with temperature=0.0 explicitly

### Issue: Slow responses
**Solution**:
- Local models are slower than cloud APIs
- Consider using smaller model for validation
- Check system resources (CPU/GPU usage)

## Future Enhancements

1. **Metrics**: Add metrics for guardrail pass/fail rates
2. **Caching**: Cache validation results for performance
3. **Configuration**: Make validation prompt configurable
4. **Multiple Models**: Support different models for validation vs. main queries
5. **Keyword Pre-filter**: Add fast keyword check before LLM validation
6. **Rate Limiting**: Add rate limiting for validation calls
7. **A/B Testing**: Support multiple validation strategies

## Conclusion

The implementation successfully:
- ✅ Configures local Mistral model via Ollama
- ✅ Implements input guardrails using Spring AI Advisors
- ✅ Validates queries are airline loyalty related
- ✅ Rejects invalid queries with user-friendly messages
- ✅ Maintains conversation history and tool functionality
- ✅ Provides fail-safe behavior for validation errors
- ✅ Logs all validation decisions for monitoring

The system is now ready for testing with the local qwen3-coder:30b model!
