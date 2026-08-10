package com.xianming.watch4sat.wear

import android.content.Context
import com.xianming.watch4sat.R
import com.xianming.watch4sat.data.repository.DataRefreshOutcome
import com.xianming.watch4sat.data.repository.DataRefreshSource
import com.xianming.watch4sat.data.repository.DataRefreshSummary

internal fun DataRefreshSummary.resolveMessage(context: Context): String {
    return when (val result = outcome) {
        is DataRefreshOutcome.Complete -> context.getString(
            R.string.vm_refresh_complete,
            result.satelliteRecordsPersisted,
            result.transmitterRecordsPersisted
        )
        is DataRefreshOutcome.Partial -> when (result.successfulSource) {
            DataRefreshSource.Satellites -> context.getString(
                R.string.vm_refresh_partial_tle_saved,
                result.recordsPersisted
            )
            DataRefreshSource.Transmitters -> context.getString(
                R.string.vm_refresh_partial_tx_saved,
                result.recordsPersisted
            )
        }
        DataRefreshOutcome.Failed -> context.getString(R.string.vm_refresh_failed)
    }
}
