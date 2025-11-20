package com.example.dwgateway.client;

public class DwGatewayClientException extends RuntimeException {

    public DwGatewayClientException(String message) {
        super(message);
    }

    public DwGatewayClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
