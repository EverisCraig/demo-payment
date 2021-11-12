package com.everis.craig.demopayment.exception;

import com.everis.craig.demopayment.util.I18AbleException;

public class ArgumentWebClientNotValid extends I18AbleException {
    public ArgumentWebClientNotValid(String key, Object... args) {
        super(key, args);
    }
}
