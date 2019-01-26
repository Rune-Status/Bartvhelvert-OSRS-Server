package io.guthix.osrs.server.net

import io.guthix.osrs.server.revision
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
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
                ctx.pipeline().addAfter(
                    ConnectionTypeDecoder::class.qualifiedName,
                    StatusResponseEncoder::class.qualifiedName,
                    StatusResponseEncoder()
                )
                if(msg.revision != revision) {
                    ctx.channel().writeAndFlush(StatusResponse.OUT_OF_DATE)
                    ctx.close()
                } else {
                    ctx.channel().writeAndFlush(StatusResponse.SUCCESSFUL)
                }
            }
        }
    }
}