package com.tony.remoting.netty;

import com.tony.remoting.absinterface.RemoteClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created by chnho02796 on 2017/10/31.
 */
public class NettyRemoteClient implements RemoteClient {

    private EventLoopGroup eventLoopGroupWorker;
    private Bootstrap bootstrap;
    private String host;
    private int port;
    private Channel channel;


    public void start() {
        bootstrap = new Bootstrap();
        eventLoopGroupWorker = new NioEventLoopGroup();
        Bootstrap handler = this.bootstrap.group(this.eventLoopGroupWorker)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .option(ChannelOption.SO_SNDBUF, 1024)
                .option(ChannelOption.SO_RCVBUF, 1024)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                                //Decode
                                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024,0,2,0,2));
                                ch.pipeline().addLast(new MsgPackageDecoding());

                                //Encode
                                ch.pipeline().addLast(new LengthFieldPrepender(2));
                                ch.pipeline().addLast(new MsgPackageEncoding());
                                ch.pipeline().addLast(new IdleStateHandler(0,0,5));
                                ch.pipeline().addLast(new NettyConnectHandler());
                                ch.pipeline().addLast(new NettyClientHandler());
                    }
                });

        try {
            ChannelFuture f = this.bootstrap.connect(host, port).sync();
            this.channel = f.channel();
        } catch (InterruptedException e) {
            System.out.println("Client link to server error host:"+host+" port:" + port);
            stop();
        }
    }

    public void SendRequest(String msg){
        try {
            this.channel.writeAndFlush(getSendByteBuf(msg)).sync();
        } catch (Exception e) {
            System.out.println("send message error");
            e.printStackTrace();
        }
    }

    private ByteBuf getSendByteBuf(String message)
            throws Exception {
        byte[] req = message.getBytes("UTF-8");
        ByteBuf pingMessage = Unpooled.buffer();
        pingMessage.writeBytes(req);

        return pingMessage;
    }

    public void stop() {
        try {
            eventLoopGroupWorker.shutdownGracefully();
        } catch (Exception ex){
            System.out.println("Client stop error");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
