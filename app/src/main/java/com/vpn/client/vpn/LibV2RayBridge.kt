package com.vpn.client.vpn

import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * پل انعکاسی به libv2ray (AndroidLibV2rayLite).
 * وقتی libv2ray.aar در app/libs باشد، هستهٔ V2Ray با InitCoreEnv، CoreController و StartLoop/StopLoop راه‌اندازی می‌شود.
 * در صورت نبود AAR، [isAvailable] false است و [start] هیچ کاری نمی‌کند.
 */
object LibV2RayBridge {

    private const val TAG = "LibV2RayBridge"

    private var coreController: Any? = null
    private var initCoreEnvMethod: Method? = null
    private var newCoreControllerMethod: Method? = null
    private var startLoopMethod: Method? = null
    private var stopLoopMethod: Method? = null
    private var handlerInterface: Class<*>? = null

    private val packageNames = arrayOf("go.libv2ray", "libv2ray")

    val isAvailable: Boolean
        get() = initCoreEnvMethod != null && newCoreControllerMethod != null &&
                startLoopMethod != null && stopLoopMethod != null && handlerInterface != null

    init {
        resolveClasses()
    }

    private fun findMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Method? {
        return try { clazz.getMethod(name, *paramTypes) } catch (_: Exception) { null }
    }

    private fun resolveClasses() {
        for (pkg in packageNames) {
            try {
                val libClass = Class.forName("$pkg.Libv2ray")
                val controllerClass = Class.forName("$pkg.CoreController")
                val handler = Class.forName("$pkg.CoreCallbackHandler")
                handlerInterface = handler

                initCoreEnvMethod = findMethod(libClass, "initCoreEnv", String::class.java, String::class.java)
                    ?: libClass.getMethod("InitCoreEnv", String::class.java, String::class.java)
                newCoreControllerMethod = findMethod(libClass, "newCoreController", handler)
                    ?: libClass.getMethod("NewCoreController", handler)
                startLoopMethod = findMethod(controllerClass, "startLoop", String::class.java)
                    ?: controllerClass.getMethod("StartLoop", String::class.java)
                stopLoopMethod = findMethod(controllerClass, "stopLoop")
                    ?: controllerClass.getMethod("StopLoop")

                Log.i(TAG, "libv2ray resolved: $pkg")
                return
            } catch (e: Throwable) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Package $pkg not found: ${e.message}")
                }
            }
        }
        Log.w(TAG, "libv2ray AAR not found; put libv2ray.aar in app/libs")
    }

    /**
     * محیط هسته را با مسیر asset و کلید مقداردهی اولیه می‌کند.
     */
    fun initCoreEnv(envPath: String, key: String) {
        if (!isAvailable) return
        try {
            initCoreEnvMethod?.invoke(null, envPath, key)
        } catch (e: Throwable) {
            Log.e(TAG, "InitCoreEnv failed", e)
        }
    }

    /**
     * هسته را با کانفیگ JSON راه می‌اندازد.
     * @return true اگر StartLoop با موفقیت اجرا شد
     */
    fun start(configJson: String): Boolean {
        if (!isAvailable) return false
        try {
            val handler = createCallbackHandler()
            val controller = newCoreControllerMethod?.invoke(null, handler) ?: return false
            coreController = controller
            startLoopMethod?.invoke(controller, configJson)
            return true
        } catch (e: Throwable) {
            Log.e(TAG, "StartLoop failed", e)
            coreController = null
            return false
        }
    }

    fun stop() {
        if (!isAvailable) return
        try {
            coreController?.let { stopLoopMethod?.invoke(it) }
        } catch (e: Throwable) {
            Log.e(TAG, "StopLoop failed", e)
        }
        coreController = null
    }

    private fun createCallbackHandler(): Any {
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any?>?): Any {
                when (method?.name) {
                    "startup" -> return 0
                    "shutdown" -> return 0
                    "onEmitStatus" -> return 0
                }
                return 0
            }
        }
        return Proxy.newProxyInstance(
            handlerInterface!!.classLoader,
            arrayOf(handlerInterface),
            handler
        )
    }
}
