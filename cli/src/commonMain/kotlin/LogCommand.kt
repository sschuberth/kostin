package dev.schuberth.kostin.cli

import dev.schuberth.kostin.client.KostalInverterClient
import dev.schuberth.kostin.client.models.TokenResponse

internal object LogCommand : AuthenticatedCommand() {
    context(session: TokenResponse)
    override fun runAuthenticated(client: KostalInverterClient) {
        val log = client.downloadLogData()
        echo(log)
    }
}
