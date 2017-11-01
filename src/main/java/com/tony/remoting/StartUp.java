package com.tony.remoting;

import com.tony.remoting.netty.NettyRemoteClient;
import com.tony.remoting.netty.NettyRemoteServer;

import java.util.concurrent.Callable;

/**
 * Created by chnho02796 on 2017/10/31.
 */
public class StartUp {

    public static void main(String[] args) {
        final NettyRemoteServer server = new NettyRemoteServer();
        server.setPort(9999);
        server.start();
        
        final NettyRemoteClient client = new NettyRemoteClient();
        client.setHost("127.0.0.1");
        client.setPort(9999);
        client.start();

        //Add shutdown hook
        Runtime.getRuntime().addShutdownHook(
                new Thread(){
                    public void run(){
                        server.stop();
                        client.stop();
                    }
                }
        );

        for (int i = 1; i< 100; i++){
            try {
                Thread.sleep(2000);
                client.SendRequest("This message is "+ i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
