package com.airline.loyalty.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TafResponse(
    @JsonProperty("icaoId")
    val airportCode: String? = null,
    
    @JsonProperty("issueTime")
    val issueTime: String? = null,
    
    @JsonProperty("validTimeFrom")
    val validFrom: Long? = null,
    
    @JsonProperty("validTimeTo")
    val validTo: Long? = null,
    
    @JsonProperty("rawTAF")
    val rawTaf: String? = null,
    
    @JsonProperty("fcsts")
    val forecastPeriods: List<ForecastPeriod>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ForecastPeriod(
    @JsonProperty("timeFrom")
    val timeFrom: Long? = null,
    
    @JsonProperty("timeTo")
    val timeTo: Long? = null,
    
    @JsonProperty("wspd")
    val windSpeedKnots: Int? = null,
    
    @JsonProperty("wdir")
    val windDirectionDegrees: Int? = null,
    
    @JsonProperty("visib")
    val visibilityMiles: String? = null,
    
    @JsonProperty("wxString")
    val weather: String? = null,
    
    @JsonProperty("clouds")
    val skyConditions: List<SkyCondition>? = null
)
