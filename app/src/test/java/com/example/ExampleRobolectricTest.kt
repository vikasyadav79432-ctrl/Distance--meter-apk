package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.location.HaversineCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Walk Meter", appName)
  }

  @Test
  fun `haversine calculation verifies distance`() {
    // 1 degree latitude difference is roughly 111,195 meters
    val dist = HaversineCalculator.calculateHaversineDistanceMeters(0.0, 0.0, 1.0, 0.0)
    assertTrue("Distance should be around 111km", dist in 111000.0..112000.0)

    // Formatted meters and km
    assertEquals("1,250.0", HaversineCalculator.formatMeters(1250.0))
    assertEquals("1.250", HaversineCalculator.formatKilometers(1250.0))
    assertEquals("01:40", HaversineCalculator.formatDuration(100L))
    assertEquals("01:00:00", HaversineCalculator.formatDuration(3600L))
  }
}
