package com.airline.loyalty.service

import com.airline.loyalty.client.AviationWeatherClient
import com.airline.loyalty.model.MetarResponse
import com.airline.loyalty.model.TafResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AviationWeatherService(
    private val client: AviationWeatherClient
) {
    
    private val logger = LoggerFactory.getLogger(AviationWeatherService::class.java)
    
    fun getMetar(airportCode: String, hours: Int = 1): MetarResponse? {
        validateAirportCode(airportCode)
        validateHours(hours)
        logger.info("Getting METAR for $airportCode (hours: $hours)")
        return client.fetchMetar(airportCode, hours)
    }
    
    fun getTaf(airportCode: String): TafResponse? {
        validateAirportCode(airportCode)
        logger.info("Getting TAF for $airportCode")
        return client.fetchTaf(airportCode)
    }
    
    fun getWeatherSummary(airportCode: String): Map<String, Any?> {
        validateAirportCode(airportCode)
        logger.info("Getting weather summary for $airportCode")
        
        val metar = client.fetchMetar(airportCode, 1)
        val taf = client.fetchTaf(airportCode)
        
        return mapOf(
            "airport_code" to airportCode,
            "current_weather" to metar,
            "forecast" to taf
        )
    }
    
    private fun validateAirportCode(code: String) {
        if (!code.matches(Regex("^[A-Z]{4}$"))) {
            throw InvalidAirportCodeException(
                "Airport code must be 4 uppercase letters (ICAO format). Provided: $code"
            )
        }
    }
    
    private fun validateHours(hours: Int) {
        if (hours < 1 || hours > 24) {
            throw IllegalArgumentException(
                "Hours must be between 1 and 24. Provided: $hours"
            )
        }
    }
}

class InvalidAirportCodeException(message: String) : IllegalArgumentException(message)
