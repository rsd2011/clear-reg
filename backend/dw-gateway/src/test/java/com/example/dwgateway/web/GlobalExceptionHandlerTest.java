package com.example.dwgateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.common.error.BusinessException;
import com.example.common.error.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    DwGatewayExceptionHandler handler = new DwGatewayExceptionHandler();

    @Test
    void businessExceptionMapsToErrorResponse() {
        var ex = new BusinessException(CommonErrorCode.NOT_FOUND, "missing");
        var response = handler.handleBusiness(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("missing");
    }

    @Test
    void permissionDeniedMapsToForbidden() {
        var ex = new BusinessException(CommonErrorCode.PERMISSION_DENIED, "nope");
        var response = handler.handleBusiness(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void conflictMapsTo409() {
        var ex = new BusinessException(CommonErrorCode.CONFLICT, "conflict");
        var response = handler.handleBusiness(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void nullCodeDefaultsTo500() {
        var ex = new BusinessException(null, "oops");
        var response = handler.handleBusiness(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }
}
