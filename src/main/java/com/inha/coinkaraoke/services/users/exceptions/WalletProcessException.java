package com.inha.coinkaraoke.services.users.exceptions;

import com.inha.coinkaraoke.exceptions.GlobalException;
import org.springframework.http.HttpStatus;

public class WalletProcessException extends GlobalException {

    private static final HttpStatus ERROR_CODE = HttpStatus.INTERNAL_SERVER_ERROR;

    public WalletProcessException() {
        super(ERROR_CODE, "cannot access to wallet!");
    }

    public WalletProcessException(String message) {
        super(ERROR_CODE, message);
    }

    public WalletProcessException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
