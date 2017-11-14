package com.tony.remoting.absinterface;

import com.tony.remoting.common.Pair;
import com.tony.remoting.exception.RemotingSendRequestException;
import com.tony.remoting.exception.RemotingTimeoutException;
import com.tony.remoting.exception.RemotingTooMuchRequestException;
import com.tony.remoting.netty.NettyEvent;
import com.tony.remoting.protocal.RemoteCommand;
import com.tony.remoting.util.RemotingHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chnho02796 on 2017/11/13.
 */
public abstract class RemotingAbstract {

    public static AtomicInteger requestId = new AtomicInteger(0);
    protected Semaphore semaphoreOneway;
    protected Semaphore semaphoreAsync;

    private final Timer timer = new Timer("CleanResponseTimer", true);
    protected abstract  ExecutorService getcallbackExecuteService();
    protected abstract  ExecutorService getDefaultWorkingService();
    protected abstract  ChannelEventListener getChannelEventListener();
    protected final NettyEventExecutor eventExecutor = new NettyEventExecutor();
    /**
     * This map caches all on-going requests.
     */
    protected final ConcurrentMap<Integer /* opaque */, ResponseFuture> responseTable =
            new ConcurrentHashMap<Integer, ResponseFuture>(256);

    protected final HashMap<Integer/* request code */, Pair<RemoteRequestProcessor, ExecutorService>> processorTable =
            new HashMap<Integer, Pair<RemoteRequestProcessor, ExecutorService>>(64);

    public RemotingAbstract(){
        semaphoreOneway = new Semaphore(28);
        semaphoreAsync = new Semaphore(256);
        timer.scheduleAtFixedRate(new TimerTask() {
                                      @Override
                                      public void run() {
                                          try {
                                              RemotingAbstract.this.scanResponseTable();
                                          }catch (Exception ex){

                                          }
                                      }
                                  }
                , 3 * 1000, 1000);
    }

    public void startEventExecute(){
        this.eventExecutor.start();
    }

    public void putNettyEvent(final NettyEvent event){
        this.eventExecutor.putNettyEvent(event);
    }

    public void registerProcessor(int requestCode, RemoteRequestProcessor processor, ExecutorService executor) {
        Pair<RemoteRequestProcessor, ExecutorService> pair = new Pair<RemoteRequestProcessor, ExecutorService>(processor, executor);
        this.processorTable.put(requestCode, pair);
    }
    public void registerDefaultProcessor(int requestCode, RemoteRequestProcessor processor) {
        Pair<RemoteRequestProcessor, ExecutorService> pair = new Pair<RemoteRequestProcessor, ExecutorService>(processor, null);
        this.processorTable.put(requestCode, pair);
    }

    public void scanResponseTable() {
        List<ResponseFuture> list = new LinkedList<ResponseFuture>();
        Iterator<Map.Entry<Integer, ResponseFuture>> it = this.responseTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ResponseFuture> next = it.next();
            ResponseFuture rep = next.getValue();

            if (rep.isTimeout()) {
                rep.release();
                it.remove();
                list.add(rep);
            }
        }

