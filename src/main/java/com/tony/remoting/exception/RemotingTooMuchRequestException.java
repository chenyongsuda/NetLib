package com.tony.remoting.exception;

/**
 * Created by chnho02796 on 2017/11/14.
 */
public class RemotingTooMuchRequestException extends RemotingException {
    private static final long serialVersionUID = 4326919581254519654L;

    public RemotingTooMuchRequestException(String message) {
        super(message);
    }
}
