package dev.schuberth.kostin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.Theme

import dev.schuberth.kostin.client.apis.InfoApi
import dev.schuberth.kostin.client.infrastructure.ApiClient
import dev.schuberth.kostin.client.models.Version

import io.ktor.client.engine.okhttp.OkHttp

import java.io.IOException
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

    private val apiUrl by lazy { "${url.removeSuffix("/")}${ApiClient.BASE_URL}" }

    private val engine by lazy {
        OkHttp.create {
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
    }

    override fun run() {
        getVersion().onSuccess { version ->
            echo(Theme.Default.info("Communicating via '${version.name}' version ${version.apiVersion}."))
            echo(Theme.Default.info("Host '${version.hostname}' has software version ${version.swVersion}."))
        }.onFailure {
            echo(Theme.Default.danger("Failed to get version information."))
            throw ProgramResult(1)
        }
    }

    private fun getVersion(): Result<Version> {
        val api = InfoApi(baseUrl = apiUrl, httpClientEngine = engine)

        return runCatching {
            runBlocking {
                val result = api.getInfo()

                if (!result.success) throw IOException("Failed to get version: ${result.response.status}")

                result.body()
            }
        }
    }
}
