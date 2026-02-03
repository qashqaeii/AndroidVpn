package com.vpn.client.vpn

/**
 * Stub: tun2socks در پروژه ادغام نشده است.
 * با اضافه کردن کتابخانهٔ tun2socks (مثلاً go-tun2socks) و فراخوانی واقعی از [start]، ترافیک TUN به SOCKS هدایت می‌شود.
 */
class Tun2SocksStub : Tun2SocksRunner {

    override fun start(tunFd: Int, proxyAddress: String) {
        // بدون پیاده‌سازی واقعی؛ فقط برای سازگاری با لایهٔ سرویس.
    }

    override fun stop() {}
    override fun isRunning(): Boolean = false
}
