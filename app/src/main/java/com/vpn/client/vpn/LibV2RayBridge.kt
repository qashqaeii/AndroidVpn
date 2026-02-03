package com.vpn.client.vpn

import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * پل انعکاسی به libv2ray (AndroidLibV2rayLite / Mronezc AAR).
 * دو API پشتیبانی می‌شود:
 * ۱) go.libv2ray: InitCoreEnv، CoreController، StartLoop(config)/StopLoop
 * ۲) libv2ray (Mronezc/2dust): Libv2ray.newV2RayPoint(handler)، V2RayPoint با setConfigureFileContent و runLoop/stopLoop
 */
object LibV2RayBridge {

    private const val TAG = "LibV2RayBridge"

    private var coreController: Any? = null
    private var runLoopThread: Thread? = null
    private var lastEnvPath: String = ""

    /** برای اینکه سوکت‌های هسته از TUN خارج شوند، باید VpnService.protect(fd) صدا زده شود. از start(configJson, protectCallback) استفاده کنید. */
    var protectCallback: ((Int) -> Boolean)? = null

    // API نوع اول: CoreController
    private var initCoreEnvMethod: Method? = null
    private var newCoreControllerMethod: Method? = null
    private var startLoopMethod: Method? = null
    private var stopLoopMethod: Method? = null
    private var handlerInterface: Class<*>? = null

    // API نوع دوم: V2RayPoint (libv2ray پکیج - Mronezc AAR)
    private var newV2RayPointMethod: Method? = null
    private var v2RayPointSetConfigMethod: Method? = null
    private var v2RayPointRunLoopMethod: Method? = null
    private var v2RayPointStopLoopMethod: Method? = null
    private var v2RayPointHandlerInterface: Class<*>? = null

    private var useV2RayPointApi = false

    val isAvailable: Boolean
        get() = when {
            useV2RayPointApi -> newV2RayPointMethod != null && v2RayPointRunLoopMethod != null &&
                    v2RayPointStopLoopMethod != null && v2RayPointHandlerInterface != null &&
                    (v2RayPointSetConfigMethod != null || (v2RayPointRunLoopMethod!!.parameterCount == 1))
            else -> initCoreEnvMethod != null && newCoreControllerMethod != null &&
                    startLoopMethod != null && stopLoopMethod != null && handlerInterface != null
        }

    init {
        resolveClasses()
    }

    private fun loadClass(name: String): Class<*>? {
        return try {
            val loader = LibV2RayBridge::class.java.classLoader ?: ClassLoader.getSystemClassLoader()
            Class.forName(name, true, loader)
        } catch (_: Throwable) { null }
    }

    private fun findMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Method? {
        return try { clazz.getMethod(name, *paramTypes) } catch (_: Exception) { null }
    }

    private fun findMethodAnyCase(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Method? {
        val lower = name.lowercase()
        for (m in clazz.methods) {
            if (m.name.lowercase() == lower && m.parameterTypes.size == paramTypes.size) {
                var match = true
                for (i in paramTypes.indices) {
                    if (m.parameterTypes[i] != paramTypes[i]) { match = false; break }
                }
                if (match) return m
            }
        }
        return null
    }

    private fun resolveClasses() {
        // کتابخانهٔ native هسته (libgojni.so) باید قبل از لود کلاس‌های جاوا لود شود
        try {
            System.loadLibrary("gojni")
        } catch (e: Throwable) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "loadLibrary(gojni): ${e.message}")
        }

