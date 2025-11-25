package com.example.dwgateway.client;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;

public class DwGatewayClientException extends BusinessException {

    public DwGatewayClientException(String message) {
        super(CommonErrorCode.INTERNAL_ERROR, message);
    }

    public DwGatewayClientException(String message, Throwable cause) {
        super(CommonErrorCode.INTERNAL_ERROR, message, cause);
    }
}
