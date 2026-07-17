package dev.schuberth.kostin.client

import io.ktor.client.engine.cio.CIOEngineConfig

import java.security.cert.X509Certificate

import javax.net.ssl.X509TrustManager

internal actual fun CIOEngineConfig.trustUnknownCertificates() {
    https {
        trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }
}
