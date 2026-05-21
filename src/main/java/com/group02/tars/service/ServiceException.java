package com.group02.tars.service;

/**
 * 业务异常 —— Service层发现错误时抛出，被Servlet的catch接住后转成JSON错误响应返回给前端。
 * 携带三个信息：httpStatus(HTTP状态码) + code(错误码，如"AUTH_EMAIL_EXISTS") + message(给用户看的提示)
 */
public class ServiceException extends Exception {

    private final int httpStatus;
    private final String code;

    public ServiceException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }
}
