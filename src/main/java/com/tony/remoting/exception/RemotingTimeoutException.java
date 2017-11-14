package com.tony.remoting.exception;

/**
 * Created by chnho02796 on 2017/11/13.
 */
public class RemotingTimeoutException extends RemotingException  {

    private static final long serialVersionUID = 4106899185095245979L;

    public RemotingTimeoutException(String message) {
        super(message);
    }
    public RemotingTimeoutException(String addr, long timeoutMillis, Throwable cause) {
        super("wait response on the channel <" + addr + "> timeout, " + timeoutMillis + "(ms)", cause);
    }
}
