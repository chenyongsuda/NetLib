package com.tony.remoting.absinterface;

import com.tony.remoting.exception.RemotingSendRequestException;
import com.tony.remoting.exception.RemotingTimeoutException;
import com.tony.remoting.netty.MsgPackageDecoding;
import com.tony.remoting.protocal.RemoteCommand;
import com.tony.remoting.protocal.body.HelloRequest;
import com.tony.remoting.util.RemotingHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chnho02796 on 2017/11/13.
 */
public abstract class RemotingAbstract {
    public static AtomicInteger requestId = new AtomicInteger(0);
    /**
     * This map caches all on-going requests.
     */
    protected final ConcurrentMap<Integer /* opaque */, ResponseFuture> responseTable =
            new ConcurrentHashMap<Integer, ResponseFuture>(256);


    public RemoteCommand sendRequestSync(Channel ch, RemoteCommand request, long timeoutMills) throws RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        final int reqID = request.getReqID();
        final ResponseFuture rf = new ResponseFuture(reqID, timeoutMills);
        final SocketAddress addr = ch.remoteAddress();
        responseTable.put(reqID, rf);
        try {
            ch.writeAndFlush(request).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        rf.setSendFinish(true);
                        return;
                    } else {
                        rf.setSendFinish(false);
                    }

                    responseTable.remove(reqID);
                    rf.putResponse(null);
                    rf.setCause(channelFuture.cause());
                }
            });

            RemoteCommand cmd = rf.waitResponse();
            if (null == cmd){
                if (rf.isSendFinish()){
                    throw new RemotingSendRequestException(RemotingHelper.parseSocketAddressAddr(addr),rf.getCause());
                }
                else{
                    throw new RemotingTimeoutException(RemotingHelper.parseSocketAddressAddr(addr),timeoutMills,rf.getCause());
                }
            }
            return cmd;
        }finally {
            responseTable.remove(reqID);
        }
    }

    public void sendRequestAsync(Channel ch,RemoteCommand request, long timeoutMills,
                                 InvokeCallback callback){

    }

    public void sendRequestOneWay(Channel ch,RemoteCommand request, long timeoutMills){

    }

    public void processCMD(ChannelHandlerContext ctx, RemoteCommand msg) throws IOException {
        if (msg.isReq()){
            processRequest(ctx,msg);
        }
        else{
            processResponse(ctx,msg);
        }
    }
    public void processRequest(ChannelHandlerContext ctx, RemoteCommand msg) throws IOException {
        if (msg.getType().equals("1")){
            HelloRequest req = MsgPackageDecoding.DecodeBody(HelloRequest.class,msg);
            try {
                Thread.sleep(1900);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            msg.setReq(false);
            ctx.writeAndFlush(msg);
        }
    }

    public void processResponse(ChannelHandlerContext ctx, RemoteCommand msg) throws IOException {
        int req = msg.getReqID();
        ResponseFuture rf = responseTable.get(req);
        if (null != rf) {
            responseTable.remove(req);
            rf.putResponse(msg);
        }
    }
}
