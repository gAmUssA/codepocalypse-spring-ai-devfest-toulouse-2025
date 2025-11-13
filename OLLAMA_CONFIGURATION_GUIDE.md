# Proper Ollama Configuration in Spring AI

## Overview

This guide explains how to properly configure and use Ollama with Spring AI, based on the official Spring AI documentation (version 1.0.3).

## Key Findings from Context7 Documentation

Spring AI provides **native Ollama integration** through the `spring-ai-starter-model-ollama` dependency. This is the recommended approach rather than using Ollama's OpenAI-compatible endpoint.

## Changes Made

### 1. Configuration (application.yml)

**❌ INCORRECT (OpenAI-compatible endpoint approach):**
```yaml
spring:
  ai:
    openai:
      api-key: not-needed
      base-url: http://localhost:11434/v1  # OpenAI-compatible endpoint
      chat:
        options:
          model: qwen3-coder:30b
          temperature: 1
```

**✅ CORRECT (Native Ollama integration):**
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434  # Native Ollama endpoint (no /v1)
      chat:
        options:
          model: qwen2.5-coder:32b
          temperature: 1.0
```

**Key Differences:**
- Use `spring.ai.ollama` namespace instead of `spring.ai.openai`
- Base URL is `http://localhost:11434` (without `/v1` suffix)
- No need for `api-key` property
- Direct access to Ollama-specific features

### 2. Code Changes (InputGuardrailAdvisor.kt)

**❌ INCORRECT:**
```kotlin
import org.springframework.ai.openai.OpenAiChatOptions

// ...
val validationPrompt = Prompt(
    "$VALIDATION_SYSTEM_PROMPT\n\nUser question: $userMessage",
    OpenAiChatOptions.builder()
        .temperature(0.0)
        .build()
)
```

**✅ CORRECT:**
```kotlin
import org.springframework.ai.ollama.api.OllamaOptions

// ...
val validationPrompt = Prompt(
    "$VALIDATION_SYSTEM_PROMPT\n\nUser question: $userMessage",
    OllamaOptions.builder()
        .temperature(0.0)
        .build()
)
```

## Why Use Native Ollama Integration?

### 1. **Official Support**
- Spring AI provides dedicated Ollama support through `spring-ai-ollama` module
- Better maintained and tested than using OpenAI-compatible endpoint

### 2. **Ollama-Specific Features**
Access to Ollama-specific options:
```kotlin
OllamaOptions.builder()
    .model("qwen2.5-coder:32b")
    .temperature(0.7)
    .topK(40)
    .topP(0.9)
    .numPredict(100)
    .build()
```

### 3. **Automatic Model Management**
Spring AI can automatically pull models at startup:
```yaml
spring:
  ai:
    ollama:
      init:
        pull-model-strategy: always  # or 'when_missing'
        timeout: 60s
        max-retries: 1
        chat:
          additional-models:
            - llama3.2
            - qwen2.5
```

### 4. **Better Error Handling**
Native integration provides better error messages and handling specific to Ollama

### 5. **Type Safety**
Using `OllamaOptions` provides compile-time type safety for Ollama-specific parameters

## Configuration Options

### Basic Configuration
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: mistral
          temperature: 0.7
```

### Advanced Configuration with Model Management
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      init:
        pull-model-strategy: when_missing  # Options: always, when_missing, never
        timeout: 120s
        max-retries: 3
        chat:
          additional-models:
            - qwen2.5-coder:32b
            - llama3.2
      chat:
        options:
          model: qwen2.5-coder:32b
          temperature: 1.0
          num-predict: 500
```

### Using HuggingFace GGUF Models
```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          model: hf.co/bartowski/gemma-2-2b-it-GGUF
      init:
        pull-model-strategy: always
```

## Programmatic Configuration

