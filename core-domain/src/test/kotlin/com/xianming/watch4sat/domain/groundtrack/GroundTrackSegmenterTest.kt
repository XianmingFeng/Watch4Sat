package com.xianming.watch4sat.domain.groundtrack

import com.xianming.watch4sat.domain.model.GroundTrackPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class GroundTrackSegmenterTest {

    @Test
    fun `track crossing antimeridian is split into separate segments`() {
        val segments = GroundTrackSegmenter.splitAtAntimeridian(
            listOf(
                GroundTrackPoint(timeMillis = 0L, latitudeDegrees = 0.0, longitudeDegrees = 170.0),
                GroundTrackPoint(timeMillis = 1L, latitudeDegrees = 0.0, longitudeDegrees = 179.0),
                GroundTrackPoint(timeMillis = 2L, latitudeDegrees = 0.0, longitudeDegrees = -179.0),
                GroundTrackPoint(timeMillis = 3L, latitudeDegrees = 0.0, longitudeDegrees = -170.0)
            )
        )

        assertEquals(2, segments.size)
        assertEquals(listOf(170.0, 179.0, 180.0), segments[0].map { it.longitudeDegrees })
        assertEquals(listOf(-180.0, -179.0, -170.0), segments[1].map { it.longitudeDegrees })
        assertEquals(segments[0].last().latitudeDegrees, segments[1].first().latitudeDegrees, 0.0)
        assertEquals(segments[0].last().timeMillis, segments[1].first().timeMillis)
    }

    @Test
    fun `track without antimeridian crossing stays one segment`() {
        val segments = GroundTrackSegmenter.splitAtAntimeridian(
            listOf(
                GroundTrackPoint(timeMillis = 0L, latitudeDegrees = 10.0, longitudeDegrees = 100.0),
                GroundTrackPoint(timeMillis = 1L, latitudeDegrees = 11.0, longitudeDegrees = 110.0)
            )
        )

        assertEquals(1, segments.size)
        assertEquals(2, segments.single().size)
    }
}
