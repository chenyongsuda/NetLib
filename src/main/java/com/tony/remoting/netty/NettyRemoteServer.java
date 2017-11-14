package com.tony.remoting.netty;

import com.tony.remoting.absinterface.RemoteService;
import com.tony.remoting.absinterface.RemotingAbstract;
import com.tony.remoting.protocal.RemoteCommand;
import com.tony.remoting.protocal.body.HelloRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

/**
 * Created by chnho02796 on 2017/10/31.
 */
public class NettyRemoteServer extends RemotingAbstract implements RemoteService {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap bp;
    private int port;
    public void start() {
        bossGroup   = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        bp          = new ServerBootstrap();
        ServerBootstrap childHandler = bp.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
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
                                ch.pipeline().addLast(new NettyServerHandler());
                            }
                        });
        try {
            ChannelFuture f = bp.bind(port).sync();
            InetSocketAddress addr = (InetSocketAddress) f.channel().localAddress();
            this.port = addr.getPort();
            System.out.println("服务器开启："+port);
        } catch (Exception ex){
            System.out.println("Server start bind errors port:" + port);
        }

    }

    public void stop() {
        try {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (Exception ex){
            System.out.println("Server stop error" + port);
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    protected ExecutorService getcallbackExecuteService() {
        return null;
    }


    class NettyServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            RemoteCommand cmd = (RemoteCommand) msg;
            processCMD(ctx,cmd);
        }
    }
}
