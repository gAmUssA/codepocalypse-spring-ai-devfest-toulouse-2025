package com.airline.loyalty.service

import com.airline.loyalty.model.MetarResponse
import com.airline.loyalty.model.TafResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Service

@Service
class WeatherToolsService(
    private val weatherService: AviationWeatherService,
    private val objectMapper: ObjectMapper
) {
    
    private val logger = LoggerFactory.getLogger(WeatherToolsService::class.java)
    
    @Tool(
        name = "get_metar",
        description = """Get current weather observation (METAR) for an airport. 
            Provide the ICAO airport code (4 uppercase letters, e.g., KJFK for New York JFK, 
            KLAX for Los Angeles, EGLL for London Heathrow, YSSY for Sydney).
            Optionally specify hours (1-24) of historical data to retrieve."""
    )
    fun getMetar(
        airportCode: String,
        hours: Int = 1
    ): String {
        logger.info("MCP Tool called: get_metar for $airportCode (hours: $hours)")
        
        return try {
            val metar = weatherService.getMetar(airportCode.uppercase(), hours)
            
            if (metar == null) {
                """{"error": "No METAR data available for airport code $airportCode"}"""
            } else {
                formatMetarResponse(metar)
            }
        } catch (e: Exception) {
            logger.error("Error in get_metar tool: ${e.message}", e)
            """{"error": "${e.message}"}"""
        }
    }
    
    @Tool(
        name = "get_taf",
        description = """Get weather forecast (TAF - Terminal Aerodrome Forecast) for an airport.
            Provide the ICAO airport code (4 uppercase letters, e.g., KJFK for New York JFK,
            KLAX for Los Angeles, EGLL for London Heathrow, YSSY for Sydney).
            TAF provides forecast information for the next 24-30 hours."""
    )
    fun getTaf(airportCode: String): String {
        logger.info("MCP Tool called: get_taf for $airportCode")
        
        return try {
            val taf = weatherService.getTaf(airportCode.uppercase())
            
            if (taf == null) {
                """{"error": "No TAF data available for airport code $airportCode"}"""
            } else {
                formatTafResponse(taf)
            }
        } catch (e: Exception) {
            logger.error("Error in get_taf tool: ${e.message}", e)
            """{"error": "${e.message}"}"""
        }
    }
    
    @Tool(
        name = "get_airport_weather_summary",
        description = """Get comprehensive weather summary combining current conditions (METAR) 
            and forecast (TAF) for an airport. Provide the ICAO airport code (4 uppercase letters, 
            e.g., KJFK for New York JFK, KLAX for Los Angeles, EGLL for London Heathrow, YSSY for Sydney).
            This provides a complete weather picture for flight planning."""
    )
    fun getAirportWeatherSummary(airportCode: String): String {
        logger.info("MCP Tool called: get_airport_weather_summary for $airportCode")
        
        return try {
            val summary = weatherService.getWeatherSummary(airportCode.uppercase())
            objectMapper.writeValueAsString(summary)
        } catch (e: Exception) {
            logger.error("Error in get_airport_weather_summary tool: ${e.message}", e)
            """{"error": "${e.message}"}"""
        }
    }
    
    private fun formatMetarResponse(metar: MetarResponse): String {
        val response = buildMap {
            put("airport_code", metar.airportCode ?: "Unknown")
            put("observation_time", metar.observationTime ?: "Unknown")
            put("raw_metar", metar.rawMetar ?: "Not available")
            metar.temperatureCelsius?.let { put("temperature_celsius", it) }
            metar.dewpointCelsius?.let { put("dewpoint_celsius", it) }
            metar.windSpeedKnots?.let { put("wind_speed_knots", it) }
            metar.windDirectionDegrees?.let { put("wind_direction_degrees", it) }
            metar.visibilityMiles?.let { put("visibility_miles", it) }
            metar.altimeterInches?.let { put("altimeter_inches", it) }
            metar.flightCategory?.let { put("flight_category", it) }
            metar.skyConditions?.let { put("sky_conditions", it) }
        }
        return objectMapper.writeValueAsString(response)
    }
    
    private fun formatTafResponse(taf: TafResponse): String {
        val response = buildMap {
            put("airport_code", taf.airportCode ?: "Unknown")
            put("issue_time", taf.issueTime ?: "Unknown")
            put("valid_from", taf.validFrom ?: "Unknown")
            put("valid_to", taf.validTo ?: "Unknown")
            put("raw_taf", taf.rawTaf ?: "Not available")
            taf.forecastPeriods?.let { put("forecast_periods", it) }
        }
        return objectMapper.writeValueAsString(response)
    }
}
