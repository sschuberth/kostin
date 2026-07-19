package dev.schuberth.kostin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

import dev.schuberth.kostin.client.KostalInverterClient
import dev.schuberth.kostin.client.models.TokenResponse

internal abstract class AuthenticatedCommand : CliktCommand() {
    private val password by option().required()
    private val serviceCode by option()

    private val client by requireObject<KostalInverterClient>()

    final override fun run() = client.authenticate(password, serviceCode) {
        runAuthenticated(client)
    }

    context(session: TokenResponse)
    abstract fun runAuthenticated(client: KostalInverterClient)
}
