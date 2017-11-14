package com.tony.remoting.netty;

import com.tony.remoting.absinterface.ChannelEventListener;
import com.tony.remoting.absinterface.RemoteService;
import com.tony.remoting.absinterface.RemotingAbstract;
import com.tony.remoting.protocal.RemoteCommand;
import com.tony.remoting.protocal.body.HelloRequest;
import com.tony.remoting.util.RemotingHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chnho02796 on 2017/10/31.
 */
public class NettyRemoteServer extends RemotingAbstract implements RemoteService {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap bp;
    private int port;
    private ExecutorService publicExecutor;
    private ExecutorService defaultWorkExecute;
    public NettyRemoteServer(){
        this.publicExecutor = Executors.newFixedThreadPool(10, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyServerPublicExecutor_" + this.threadIndex.incrementAndGet());
            }
        });

        this.defaultWorkExecute = Executors.newFixedThreadPool(10, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyServerWorkExecutor_" + this.threadIndex.incrementAndGet());
            }
        });
    }
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

    protected ExecutorService getDefaultWorkingService() {
        return null;
    }

    protected ChannelEventListener getChannelEventListener() {
        return null;
    }


    class NettyServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            RemoteCommand cmd = (RemoteCommand) msg;
            processCMD(ctx,cmd);
        }
    }

    class NettyConnectHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            NettyRemoteServer.this.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddress, ctx.channel()));
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            ctx.close();
            super.channelInactive(ctx);
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            NettyRemoteServer.this.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddress, ctx.channel()));
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                    NettyRemoteServer.this.putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddress, ctx.channel()));
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
            super.exceptionCaught(ctx, cause);
            final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
            NettyRemoteServer.this.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddress, ctx.channel()));
        }
    }
}
