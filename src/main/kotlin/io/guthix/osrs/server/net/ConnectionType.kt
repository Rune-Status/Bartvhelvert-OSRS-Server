/*
OSRS-Server
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

import io.guthix.osrs.server.revision
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.ByteToMessageDecoder
import java.io.IOException

enum class ConnectionType(val opcode: Int) { GAME(14), JS5(15); }

class ConnectionTypeDecoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext?, inc: ByteBuf, out: MutableList<Any>) {
        if (!inc.isReadable) return
        inc.markReaderIndex()
        when (inc.readUnsignedByte().toInt()) {
            ConnectionType.GAME.opcode -> {
                TODO()
            }
            ConnectionType.JS5.opcode -> {
                if (!inc.isReadable(Int.SIZE_BYTES)) {
                    inc.resetReaderIndex()
                    return
                }
                out.add(RevisionHandshakePacket(inc.readInt()))
            }
            else -> throw IOException("Received request for unsupported connection type.")
        }
    }
}

class RevisionHandshakePacket(val revision: Int): IncPacket

class ConnectionTypeHandler : SimpleChannelInboundHandler<IncPacket>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: IncPacket) {
        when(msg) {
            is RevisionHandshakePacket -> {
                ctx.pipeline().addStatusResponseEncoder()
                if(msg.revision != revision) {
                    ctx.channel().writeAndFlush(StatusResponse.OUT_OF_DATE)
                    ctx.close()
                } else {
                    ctx.channel().writeAndFlush(StatusResponse.SUCCESSFUL)
                    ctx.pipeline().swapToJS5()
                }
            }
        }
    }

    private fun ChannelPipeline.addStatusResponseEncoder() = addAfter(
        ConnectionTypeDecoder::class.qualifiedName,
        StatusResponseEncoder::class.qualifiedName,
        StatusResponseEncoder()
    )

    private fun ChannelPipeline.swapToJS5() {
        TODO()
    }
}