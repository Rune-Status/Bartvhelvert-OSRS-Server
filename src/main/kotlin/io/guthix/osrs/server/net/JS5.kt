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

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import java.io.IOException

enum class JS5MessageType(val opcode: Int) {
    NORMAL_FILE_REQUEST(0),
    PRIORITY_FILE_REQUEST(1),
    CLIENT_LOGGED_IN(2),
    CLIENT_LOGGED_OUT(3),
    ENCRYPTION_KEY_UPDATE(4);
}

class JS5Decoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, inc: ByteBuf, out: MutableList<Any>) {
        if (!inc.isReadable(Int.SIZE_BYTES)) return
        when (inc.readUnsignedByte().toInt()) {
            JS5MessageType.NORMAL_FILE_REQUEST.opcode, JS5MessageType.PRIORITY_FILE_REQUEST.opcode -> {
                val indexId = inc.readUnsignedByte().toInt()
                val archiveId = inc.readUnsignedShort()
                out.add(JS5FileRequest(indexId, archiveId))
            }

            JS5MessageType.CLIENT_LOGGED_IN.opcode, JS5MessageType.CLIENT_LOGGED_OUT.opcode -> {
                val statusCode = inc.readUnsignedMedium()
                if (statusCode != 0) {
                    ctx.close()
                }
            }

            JS5MessageType.ENCRYPTION_KEY_UPDATE.opcode -> {
                ctx.channel().pipeline().get(XOREncoder::class.java).key = inc.readUnsignedByte().toInt()
                inc.skipBytes(2)
            }

            else -> throw IOException("Received unsupported JS5 Message")
        }
    }
}

class JS5FileRequest(val indexId: Int, val archiveId: Int) : IncPacket

class JS5Handler : ForcableChannelInboundHandler<JS5FileRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: JS5FileRequest) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class JS5Encoder : MessageToByteEncoder<JS5FileRequest>() {
    override fun encode(ctx: ChannelHandlerContext, msg: JS5FileRequest, out: ByteBuf) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class XOREncoder : MessageToByteEncoder<ByteBuf>() {
    var key = 0

    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        while (msg.isReadable) {
            out.writeByte(msg.readUnsignedByte().toInt() xor key)
        }
    }
}
