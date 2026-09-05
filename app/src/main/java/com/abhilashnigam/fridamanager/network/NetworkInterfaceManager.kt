package com.abhilashnigam.fridamanager.network

import java.net.Inet4Address
import java.net.NetworkInterface

data class NetworkAddress(
    val address: String,
    val interfaceName: String
)

object NetworkInterfaceManager {

    fun getIpv4Addresses(): List<NetworkAddress> {
        return try {
            NetworkInterface.getNetworkInterfaces()
                .toList()
                .flatMap { networkInterface ->
                    networkInterface.inetAddresses
                        .toList()
                        .filterIsInstance<Inet4Address>()
                        .filter { !it.isLoopbackAddress }
                        .mapNotNull { address ->
                            address.hostAddress?.let {
                                NetworkAddress(
                                    address = it,
                                    interfaceName = networkInterface.displayName
                                )
                            }
                        }
                }
                .distinctBy { it.address }
                .sortedBy { it.address }
        } catch (e: Exception) {
            emptyList()
        }
    }
}