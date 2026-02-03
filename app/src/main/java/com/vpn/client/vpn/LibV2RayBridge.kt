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
            useV2RayPointApi -> newV2RayPointMethod != null && v2RayPointSetConfigMethod != null &&
                    v2RayPointRunLoopMethod != null && v2RayPointStopLoopMethod != null && v2RayPointHandlerInterface != null
            else -> initCoreEnvMethod != null && newCoreControllerMethod != null &&
                    startLoopMethod != null && stopLoopMethod != null && handlerInterface != null
        }

    init {
        resolveClasses()
    }

    private fun findMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Method? {
        return try { clazz.getMethod(name, *paramTypes) } catch (_: Exception) { null }
    }

    private fun resolveClasses() {
        // اول API نوع اول (go.libv2ray با CoreController) را امتحان کن
        try {
            val pkg = "go.libv2ray"
            val libClass = Class.forName("$pkg.Libv2ray")
            val controllerClass = Class.forName("$pkg.CoreController")
            val handler = Class.forName("$pkg.CoreCallbackHandler")
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
            val libClass = Class.forName("$pkg.Libv2ray")
            val pointClass = Class.forName("$pkg.V2RayPoint")
            val handlerClass = Class.forName("$pkg.V2RayVPNServiceSupportsSet")
            v2RayPointHandlerInterface = handlerClass
            newV2RayPointMethod = findMethod(libClass, "newV2RayPoint", handlerClass)
                ?: findMethod(libClass, "NewV2RayPoint", handlerClass)
            v2RayPointSetConfigMethod = findMethod(pointClass, "setConfigureFileContent", String::class.java)
                ?: findMethod(pointClass, "setConfigurefilecontent", String::class.java)
            v2RayPointRunLoopMethod = findMethod(pointClass, "runLoop")
                ?: findMethod(pointClass, "RunLoop")
            v2RayPointStopLoopMethod = findMethod(pointClass, "stopLoop")
                ?: findMethod(pointClass, "StopLoop")
            if (newV2RayPointMethod != null && v2RayPointSetConfigMethod != null &&
                v2RayPointRunLoopMethod != null && v2RayPointStopLoopMethod != null
            ) {
                useV2RayPointApi = true
                Log.i(TAG, "libv2ray resolved (V2RayPoint API): $pkg")
                return
            }
        } catch (e: Throwable) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "libv2ray V2RayPoint API not found: ${e.message}")
            }
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
     * @return true اگر راه‌اندازی با موفقیت شروع شد
     */
    fun start(configJson: String): Boolean {
        if (!isAvailable) return false
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
            v2RayPointSetConfigMethod?.invoke(point, configJson)
            // runLoop() در Go بلوک می‌کند؛ در یک رشتهٔ جدا اجرا می‌شود
            runLoopThread = Thread {
                try {
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
                when (method?.name) {
                    "setup", "prepare", "shutdown", "protect", "onEmitStatus" -> return 0
                }
                return 0
            }
        }
        return Proxy.newProxyInstance(h.classLoader, arrayOf(h), handler)
    }
}
