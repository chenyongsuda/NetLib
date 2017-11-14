package com.tony.remoting;

import com.tony.remoting.netty.MsgPackageEncoding;
import com.tony.remoting.netty.NettyRemoteClient;
import com.tony.remoting.netty.NettyRemoteServer;
import com.tony.remoting.protocal.RemoteCommand;
import com.tony.remoting.protocal.body.HelloRequest;

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

        for (int i = 1; i< 10000; i++){
            try {
//                if (i == 6){
//                    Thread.sleep(10000);
//                }
//                else {
//                    Thread.sleep(2000);
//                }
                RemoteCommand cmd = new RemoteCommand();
                cmd.setReq(true);
                cmd.setType("1");
                cmd.setInfos("hahahahaha");
                HelloRequest req = new HelloRequest();
                req.setName("tony");
                MsgPackageEncoding.EncodeBody(req,cmd);
//                client.SendRequest(cmd);
                long start = System.currentTimeMillis();
                RemoteCommand resp = client.sendRequestSync(cmd,2000);
                System.out.println("Time:" +(System.currentTimeMillis() - start));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
