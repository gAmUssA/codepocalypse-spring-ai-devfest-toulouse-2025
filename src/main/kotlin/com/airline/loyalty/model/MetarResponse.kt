package com.airline.loyalty.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MetarResponse(
    @JsonProperty("icaoId")
    val airportCode: String? = null,
    
    @JsonProperty("reportTime")
    val observationTime: String? = null,
    
    @JsonProperty("rawOb")
    val rawMetar: String? = null,
    
    @JsonProperty("temp")
    val temperatureCelsius: Double? = null,
    
    @JsonProperty("dewp")
    val dewpointCelsius: Double? = null,
    
    @JsonProperty("wspd")
    val windSpeedKnots: Int? = null,
    
    @JsonProperty("wdir")
    val windDirectionDegrees: Int? = null,
    
    @JsonProperty("visib")
    val visibilityMiles: String? = null,
    
    @JsonProperty("altim")
    val altimeterInches: Double? = null,
    
    @JsonProperty("fltCat")
    val flightCategory: String? = null,
    
    @JsonProperty("clouds")
    val skyConditions: List<SkyCondition>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SkyCondition(
    @JsonProperty("cover")
    val coverage: String? = null,
    
    @JsonProperty("base")
    val altitudeFeet: Int? = null
)
