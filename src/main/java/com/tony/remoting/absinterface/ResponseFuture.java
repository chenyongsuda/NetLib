package com.tony.remoting.absinterface;

import com.tony.remoting.protocal.RemoteCommand;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by chnho02796 on 2017/11/13.
 */
public class ResponseFuture {
    private int reqID;
    private RemoteCommand response;
    private boolean sendFinish;
    private Throwable cause;

    private long beginTime = System.currentTimeMillis();
    private long timeout;
    private CountDownLatch count = new CountDownLatch(1);

    public ResponseFuture(int reqID,long timeout){
        this.reqID = reqID;
        this.timeout = timeout;
    }
    public RemoteCommand waitResponse() throws InterruptedException {
        count.await(timeout, TimeUnit.MILLISECONDS);
        return response;
    }

    public void putResponse(RemoteCommand cmd){
        this.response = cmd;
        this.count.countDown();
    }

    public boolean isTimeout(){
        return timeout < (System.currentTimeMillis() - beginTime);
    }

    public int getReqID() {
        return reqID;
    }

    public void setReqID(int reqID) {
        this.reqID = reqID;
    }

    public RemoteCommand getResponse() {
        return response;
    }

    public void setResponse(RemoteCommand response) {
        this.response = response;
    }

    public boolean isSendFinish() {
        return sendFinish;
    }

    public void setSendFinish(boolean sendFinish) {
        this.sendFinish = sendFinish;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
