package com.example.usermanagement.common;

import lombok.Data;

@Data
public class Result<T> {
    private int status;
    private String msg;
    private T data;

    private Result(int status, String msg, T data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(){
        return success(null);
    }

    public static <T> Result<T> error(ResultCode code){
        return new Result<>(code.getCode(),code.getMessage(),null);
    }

    public static <T> Result<T> error(int status, String msg){
        return new Result<>(status,msg, null);
    }
}
