package com.xianming.watch4sat.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MinifiedRuntimeSmokeTest {

    @Test
    fun minifiedAppRestoresDashboardAndDataBackedPasses() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wakeUp()
        device.pressHome()
        device.executeShellCommand(
            "am start -W -n com.xianming.watch4sat/.MainActivity"
        )

        assertTrue(
            "Minified app did not restore the Dashboard",
            device.wait(Until.hasObject(By.text("Watch4Sat")), UiTimeoutMillis)
        )
        val passes = device.wait(Until.findObject(By.desc("Passes")), UiTimeoutMillis)
        assertTrue("Minified Dashboard was not interactive", passes != null)
        passes.click()

        assertTrue(
            "Minified app could not open the data-backed Passes screen",
            device.wait(Until.hasObject(By.textStartsWith("Passes \u00b7 ")), UiTimeoutMillis)
        )
    }

    private companion object {
        const val UiTimeoutMillis = 10_000L
    }
}