        // اول API نوع اول (go.libv2ray با CoreController) را امتحان کن
        try {
            val pkg = "go.libv2ray"
            val libClass = loadClass("$pkg.Libv2ray") ?: throw ClassNotFoundException("$pkg.Libv2ray")
            val controllerClass = loadClass("$pkg.CoreController") ?: throw ClassNotFoundException("$pkg.CoreController")
            val handler = loadClass("$pkg.CoreCallbackHandler") ?: throw ClassNotFoundException("$pkg.CoreCallbackHandler")
            handlerInterface = handler
            initCoreEnvMethod = findMethod(libClass, "initCoreEnv", String::class.java, String::class.java)
                ?: findMethod(libClass, "InitCoreEnv", String::class.java, String::class.java)
            newCoreControllerMethod = findMethod(libClass, "newCoreController", handler)
                ?: findMethod(libClass, "NewCoreController", handler)
            startLoopMethod = findMethod(controllerClass, "startLoop", String::class.java)
                ?: findMethod(controllerClass, "StartLoop", String::class.java)
            stopLoopMethod = findMethod(controllerClass, "stopLoop")
                ?: findMethod(controllerClass, "StopLoop")
            if (initCoreEnvMethod != null && newCoreControllerMethod != null && startLoopMethod != null && stopLoopMethod != null) {
                Log.i(TAG, "libv2ray resolved (CoreController API): $pkg")
                return
            }
        } catch (_: Throwable) { }

        // دوم API نوع دوم (libv2ray با V2RayPoint - Mronezc/2dust AAR)
        try {
            val pkg = "libv2ray"
            val libClass = loadClass("$pkg.Libv2ray")
            if (libClass == null) {
                Log.w(TAG, "libv2ray.Libv2ray not loadable (AAR in app/libs?)")
                throw ClassNotFoundException("$pkg.Libv2ray")
            }
            val pointClass = loadClass("$pkg.V2RayPoint") ?: throw ClassNotFoundException("$pkg.V2RayPoint")
            val handlerClass = loadClass("$pkg.V2RayVPNServiceSupportsSet") ?: throw ClassNotFoundException("$pkg.V2RayVPNServiceSupportsSet")
            v2RayPointHandlerInterface = handlerClass
            newV2RayPointMethod = findMethod(libClass, "newV2RayPoint", handlerClass)
                ?: findMethod(libClass, "NewV2RayPoint", handlerClass)
                ?: findMethodAnyCase(libClass, "newV2RayPoint", handlerClass)
            v2RayPointSetConfigMethod = findMethod(pointClass, "setConfigureFileContent", String::class.java)
                ?: findMethod(pointClass, "setConfigurefilecontent", String::class.java)
                ?: findMethodAnyCase(pointClass, "setConfigureFileContent", String::class.java)
            v2RayPointRunLoopMethod = findMethod(pointClass, "runLoop")
                ?: findMethod(pointClass, "RunLoop")
                ?: findMethod(pointClass, "runLoop", String::class.java)
                ?: findMethod(pointClass, "RunLoop", String::class.java)
                ?: findMethodAnyCase(pointClass, "runLoop")
            v2RayPointStopLoopMethod = findMethod(pointClass, "stopLoop")
                ?: findMethod(pointClass, "StopLoop")
                ?: findMethodAnyCase(pointClass, "stopLoop")
            if (newV2RayPointMethod != null && v2RayPointRunLoopMethod != null && v2RayPointStopLoopMethod != null &&
                (v2RayPointSetConfigMethod != null || (v2RayPointRunLoopMethod!!.parameterCount == 1 && v2RayPointRunLoopMethod!!.parameterTypes[0] == String::class.java))
            ) {
                useV2RayPointApi = true
                Log.i(TAG, "libv2ray resolved (V2RayPoint API): $pkg")
                return
            }
        } catch (e: Throwable) {
            Log.w(TAG, "libv2ray V2RayPoint API not found: ${e.message}", e)
        }

