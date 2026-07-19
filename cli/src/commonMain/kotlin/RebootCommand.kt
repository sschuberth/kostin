package dev.schuberth.kostin.cli

import dev.schuberth.kostin.client.KostalInverterClient
import dev.schuberth.kostin.client.models.TokenResponse

internal object RebootCommand : AuthenticatedCommand() {
    context(session: TokenResponse)
    override fun runAuthenticated(client: KostalInverterClient) = client.reboot()
}
