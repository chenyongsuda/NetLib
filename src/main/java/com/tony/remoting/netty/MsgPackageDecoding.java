package com.tony.remoting.netty;

import com.tony.remoting.protocal.RemoteCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.msgpack.MessagePack;

import java.io.IOException;
import java.util.List;

/**
 * Created by chnho02796 on 2017/11/9.
 */
public class MsgPackageDecoding extends MessageToMessageDecoder<ByteBuf> {
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        //From ByteBuf To Object
        int lenght = byteBuf.readableBytes();
        byte[] content = new byte[lenght];
        byteBuf.getBytes(byteBuf.readerIndex(),content,0,lenght);
        MessagePack pack = new MessagePack();
        list.add(pack.read(content,RemoteCommand.class));
    }

    public static <T> T DecodeBody(Class<T> cls,RemoteCommand cmd) throws IOException {
        MessagePack pack = new MessagePack();
        return (T)pack.read(cmd.getBody(),cls);
    }
}
