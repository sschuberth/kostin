package dev.schuberth.kostin.client

import io.ktor.client.engine.HttpClientEngine

internal expect fun getHttpClientEngine(): HttpClientEngine
