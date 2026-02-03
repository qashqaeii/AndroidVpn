package com.vpn.client.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vpn.client.di.SecurityConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiModule {

    /** آدرس سرور API — باید با ALLOWED_HOSTS و CORS در بک‌اند یکی باشد (اسلش انتهایی الزامی). */
    private const val BASE_URL = "http://77.110.116.139:8000/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun createServersApi(okHttp: OkHttpClient): ServersApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ServersApi::class.java)
    }

    fun createOkHttpClient(securityConfig: SecurityConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
        securityConfig.applyCertificatePinning(builder)
        if (securityConfig.isDebugLogging) {
            builder.addInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            )
        }
        return builder.build()
    }
}
