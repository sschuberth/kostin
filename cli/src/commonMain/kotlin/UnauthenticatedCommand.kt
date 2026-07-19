package dev.schuberth.kostin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject

import dev.schuberth.kostin.client.KostalInverterClient

internal abstract class UnauthenticatedCommand : CliktCommand() {
    private val client by requireObject<KostalInverterClient>()

    final override fun run() = runUnauthenticated(client)

    abstract fun runUnauthenticated(client: KostalInverterClient)
}
