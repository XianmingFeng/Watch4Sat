package com.xianming.watch4sat.data.network

interface Watch4SatNetworkDataSource {

    suspend fun fetchCelestrakAmateur(): String

    suspend fun fetchSatnogsActiveTransmitters(): String
}
