package com.tony.remoting.protocal;

import com.tony.remoting.absinterface.RemotingAbstract;
import org.msgpack.annotation.Message;

/**
 * Created by chnho02796 on 2017/11/9.
 */
@org.msgpack.annotation.Message
public class RemoteCommand {
    private int reqID = RemotingAbstract.requestId.addAndGet(1);
    private boolean isReq = true;
    private String type;
    private String infos;
    private byte[] body;

    public int getReqID() {
        return reqID;
    }

    public boolean isReq() {
        return isReq;
    }

    public void setReq(boolean req) {
        isReq = req;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInfos() {
        return infos;
    }

    public void setInfos(String infos) {
        this.infos = infos;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "RemoteCommand{" +
                "type='" + type + '\'' +
                ", infos='" + infos + '\'' +
                '}';
    }
}
