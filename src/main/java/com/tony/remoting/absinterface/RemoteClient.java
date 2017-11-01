package com.tony.remoting.absinterface;

/**
 * Created by chnho02796 on 2017/10/31.
 */
public interface RemoteClient extends RemoteService{
    void SendRequest(String msg);
}
