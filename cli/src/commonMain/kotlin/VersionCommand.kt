package dev.schuberth.kostin.cli

import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.mordant.rendering.Theme

import dev.schuberth.kostin.client.KostalInverterClient

internal object VersionCommand : UnauthenticatedCommand() {
    override fun runUnauthenticated(client: KostalInverterClient) {
        runCatching {
            client.getVersion()
        }.onSuccess { version ->
            echo(Theme.Default.info("Communicating via '${version.name}' version ${version.apiVersion}."))
            echo(Theme.Default.info("Host '${version.hostname}' has software version ${version.swVersion}."))
        }.onFailure {
            echo(Theme.Default.danger("Failed to get version information: $it"))
            throw ProgramResult(1)
        }
    }
}
