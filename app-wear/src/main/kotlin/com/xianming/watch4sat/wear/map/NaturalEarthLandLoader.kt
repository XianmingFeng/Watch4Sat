package com.xianming.watch4sat.wear.map

import android.content.Context
import com.xianming.watch4sat.domain.geometry.NaturalEarthGeoJsonParser

internal object NaturalEarthLandLoader {
    fun load(context: Context): List<OfflineWorldPolygon> {
        return runCatching {
            context.assets.open("naturalearth/ne_110m_land.geojson").bufferedReader().use { reader ->
                NaturalEarthLandParser.parse(reader.readText())
            }
        }.getOrElse {
            fallbackPolygons()
        }
    }

    private fun fallbackPolygons(): List<OfflineWorldPolygon> {
        return listOf(
            roughBox(72.0, -168.0, 7.0, -52.0),
            roughBox(72.0, -20.0, 35.0, 178.0),
            roughBox(35.0, -18.0, -35.0, 52.0),
            roughBox(12.0, 95.0, -45.0, 155.0),
            roughBox(12.0, -82.0, -55.0, -35.0)
        )
    }

    private fun roughBox(
        north: Double,
        west: Double,
        south: Double,
        east: Double
    ): OfflineWorldPolygon {
        return OfflineWorldPolygon(
            rings = listOf(
                listOf(
                    OfflineMapLocation(north, west),
                    OfflineMapLocation(north, east),
                    OfflineMapLocation(south, east),
                    OfflineMapLocation(south, west),
                    OfflineMapLocation(north, west)
                )
            )
        )
    }
}

internal object NaturalEarthLandParser {
    fun parse(geoJson: String): List<OfflineWorldPolygon> {
        return NaturalEarthGeoJsonParser.parse(geoJson).map { polygon ->
            OfflineWorldPolygon(
                rings = polygon.rings.map { ring ->
                    ring.map { coordinate ->
                        OfflineMapLocation(
                            latitude = coordinate.latitudeDegrees,
                            longitude = coordinate.longitudeDegrees
                        )
                    }
                }
            )
        }
    }
}
