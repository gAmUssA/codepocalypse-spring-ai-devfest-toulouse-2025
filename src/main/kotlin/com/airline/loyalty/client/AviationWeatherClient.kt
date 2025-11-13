package com.airline.loyalty.client

import com.airline.loyalty.model.MetarResponse
import com.airline.loyalty.model.TafResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.RestClientException

@Component
class AviationWeatherClient {
    
    private val logger = LoggerFactory.getLogger(AviationWeatherClient::class.java)
    private val restTemplate = RestTemplate()
    
    companion object {
        private const val BASE_URL = "https://aviationweather.gov/api/data"
    }
    
    fun fetchMetar(airportCode: String, hours: Int = 1): MetarResponse? {
        val url = "$BASE_URL/metar?ids=$airportCode&format=json&hours=$hours"
        logger.info("Fetching METAR for $airportCode from $url")
        
        return try {
            val response = restTemplate.getForObject(url, Array<MetarResponse>::class.java)
            response?.firstOrNull()?.also {
                logger.info("Successfully fetched METAR for $airportCode")
            } ?: run {
                logger.warn("No METAR data found for $airportCode")
                null
            }
        } catch (e: RestClientException) {
            logger.error("Failed to fetch METAR for $airportCode: ${e.message}", e)
            throw WeatherDataNotFoundException("Could not fetch METAR for $airportCode", e)
        }
    }
    
    fun fetchTaf(airportCode: String): TafResponse? {
        val url = "$BASE_URL/taf?ids=$airportCode&format=json"
        logger.info("Fetching TAF for $airportCode from $url")
        
        return try {
            val response = restTemplate.getForObject(url, Array<TafResponse>::class.java)
            response?.firstOrNull()?.also {
                logger.info("Successfully fetched TAF for $airportCode")
            } ?: run {
                logger.warn("No TAF data found for $airportCode")
                null
            }
        } catch (e: RestClientException) {
            logger.error("Failed to fetch TAF for $airportCode: ${e.message}", e)
            throw WeatherDataNotFoundException("Could not fetch TAF for $airportCode", e)
        }
    }
}

class WeatherDataNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