        Log.w(TAG, "libv2ray AAR not found or incompatible; put libv2ray.aar in app/libs")
    }

    /**
     * محیط هسته را با مسیر و کلید مقداردهی اولیه می‌کند (فقط برای API نوع اول).
     */
    fun initCoreEnv(envPath: String, key: String) {
        if (!isAvailable) return
        lastEnvPath = envPath
        if (useV2RayPointApi) return // در API V2RayPoint از lastEnvPath در start استفاده می‌شود
        try {
            initCoreEnvMethod?.invoke(null, envPath, key)
        } catch (e: Throwable) {
            Log.e(TAG, "InitCoreEnv failed", e)
        }
    }

    /**
     * هسته را با کانفیگ JSON راه می‌اندازد.
     * @param protectCallback برای هر fd سوکت هسته صدا زده می‌شود؛ باید VpnService.protect(fd) برگرداند تا اتصال درست کار کند.
     * @return true اگر راه‌اندازی با موفقیت شروع شد
     */
    fun start(configJson: String, protectCallback: ((Int) -> Boolean)? = this.protectCallback): Boolean {
        if (!isAvailable) {
            resolveClasses()
        }
        if (!isAvailable) return false
        this.protectCallback = protectCallback
        return if (useV2RayPointApi) {
            startV2RayPoint(configJson)
        } else {
            startCoreController(configJson)
        }
    }

    private fun startCoreController(configJson: String): Boolean {
        return try {
            val handler = createCoreControllerHandler()
            val controller = newCoreControllerMethod?.invoke(null, handler) ?: return false
            coreController = controller
            startLoopMethod?.invoke(controller, configJson)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "StartLoop failed", e)
            coreController = null
            false
        }
    }

    private fun startV2RayPoint(configJson: String): Boolean {
        return try {
            val handler = createV2RayPointHandler()
            val point = newV2RayPointMethod?.invoke(null, handler) ?: return false
            coreController = point
            val pointClass = point.javaClass
            findMethod(pointClass, "setPackageCodePath", String::class.java)?.invoke(point, lastEnvPath)
            findMethod(pointClass, "setPackageName", String::class.java)?.invoke(point, "com.vpn.client")
            val runLoopTakesConfig = v2RayPointRunLoopMethod!!.parameterCount == 1
            if (!runLoopTakesConfig) {
                v2RayPointSetConfigMethod?.invoke(point, configJson)
            }
            val configArg = if (runLoopTakesConfig) configJson else null
            // runLoop() در Go بلوک می‌کند؛ در یک رشتهٔ جدا اجرا می‌شود
            runLoopThread = Thread {
                try {
                    if (configArg != null)
                        v2RayPointRunLoopMethod?.invoke(point, configArg)
                    else
                        v2RayPointRunLoopMethod?.invoke(point)
                } catch (e: Throwable) {
                    Log.e(TAG, "RunLoop error", e)
                }
            }.apply { name = "V2RayRunLoop"; start() }
            true
        } catch (e: Throwable) {
            Log.e(TAG, "V2RayPoint start failed", e)
            coreController = null
            false
        }
    }

    fun stop() {
        if (!isAvailable) return
        if (useV2RayPointApi) {
            try {
                coreController?.let { v2RayPointStopLoopMethod?.invoke(it) }
            } catch (e: Throwable) {
                Log.e(TAG, "StopLoop failed", e)
            }
            runLoopThread = null
        } else {
            try {
                coreController?.let { stopLoopMethod?.invoke(it) }
            } catch (e: Throwable) {
                Log.e(TAG, "StopLoop failed", e)
            }
        }
        coreController = null
    }

    private fun createCoreControllerHandler(): Any {
        val h = handlerInterface!!
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any?>?): Any {
                when (method?.name) {
                    "startup", "shutdown", "onEmitStatus" -> return 0
                }
                return 0
            }
        }
        return Proxy.newProxyInstance(h.classLoader, arrayOf(h), handler)
    }

    /** V2RayVPNServiceSupportsSet: Setup(Conf), Prepare(), Shutdown(), Protect(fd), OnEmitStatus(code, msg) */
    private fun createV2RayPointHandler(): Any {
        val h = v2RayPointHandlerInterface!!
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any?>?): Any {
                when (method?.name?.lowercase()) {
                    "setup", "prepare", "shutdown", "onemitstatus" -> return 0
                    "protect" -> {
                        val fd = when (val a = args?.getOrNull(0)) {
                            is Number -> a.toInt()
                            else -> 0
                        }
                        val ok = protectCallback?.invoke(fd) == true
                        return if (ok) 1 else 0
                    }
                }
                return 0
            }
        }
        return Proxy.newProxyInstance(h.classLoader, arrayOf(h), handler)
    }
}
