package com.refactor.enums;

/**
 * @description:
 * @author: xyc
 * @date: 2025-02-25 15:51
 */
public enum ResponseStatusEnum {
    SUCCESS(10000, "Operation successful."),
    FAILURE(-1, "Operation failed."),
    PARAM_IS_INVALID(10001, "Invalid parameter."),
    PARAM_IS_BLANK(10002, "Parameter is empty."),
    PARAM_TYPE_BIND_ERROR(10003, "Parameter format error."),
    PARAM_NOT_COMPLETE(10004, "Missing parameter."),
    USER_NOT_LOGGED_IN(20001, "User not logged in, please log in first."),
    USER_LOGIN_ERROR(20002, "Account does not exist or password is incorrect."),
    USER_ACCOUNT_FORBIDDEN(20003, "Account has been disabled."),
    USER_NOT_EXIST(20004, "User does not exist."),
    USER_HAS_EXISTED(20005, "User already exists."),
    FILE_MAX_SIZE_OVERFLOW(30003, "Uploaded size exceeds the limit."),
    FILE_ACCEPT_NOT_SUPPORT(30004, "Unsupported file format."),
    SYSTEM_UNKNOWN_ERROR(30005, "System error."),
    RESULT_DATA_NONE(40001, "Data not found."),
    DATA_IS_WRONG(40002, "Data is incorrect."),
    DATA_ALREADY_EXISTED(40003, "Data already exists."),
    AUTH_CODE_ERROR(40004, "Verification code error."),
    PERMISSION_UNAUTHENTICATED(50001, "This operation requires login."),
    PERMISSION_UNAUTHORIZED(50002, "Insufficient permissions, unauthorized operation."),
    PERMISSION_EXPIRE(50003, "Login status expired."),
    PERMISSION_TOKEN_EXPIRED(50004, "Token has expired."),
    PERMISSION_LIMIT(50005, "Access limit exceeded."),
    PERMISSION_TOKEN_INVALID(50006, "Invalid token."),
    PERMISSION_SIGNATURE_ERROR(50007, "Signature failed."),
    SERVICE_UNAVAILABLE(60001, "Service is unavailable.");

    private final int code;
    private final String message;

    private ResponseStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }
}