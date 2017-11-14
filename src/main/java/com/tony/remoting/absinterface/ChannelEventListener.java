package com.tony.remoting.absinterface;

import io.netty.channel.Channel;

/**
 * Created by chnho02796 on 2017/11/14.
 */
public interface ChannelEventListener {
    void onChannelConnect(final String remoteAddr, final Channel channel);

    void onChannelClose(final String remoteAddr, final Channel channel);

    void onChannelException(final String remoteAddr, final Channel channel);

    void onChannelIdle(final String remoteAddr, final Channel channel);
}
