package com.tony.remoting.absinterface;

import com.tony.remoting.protocal.RemoteCommand;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by chnho02796 on 2017/11/14.
 */
public interface RemoteRequestProcessor {
    public RemoteCommand processRequest(ChannelHandlerContext ctx, RemoteCommand msg) throws Exception;
}
