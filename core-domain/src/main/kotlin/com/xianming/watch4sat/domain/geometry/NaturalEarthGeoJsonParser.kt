package com.xianming.watch4sat.domain.geometry

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class GeographicCoordinate(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double
)

data class GeographicPolygon(
    val rings: List<List<GeographicCoordinate>>
)

object NaturalEarthGeoJsonParser {
    fun parse(geoJson: String): List<GeographicPolygon> {
        val features = Json.parseToJsonElement(geoJson)
            .jsonObject
            .getValue("features")
            .jsonArray
        return buildList {
            features.forEach { feature ->
                val geometry = feature.jsonObject.getValue("geometry").jsonObject
                val coordinates = geometry.getValue("coordinates").jsonArray
                when (geometry.getValue("type").jsonPrimitive.content) {
                    "Polygon" -> add(parsePolygon(coordinates))
                    "MultiPolygon" -> coordinates.forEach { polygon ->
                        add(parsePolygon(polygon.jsonArray))
                    }
                }
            }
        }
    }

    private fun parsePolygon(polygon: JsonArray): GeographicPolygon {
        return GeographicPolygon(
            rings = polygon.mapNotNull { ringElement ->
                val ring = ringElement.jsonArray.map { pointElement ->
                    val point = pointElement.jsonArray
                    GeographicCoordinate(
                        latitudeDegrees = point[1].jsonPrimitive.double,
                        longitudeDegrees = point[0].jsonPrimitive.double
                    )
                }
                ring.takeIf { it.size >= MinimumPolygonPoints }
            }
        )
    }

    private const val MinimumPolygonPoints = 3
}