        for (ResponseFuture rf : list) {
            executeInvokeCallBack(rf);
        }
    }

    public RemoteCommand sendRequestSync(Channel ch, RemoteCommand request, long timeoutMills) throws RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        final int reqID = request.getReqID();
        final ResponseFuture rf = new ResponseFuture(reqID, timeoutMills,null,null);
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
                    throw new RemotingTimeoutException(RemotingHelper.parseSocketAddressAddr(addr),timeoutMills,rf.getCause());
                }
                else{
                    throw new RemotingSendRequestException(RemotingHelper.parseSocketAddressAddr(addr),rf.getCause());
                }
            }
            return cmd;
        }finally {
            responseTable.remove(reqID);
        }
    }

    public void sendRequestAsync(Channel ch,RemoteCommand request, long timeoutMills,
                                 InvokeCallback callback) throws RemotingTooMuchRequestException, RemotingTimeoutException, InterruptedException, RemotingSendRequestException {
        boolean acquired = this.semaphoreAsync.tryAcquire(timeoutMills, TimeUnit.MILLISECONDS);
        SemaphoreReleaseOnlyOnce one = new SemaphoreReleaseOnlyOnce(semaphoreAsync);
        if (acquired){
            final int reqID = request.getReqID();
            final ResponseFuture rf = new ResponseFuture(reqID, timeoutMills,callback,one);
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
                        executeInvokeCallBack(rf);
                        rf.release();
                    }
                });

            }catch (Exception ex){

            }
            finally {
                responseTable.remove(reqID);
            }
        }
        else{
            if (timeoutMills <= 0){
                throw  new RemotingTooMuchRequestException("Too Qucik Request");
            }
            else{
                String info = String.format(
                        "invokeOnewayImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d",
                        timeoutMills,
                        this.semaphoreAsync.getQueueLength(),
                        this.semaphoreAsync.availablePermits()
                );
                throw new RemotingTimeoutException(info);
            }
        }
    }

    public void sendRequestOneWay(Channel ch,RemoteCommand request, long timeoutMills) throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        boolean acquired = this.semaphoreOneway.tryAcquire(timeoutMills, TimeUnit.MILLISECONDS);
        SemaphoreReleaseOnlyOnce one = new SemaphoreReleaseOnlyOnce(semaphoreAsync);
        final SocketAddress addr = ch.remoteAddress();
        if (acquired){
            try {
                ch.writeAndFlush(request).sync();
            }catch (Exception ex){
                throw new RemotingSendRequestException(RemotingHelper.parseSocketAddressAddr(addr));
            }
            finally {
                one.release();
            }
        }
        else{
            if (timeoutMills <= 0){
                throw  new RemotingTooMuchRequestException("Too Qucik Request");
            }
            else{
                String info = String.format(
                        "invokeOnewayImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d",
                        timeoutMills,
                        this.semaphoreOneway.getQueueLength(),
                        this.semaphoreOneway.availablePermits()
                );
                throw new RemotingTimeoutException(info);
            }
        }
    }

    public void executeInvokeCallBack(final ResponseFuture future){
        boolean runInThisThread = false;
        ExecutorService service = getcallbackExecuteService();
        if (null == service){
            runInThisThread = true;
        }
        if (runInThisThread) {
            future.executeInvokeCallBack();
        }
        else{
            service.submit(new Runnable() {
                public void run() {
                    future.executeInvokeCallBack();
                }
            });
        }
    }

    public void processCMD(ChannelHandlerContext ctx, RemoteCommand msg) throws IOException {
        if (msg.isReq()){
            processRequest(ctx,msg);
        }
        else{
            processResponse(ctx,msg);
        }
    }
    public void processRequest(final ChannelHandlerContext ctx,final RemoteCommand msg) throws IOException {
        int type = msg.getType();
        final Pair<RemoteRequestProcessor, ExecutorService> matched = this.processorTable.get(type);
        if (matched != null){
            ExecutorService service = matched.getObject2() == null ? getDefaultWorkingService(): matched.getObject2();
            service.submit(new Runnable() {
                public void run() {
                    try {
                        final RemoteCommand response = matched.getObject1().processRequest(ctx,msg);
                        if (!msg.isOneway()){
                            ctx.writeAndFlush(response);
                        }
                    } catch (Exception e) {

                    }
                }
            });
        }
        else{

        }

    }

    public void processResponse(ChannelHandlerContext ctx, RemoteCommand msg) throws IOException {
        int req = msg.getReqID();
        ResponseFuture rf = responseTable.get(req);
        if (null != rf) {
            rf.putResponse(msg);
            responseTable.remove(req);
            rf.release();
            if (null != rf.getCallback()){
                executeInvokeCallBack(rf);
            }
        }
        else{

        }
    }

    class NettyEventExecutor extends Thread {
        private final LinkedBlockingQueue<NettyEvent> eventQueue = new LinkedBlockingQueue<NettyEvent>();
        private final int maxSize = 10000;

        public NettyEventExecutor(){

            this.setDaemon(true);
        }

        public void putNettyEvent(final NettyEvent event) {
            if (this.eventQueue.size() <= maxSize) {
                this.eventQueue.add(event);
            } else {
            }
        }

        @Override
        public void run() {
            final ChannelEventListener listener = getChannelEventListener();
            while (true) {
                try {
                    NettyEvent event = this.eventQueue.poll(3000, TimeUnit.MILLISECONDS);
                    if (event != null && listener != null) {
                        switch (event.getType()) {
                            case IDLE:
                                listener.onChannelIdle(event.getRemoteAddr(), event.getChannel());
                                break;
                            case CLOSE:
                                listener.onChannelClose(event.getRemoteAddr(), event.getChannel());
                                break;
                            case CONNECT:
                                listener.onChannelConnect(event.getRemoteAddr(), event.getChannel());
                                break;
                            case EXCEPTION:
                                listener.onChannelException(event.getRemoteAddr(), event.getChannel());
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {

                }
            }
        }
    }
}



class SemaphoreReleaseOnlyOnce {
    private final AtomicBoolean released = new AtomicBoolean(false);
    private final Semaphore semaphore;

    public SemaphoreReleaseOnlyOnce(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public void release() {
        if (this.semaphore != null) {
            if (this.released.compareAndSet(false, true)) {
                this.semaphore.release();
            }
        }
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }
}