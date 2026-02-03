package com.vpn.client.vpn

/**
 * رابط اجرای tun2socks: ترافیک TUN را به پروکسی SOCKS (معمولاً 127.0.0.1:10808) هدایت می‌کند.
 * برای VPN واقعی باید یک پیاده‌سازی (مثلاً از go-tun2socks یا xxf098/go-tun2socks-build) با TUN fd ادغام شود.
 */
interface Tun2SocksRunner {

    /**
     * tun2socks را با فایل‌دیسکریپتور TUN و آدرس پروکسی SOCKS راه می‌اندازد.
     * @param tunFd فایل دیسکریپتور TUN (از VpnService.Builder.establish())
     * @param proxyAddress آدرس پروکسی مثلاً "127.0.0.1:10808"
     */
    fun start(tunFd: Int, proxyAddress: String)

    fun stop()
    fun isRunning(): Boolean
}
