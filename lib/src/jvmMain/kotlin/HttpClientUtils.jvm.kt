package dev.schuberth.kostin.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

import java.security.SecureRandom
import java.security.cert.X509Certificate

import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

internal actual fun getHttpClientEngine(): HttpClientEngine = OkHttp.create {
    config {
        val trustAllCerts = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
        }

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, arrayOf(trustAllCerts), SecureRandom())
        }

        sslSocketFactory(sslContext.socketFactory, trustAllCerts)
        hostnameVerifier { _, _ -> true }
    }
}
