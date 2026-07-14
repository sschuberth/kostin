package dev.schuberth.kostin.cli

import at.asitplus.signum.indispensable.Digest as SignumDigest
import at.asitplus.signum.indispensable.HMAC
import at.asitplus.signum.indispensable.kdf.PBKDF2
import at.asitplus.signum.supreme.hash.digest
import at.asitplus.signum.supreme.kdf.deriveKey
import at.asitplus.signum.supreme.mac.mac

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.Theme

import dev.schuberth.kostin.client.apis.AuthApi
import dev.schuberth.kostin.client.apis.InfoApi
import dev.schuberth.kostin.client.infrastructure.ApiClient
import dev.schuberth.kostin.client.models.AuthClientFinal
import dev.schuberth.kostin.client.models.AuthClientFirst
import dev.schuberth.kostin.client.models.Version

import io.ktor.client.engine.okhttp.OkHttp

import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate

import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

import kotlin.experimental.xor
import kotlin.io.encoding.Base64
import kotlin.system.exitProcess
import kotlin.random.Random

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    Main.main(args)
    exitProcess(0)
}

object Main : CliktCommand() {
    private val url by option().required()
    private val password by option()

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

        password?.also { authenticate(password = it) }
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

    private fun authenticate(username: String = "user", password: String): Result<String> {
        val api = AuthApi(baseUrl = apiUrl, httpClientEngine = engine)

        return runCatching {
            runBlocking {
                // 12 bytes provide enough entropy and result in 16 chars Base64-encoded without any padding.
                val nonce = Base64.encode(Random.nextBytes(12))

                // Installers need to use "master" as the username.
                val resultStart = api.postAuthStart(AuthClientFirst(username, nonce))

                if (!resultStart.success) {
                    throw IOException("Failed to start authentication: ${resultStart.response.status}")
                }

                val auth = resultStart.body()

                val kdf = PBKDF2.HMAC_SHA256(auth.rounds)
                val saltedPassword = kdf.deriveKey(
                    Base64.decode(auth.salt),
                    password.encodeToByteArray(),
                    kdf.pbkdf2.prf.digest.outputLength
                ).getOrThrow()

                val clientKey = HMAC.SHA256.mac(saltedPassword, "Client Key".encodeToByteArray()).getOrThrow()

                val storedKey = SignumDigest.SHA256.digest(clientKey)
                val authMessage = listOf(
                    "n=$username",
                    "r=$nonce",
                    "r=${auth.nonce}",
                    "s=${auth.salt}",
                    "i=${auth.rounds}",
                    "c=biws",
                    "r=${auth.nonce}"
                ).joinToString(",")

                val clientSignature = HMAC.SHA256.mac(storedKey, authMessage.encodeToByteArray()).getOrThrow()
                val proof = clientKey.zip(clientSignature) { a, b -> a xor b }.toByteArray()

                val resultFinish = api.postAuthFinish(AuthClientFinal(auth.transactionId, Base64.encode(proof)))

                if (!resultFinish.success) {
                    throw IOException("Failed to finish authentication: ${resultFinish.response.status}")
                }

                resultFinish.body().token
            }
        }
    }
}
