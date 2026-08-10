package com.xianming.watch4sat.location

import kotlinx.coroutines.flow.Flow

interface LocationUpdateProvider {
    fun locationUpdates(): Flow<LocationFix>
}
