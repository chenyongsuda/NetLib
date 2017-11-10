package com.tony.remoting.protocal;

import org.msgpack.annotation.Message;

/**
 * Created by chnho02796 on 2017/11/9.
 */
@org.msgpack.annotation.Message
public class RemoteCommand {
    private String type;
    private String infos;

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

    @Override
    public String toString() {
        return "RemoteCommand{" +
                "type='" + type + '\'' +
                ", infos='" + infos + '\'' +
                '}';
    }
}
