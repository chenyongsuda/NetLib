package com.tony.remoting.netty;

import com.tony.remoting.absinterface.RemoteService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * Created by chnho02796 on 2017/10/31.
 */
public class NettyRemoteServer implements RemoteService {
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
}
