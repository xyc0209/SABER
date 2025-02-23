package com.refactor.enumeration;

/**
 * @author Cocoicobird
 * @version 1.0
 */
public enum RequestMethod {

    GET,
    POST,
    PUT,
    PATCH,
    DELETE;

    @Override
    public String toString() {
        return "Request." + super.toString();
    }
}
