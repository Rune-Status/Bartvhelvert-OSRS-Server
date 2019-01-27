/*
Copyright (C) 2019 Guthix

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.guthix.osrs.server.net

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun networkBootstrap(port: Int) {
    val bossGroup = NioEventLoopGroup(1)
    val loopGroup = NioEventLoopGroup()
    try {
        ServerBootstrap().apply {
            group(bossGroup, loopGroup)
            channel(NioServerSocketChannel()::class.java)
            childOption(ChannelOption.SO_KEEPALIVE, true)
            childOption(ChannelOption.TCP_NODELAY, true)
            childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark(8192, 131072))
            childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(channel: SocketChannel) {
                    channel.pipeline().addLast(ConnectionTypeDecoder::class.qualifiedName, ConnectionTypeDecoder())
                    channel.pipeline().addLast(ConnectionTypeHandler::class.qualifiedName, ConnectionTypeHandler())
                }
            })
        }.bind(port).sync().apply {
            addListener {
                if (it.isSuccess) {
                    logger.info("Server now listening to port $port.")
                } else {
                    logger.error("Server failed to connect to port $port.")
                    it.cause().printStackTrace()
                }
            }
            channel().closeFuture().sync()
        }
    } finally {
        bossGroup.shutdownGracefully()
        loopGroup.shutdownGracefully()
    }
}

interface IncPacket

abstract class ForcableChannelInboundHandler<P : IncPacket> : SimpleChannelInboundHandler<P>() {
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (!cause.message.equals("An existing connection was forcibly closed by the remote host")) {
            cause.printStackTrace()
        }
        ctx.close()
    }
}