### Manual Bean Configuration
```kotlin
@Configuration
class OllamaConfiguration {
    
    @Bean
    fun ollamaApi(): OllamaApi {
        return OllamaApi.builder()
            .baseUrl("http://localhost:11434")
            .build()
    }
    
    @Bean
    fun chatModel(ollamaApi: OllamaApi): OllamaChatModel {
        return OllamaChatModel.builder()
            .ollamaApi(ollamaApi)
            .defaultOptions(
                OllamaOptions.builder()
                    .model("qwen2.5-coder:32b")
                    .temperature(0.9)
                    .build()
            )
            .build()
    }
}
```

### Using ChatClient with Ollama Options
```kotlin
val response = chatClient.prompt()
    .user("Your question here")
    .options(
        OllamaOptions.builder()
            .model("qwen2.5-coder:32b")
            .temperature(0.7)
            .build()
    )
    .call()
    .content()
```

## Streaming Responses

Ollama supports streaming responses natively:

```kotlin
val flux: Flux<ChatResponse> = chatModel.stream(
    Prompt("Generate the names of 5 famous pirates.")
)

flux.subscribe { response ->
    println(response.results[0].output.text)
}
```

## Dependencies

Ensure you have the correct dependency in `build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    // Remove or don't use: spring-ai-starter-model-openai for Ollama
}
```

## Testing Your Configuration

### 1. Verify Ollama is Running
```bash
ollama ps
curl http://localhost:11434/api/tags
```

### 2. Pull Required Model
```bash
ollama pull qwen2.5-coder:32b
```

### 3. Start Your Application
```bash
./gradlew bootRun
```

### 4. Check Logs
Look for successful initialization:
- No errors about missing models
- Successful connection to Ollama
- ChatModel bean created successfully

## Common Issues and Solutions

### Issue: "Connection refused to localhost:11434"
**Solution:** Ensure Ollama is running: `ollama serve`

### Issue: "Model not found"
**Solution:** Pull the model: `ollama pull qwen2.5-coder:32b`

### Issue: "Unresolved reference: OllamaOptions"
**Solution:** Ensure you have `spring-ai-starter-model-ollama` dependency and use correct import:
```kotlin
import org.springframework.ai.ollama.api.OllamaOptions
```

### Issue: Slow startup with pull-model-strategy: always
**Solution:** 
- Use `when_missing` instead of `always` in production
- Pre-pull models before deployment
- Increase timeout if needed

## Performance Considerations

### Model Pulling
- **Development:** Use `pull-model-strategy: always` to ensure latest models
- **Production:** Use `pull-model-strategy: never` and pre-pull models during deployment

### Connection Pooling
Ollama API client handles connection pooling automatically

### Timeout Configuration
```yaml
spring:
  ai:
    ollama:
      init:
        timeout: 120s  # Increase for large models
```

## Migration Checklist

If migrating from OpenAI-compatible endpoint to native Ollama:

- [ ] Update `application.yml`: Change `spring.ai.openai` to `spring.ai.ollama`
- [ ] Remove `/v1` suffix from base URL
- [ ] Remove `api-key` property
- [ ] Update imports: `OpenAiChatOptions` → `OllamaOptions`
- [ ] Update code: `OpenAiChatOptions.builder()` → `OllamaOptions.builder()`
- [ ] Test compilation: `./gradlew compileKotlin`
- [ ] Test build: `./gradlew build`
- [ ] Test runtime: `./gradlew bootRun`

## Additional Resources

- **Spring AI Documentation:** https://docs.spring.io/spring-ai/reference/
- **Ollama Documentation:** https://ollama.ai/
- **Spring AI GitHub:** https://github.com/spring-projects/spring-ai
- **Ollama Models:** https://ollama.ai/library

## Summary

Using Spring AI's native Ollama integration provides:
- ✅ Better type safety with `OllamaOptions`
- ✅ Access to Ollama-specific features
- ✅ Automatic model management
- ✅ Better error handling
- ✅ Official support and maintenance
- ✅ Cleaner configuration

The OpenAI-compatible endpoint approach should only be used when you need to switch between OpenAI and Ollama dynamically, but even then, Spring AI's abstraction layer makes this unnecessary.
