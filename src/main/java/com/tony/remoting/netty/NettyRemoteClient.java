package com.tony.remoting.netty;

import com.tony.remoting.absinterface.InvokeCallback;
import com.tony.remoting.absinterface.RemoteClient;
import com.tony.remoting.absinterface.RemotingAbstract;
import com.tony.remoting.exception.RemotingSendRequestException;
import com.tony.remoting.exception.RemotingTimeoutException;
import com.tony.remoting.exception.RemotingTooMuchRequestException;
import com.tony.remoting.protocal.RemoteCommand;
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

import java.util.concurrent.ExecutorService;

/**
 * Created by chnho02796 on 2017/10/31.
 */
public class NettyRemoteClient extends RemotingAbstract implements RemoteClient {

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

    public void SendRequest(RemoteCommand msg){
        try {
            this.channel.writeAndFlush(msg).sync();
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

    public RemoteCommand sendRequestSync(RemoteCommand request, long timeoutMills) throws RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        return super.sendRequestSync(this.channel, request, timeoutMills);
    }

    public void sendRequestAsync(RemoteCommand request, long timeoutMills, InvokeCallback callback) throws InterruptedException, RemotingTimeoutException, RemotingTooMuchRequestException, RemotingSendRequestException {
        super.sendRequestAsync(this.channel, request, timeoutMills, callback);
    }

    public void sendRequestOneWay(RemoteCommand request, long timeoutMills) throws InterruptedException, RemotingTimeoutException, RemotingTooMuchRequestException, RemotingSendRequestException {
        super.sendRequestOneWay(this.channel, request, timeoutMills);
    }

    protected ExecutorService getcallbackExecuteService() {
        return null;
    }


    class NettyClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            RemoteCommand cmd = (RemoteCommand) msg;
            processCMD(ctx,cmd);
        }
    }
}
