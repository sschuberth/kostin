package dev.schuberth.kostin.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.platform.MultiplatformSystem.exitProcess

import dev.schuberth.kostin.client.KostalInverterClient

internal object Main : CliktCommand() {
    private val url by option().required()

    init {
        context {
            helpFormatter = { MordantHelpFormatter(context = it, requiredOptionMarker = "*", showDefaultValues = true) }
        }

        subcommands(VersionCommand, LogCommand, RebootCommand)
    }

    override fun run() {
        val client = KostalInverterClient(url)
        currentContext.findOrSetObject { client }
    }
}

fun main(args: Array<String>) {
    Main.main(args)
    exitProcess(0)
}
