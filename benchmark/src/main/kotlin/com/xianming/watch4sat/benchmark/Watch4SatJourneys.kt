package com.xianming.watch4sat.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

internal const val TargetPackage = "com.xianming.watch4sat"

private const val UiTimeoutMillis = 10_000L
private const val DashboardTitle = "Watch4Sat"
private const val PassesDescription = "Passes"
private const val PassesTitlePrefix = "$PassesDescription \u00b7 "

internal fun MacrobenchmarkScope.prepareForLaunch() {
    val device = device()
    device.wakeUp()
    device.pressBack()
    pressHome()
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.startDashboard() {
    startActivityAndWait()
    device().waitForDashboard()
}

internal fun MacrobenchmarkScope.openPassesAndReturnToDashboard() {
    val device = device()
    val passes = checkNotNull(
        device.wait(Until.findObject(By.desc(PassesDescription)), UiTimeoutMillis)
    ) {
        "Dashboard Passes action was not available; the setup-completed fixture is required"
    }
    passes.click()
    check(device.wait(Until.gone(By.text(DashboardTitle)), UiTimeoutMillis)) {
        "Dashboard did not navigate to Passes"
    }
    check(device.wait(Until.hasObject(By.textStartsWith(PassesTitlePrefix)), UiTimeoutMillis)) {
        "Passes screen did not become visible"
    }

    device.pressBack()
    device.waitForDashboard()
}

private fun UiDevice.waitForDashboard() {
    check(wait(Until.hasObject(By.text(DashboardTitle)), UiTimeoutMillis)) {
        "Watch4Sat Dashboard was not visible; complete first-run setup before benchmarking"
    }
    check(wait(Until.hasObject(By.desc(PassesDescription)), UiTimeoutMillis)) {
        "Dashboard was not interactive before the timeout"
    }
}

private fun device(): UiDevice {
    return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
}
