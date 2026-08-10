package com.xianming.watch4sat.data

import android.content.Context
import com.xianming.watch4sat.data.local.Watch4SatDatabase
import com.xianming.watch4sat.data.network.NetworkClientIdentity
import com.xianming.watch4sat.data.network.OkHttpWatch4SatNetworkDataSource
import com.xianming.watch4sat.data.network.Watch4SatNetworkDataSource
import com.xianming.watch4sat.data.pass.PassSnapshotCache
import com.xianming.watch4sat.data.pass.PassSnapshotStore
import com.xianming.watch4sat.data.repository.DefaultSatelliteDataRepository
import com.xianming.watch4sat.data.repository.SatelliteDataRepository
import com.xianming.watch4sat.data.settings.Watch4SatSettingsStore
import okhttp3.OkHttpClient

object Watch4SatDataLayer {

    fun create(
        context: Context,
        networkClientIdentity: NetworkClientIdentity,
        okHttpClient: OkHttpClient = OkHttpClient()
    ): Watch4SatDependencies {
        val database = Watch4SatDatabase.create(context)
        val settingsStore = Watch4SatSettingsStore(context)
        val passSnapshotCache = PassSnapshotStore(context)
        val repository = DefaultSatelliteDataRepository(
            satelliteDao = database.satelliteDao(),
            transmitterDao = database.transmitterDao(),
            settingsStore = settingsStore,
            networkDataSource = OkHttpWatch4SatNetworkDataSource(
                client = okHttpClient,
                userAgent = networkClientIdentity.userAgent
            )
        )
        return Watch4SatDependencies(
            satelliteDataRepository = repository,
            settingsStore = settingsStore,
            passSnapshotCache = passSnapshotCache
        )
    }

    fun createSatelliteDataRepository(
        context: Context,
        networkClientIdentity: NetworkClientIdentity,
        okHttpClient: OkHttpClient = OkHttpClient()
    ): SatelliteDataRepository {
        return create(context, networkClientIdentity, okHttpClient).satelliteDataRepository
    }

    fun createLocalOnly(context: Context): Watch4SatDependencies {
        val database = Watch4SatDatabase.create(context)
        val settingsStore = Watch4SatSettingsStore(context)
        val passSnapshotCache = PassSnapshotStore(context)
        val repository = DefaultSatelliteDataRepository(
            satelliteDao = database.satelliteDao(),
            transmitterDao = database.transmitterDao(),
            settingsStore = settingsStore,
            networkDataSource = LocalOnlyNetworkDataSource
        )
        return Watch4SatDependencies(
            satelliteDataRepository = repository,
            settingsStore = settingsStore,
            passSnapshotCache = passSnapshotCache
        )
    }

    private object LocalOnlyNetworkDataSource : Watch4SatNetworkDataSource {
        override suspend fun fetchCelestrakAmateur(): String {
            error("Network refresh is not available from local-only background paths.")
        }

        override suspend fun fetchSatnogsActiveTransmitters(): String {
            error("Network refresh is not available from local-only background paths.")
        }
    }
}

data class Watch4SatDependencies(
    val satelliteDataRepository: SatelliteDataRepository,
    val settingsStore: Watch4SatSettingsStore,
    val passSnapshotCache: PassSnapshotCache
)
