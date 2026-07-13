package dev.schuberth.kostin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.Theme

import dev.schuberth.kostin.client.apis.InfoApi
import dev.schuberth.kostin.client.infrastructure.ApiClient

import io.ktor.client.engine.okhttp.OkHttp

import java.security.SecureRandom
import java.security.cert.X509Certificate

import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

import kotlin.system.exitProcess

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    Main.main(args)
    exitProcess(0)
}

object Main : CliktCommand() {
    private val url by option().required()

    override fun run() {
        val engine = OkHttp.create {
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

        val api = InfoApi(
            baseUrl = "${url.removeSuffix("/")}${ApiClient.BASE_URL}",
            httpClientEngine = engine
        )

        runBlocking {
            val result = api.getInfo()

            if (result.success) {
                val version = result.body()
                echo(Theme.Default.success(version.toString()))
            } else {
                echo(Theme.Default.warning(result.response.status.toString()))
            }
        }
    }
}
