package com.tony.remoting.netty;

import com.tony.remoting.protocal.RemoteCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by chnho02796 on 2017/10/31.
 */

public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        ByteBuf in = (ByteBuf) msg;
//        byte[] req = new byte[in.readableBytes()];
//        in.readBytes(req);
//        String body = new String(req,"utf-8");
//        System.out.println("收到客户端消息:"+body);
//      ctx.write(Unpooled.copiedBuffer(calrResult.getBytes()));
        RemoteCommand cmd = (RemoteCommand) msg;
        System.out.println("收到客户端消息:"+cmd);
    }

}
