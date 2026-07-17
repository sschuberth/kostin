package dev.schuberth.kostin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.platform.MultiplatformSystem.exitProcess

import dev.schuberth.kostin.client.KostalInverterClient

object Main : CliktCommand() {
    private val url by option().required()

    private val password by option()
    private val serviceCode by option()

    private val command by argument()

    init {
        context {
            helpFormatter = { MordantHelpFormatter(context = it, requiredOptionMarker = "*", showDefaultValues = true) }
        }
    }

    override fun run() {
        val client = KostalInverterClient(url)

        runCatching {
            client.getVersion()
        }.onSuccess { version ->
            echo(Theme.Default.info("Communicating via '${version.name}' version ${version.apiVersion}."))
            echo(Theme.Default.info("Host '${version.hostname}' has software version ${version.swVersion}."))
        }.onFailure {
            echo(Theme.Default.danger("Failed to get version information: $it"))
            throw ProgramResult(1)
        }

        val result = password?.let {
            client.authenticate(it, serviceCode) {
                when (command) {
                    "log" -> downloadLogData()
                    "reboot" -> reboot()
                    else -> throw UsageError("Invalid authenticated command argument '$command'.")
                }
            }
        } ?: with(client) {
            when (command) {
                "version" -> getVersion()
                else -> throw UsageError("Invalid unauthenticated command argument '$command'.")
            }
        }

        echo(result)
    }
}

fun main(args: Array<String>) {
    Main.main(args)
    exitProcess(0)
}
