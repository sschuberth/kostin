package dev.schuberth.kostin.client

import dev.schuberth.kostin.client.apis.AuthApi
import dev.schuberth.kostin.client.apis.InfoApi
import dev.schuberth.kostin.client.apis.LogdataApi
import dev.schuberth.kostin.client.apis.SystemApi
import dev.schuberth.kostin.client.infrastructure.ApiClient
import dev.schuberth.kostin.client.models.AuthClientFinal
import dev.schuberth.kostin.client.models.AuthClientFirst
import dev.schuberth.kostin.client.models.AuthCreateSessionRequest
import dev.schuberth.kostin.client.models.DownloadRequest
import dev.schuberth.kostin.client.models.TokenResponse
import dev.schuberth.kostin.client.models.Version

import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.time.Clock

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.IOException

/**
 * A client for the REST-based API V2 for PIKO IQ and PLENTICORE plus inverters.
 */
class KostalInverterClient(baseUrl: String) {
    private val apiUrl = "${baseUrl.removeSuffix("/")}${ApiClient.BASE_URL}"
    private val engine by lazy { getHttpClientEngine() }

    fun getVersion(): Version {
        val api = InfoApi(baseUrl = apiUrl, httpClientEngine = engine)

        return runBlocking {
            val result = api.getInfo()

            if (!result.success) throw IOException("Failed to get version: ${result.response.status}")

            result.body()
        }
    }

    fun <T> authenticate(
        password: String,
        serviceCode: String?,
        block: context(TokenResponse) KostalInverterClient.() -> T
    ): T = with(login(password, serviceCode)) {
        block().also { logout() }
    }

    private fun login(password: String, serviceCode: String?): TokenResponse {
        val api = AuthApi(baseUrl = apiUrl, httpClientEngine = engine)
        val user = if (serviceCode != null) User.master else User.user
        val crypto = CryptoHelper.get()

        return runBlocking {
            // 12 bytes provide enough entropy and result in 16 chars Base64-encoded without any padding.
            val nonce = Base64.encode(Random.nextBytes(12))

            // Installers need to use "master" as the username.
            val resultStart = api.postAuthStart(AuthClientFirst(user.name, nonce))

            if (!resultStart.success) {
                throw IOException("Failed to start authentication: ${resultStart.response.status}")
            }

            val authStart = resultStart.body()
            val saltedPassword = crypto.getSaltedPassword(authStart, password)
            val authMessage = crypto.getAuthMessage(user, nonce, authStart)
            val proof = crypto.getProof(authMessage, saltedPassword)
            val resultFinish = api.postAuthFinish(AuthClientFinal(authStart.transactionId, Base64.encode(proof.proof)))

            if (!resultFinish.success) {
                throw IOException("Failed to finish authentication: ${resultFinish.response.status}")
            }

            val authFinal = resultFinish.body()

            if (!crypto.checkServerSignature(authFinal, saltedPassword, proof.authMessage)) {
                throw IOException("Server signature verification failed.")
            }

            val token = when (user) {
                User.user -> authFinal.token
                User.master -> "${authFinal.token}:$serviceCode"
            }

            val box = crypto.encryptSessionToken(token, proof)

            val request = AuthCreateSessionRequest(
                authStart.transactionId,
                Base64.encode(box.nonce),
                Base64.encode(box.authTag),
                Base64.encode(box.encryptedData)
            )
            val resultSession = api.postAuthCreateSession(request)

            if (!resultSession.success) {
                throw IOException("Failed to create session: ${resultSession.response.status}")
            }

            resultSession.body()
        }
    }

    context(session: TokenResponse)
    private fun authConfig(): HttpClientConfig<*>.() -> Unit = {
        defaultRequest {
            // Calling `AuthApi.setBearerToken()` throws "No Bearer authentication configured" but there seems to be
            // no way to set it, so set it manually.
            header(HttpHeaders.Authorization, "Bearer ${session.token}")
        }

        install(HttpTimeout) {
            @Suppress("MagicNumber")
            socketTimeoutMillis = 30_000
        }
    }

    context(session: TokenResponse)
    private fun logout() {
        val api = AuthApi(baseUrl = apiUrl, httpClientEngine = engine, httpClientConfig = authConfig())

        runBlocking {
            val result = api.postLogout()
            if (!result.success) throw IOException("Failed to logout: ${result.response.status}")
        }
    }

    context(session: TokenResponse)
    fun downloadLogData(begin: LocalDate? = null, end: LocalDate? = null): String {
        val api = LogdataApi(baseUrl = apiUrl, httpClientEngine = engine, httpClientConfig = authConfig())

        return runBlocking {
            val request = when {
                begin == null && end == null -> {
                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    DownloadRequest(today, today)
                }

                begin == null && end != null -> DownloadRequest(end, end)

                begin != null && end == null -> DownloadRequest(begin, begin)

                else -> DownloadRequest(begin, end)
            }

            val result = api.postLogdataDownload(request)
            if (!result.success) throw IOException("Failed to download log data: ${result.response.status}")

            result.response.body<String>()
        }
    }

    context(session: TokenResponse)
    fun reboot() {
        val api = SystemApi(baseUrl = apiUrl, httpClientEngine = engine, httpClientConfig = authConfig())

        runBlocking {
            val result = api.postReboot()
            if (!result.success) throw IOException("Failed to reboot: ${result.response.status}")
        }
    }
}
