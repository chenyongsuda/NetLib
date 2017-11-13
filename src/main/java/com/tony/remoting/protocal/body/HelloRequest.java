package com.tony.remoting.protocal.body;

import org.msgpack.annotation.Message;

/**
 * Created by chnho02796 on 2017/11/13.
 */
@Message
public class HelloRequest {
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
