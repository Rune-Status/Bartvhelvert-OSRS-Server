package io.guthix.osrs.server

import io.guthix.osrs.server.net.networkBootstrap

val revision = 177

val port = 43594

fun main(args: Array<String>) {
    networkBootstrap(port)
}