package com.tony.remoting.netty;

import com.tony.remoting.protocal.RemoteCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

import java.io.IOException;

/**
 * Created by chnho02796 on 2017/11/9.
 */
public class MsgPackageEncoding extends MessageToByteEncoder<Object> {

    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        MessagePack pack = new MessagePack();
        byte[] bytes = pack.write(o);
        byteBuf.writeBytes(bytes);
    }

    public static void EncodeBody(Object obj,RemoteCommand cmd) throws IOException {
        MessagePack pack = new MessagePack();
        cmd.setBody(pack.write(obj));
    }
}
