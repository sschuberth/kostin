package dev.schuberth.kostin.client

import io.ktor.client.engine.cio.CIOEngineConfig

internal actual fun CIOEngineConfig.trustUnknownCertificates() {
    https {
        // TODO
    }
}
