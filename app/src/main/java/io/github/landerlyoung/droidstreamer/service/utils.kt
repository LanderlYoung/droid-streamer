package io.github.landerlyoung.droidstreamer.service

import io.github.landerlyoung.droidstreamer.Global
import org.jetbrains.anko.wifiManager
import java.net.Inet4Address
import java.net.InetAddress

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2018-02-01
 * Time:   15:02
 * Life with Passion, Code with Creativity.
 * </pre>
 */

fun getWifiIpAddress(): Inet4Address {
    val localIpAddressInt = Global.app.wifiManager.connectionInfo?.ipAddress ?: 0
    return InetAddress.getByAddress(
            ByteArray(4, { i -> (localIpAddressInt.shr(i * 8).and(255)).toByte() })
    ) as Inet4Address
}