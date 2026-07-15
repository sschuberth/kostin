package dev.schuberth.kostin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.Theme

import kotlin.system.exitProcess

object Main : CliktCommand() {
    private val url by option().required()

    private val password by option()
    private val serviceCode by option()

    override fun run() {
        val client = KostalInverterClient(url)

        runCatching {
            client.getVersion()
        }.onSuccess { version ->
            echo(Theme.Default.info("Communicating via '${version.name}' version ${version.apiVersion}."))
            echo(Theme.Default.info("Host '${version.hostname}' has software version ${version.swVersion}."))
        }.onFailure {
            echo(Theme.Default.danger("Failed to get version information."))
            throw ProgramResult(1)
        }

        password?.also {
            client.authenticate(it, serviceCode) {
                downloadLogData()
            }
        }
    }
}

fun main(args: Array<String>) {
    Main.main(args)
    exitProcess(0)
}
