package dev.schuberth.kostin.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.curl.Curl

internal actual fun getHttpClientEngine(): HttpClientEngine = Curl.create {
    sslVerify = false
}
