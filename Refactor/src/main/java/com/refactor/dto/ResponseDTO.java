package com.refactor.dto;

/**
 * @description:
 * @author: xyc
 * @date: 2025-02-25 15:51
 */
import com.refactor.enums.ResponseStatusEnum;
import lombok.Data;

import java.io.Serializable;

@Data
public class ResponseDTO<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;

    public ResponseDTO() {
    }

    public ResponseDTO(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ResponseDTO(Integer code, T data) {
        this.code = code;
        this.data = data;
    }

    public ResponseDTO(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    private ResponseDTO(ResponseStatusEnum resultStatus, T data) {
        this.code = resultStatus.getCode();
        this.msg = resultStatus.getMessage();
        this.data = data;
    }

    public static ResponseDTO<Void> success() {
        return new ResponseDTO(ResponseStatusEnum.SUCCESS, (Object)null);
    }

    public static <T> ResponseDTO<T> success(T data) {
        return new ResponseDTO(ResponseStatusEnum.SUCCESS, data);
    }

    public static <T> ResponseDTO<T> success(ResponseStatusEnum resultStatus, T data) {
        return resultStatus == null ? success(data) : new ResponseDTO(resultStatus, data);
    }

    public static <T> ResponseDTO<T> failure() {
        return new ResponseDTO(ResponseStatusEnum.FAILURE, (Object)null);
    }

    public static <T> ResponseDTO<T> failure(ResponseStatusEnum resultStatus) {
        return (ResponseDTO<T>) failure(resultStatus, (Object)null);
    }

    public static <T> ResponseDTO<T> failure(ResponseStatusEnum resultStatus, T data) {
        return resultStatus == null ? new ResponseDTO(ResponseStatusEnum.FAILURE, (Object)null) : new ResponseDTO(resultStatus, data);
    }

    public static <T> ResponseDTO<T> failure(Integer code, String msg) {
        return new ResponseDTO(code, msg);
    }


    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ResponseDTO)) {
            return false;
        } else {
            ResponseDTO<?> other = (ResponseDTO)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                label47: {
                    Object this$code = this.getCode();
                    Object other$code = other.getCode();
                    if (this$code == null) {
                        if (other$code == null) {
                            break label47;
                        }
                    } else if (this$code.equals(other$code)) {
                        break label47;
                    }

                    return false;
                }

                Object this$msg = this.getMsg();
                Object other$msg = other.getMsg();
                if (this$msg == null) {
                    if (other$msg != null) {
                        return false;
                    }
                } else if (!this$msg.equals(other$msg)) {
                    return false;
                }

                Object this$data = this.getData();
                Object other$data = other.getData();
                if (this$data == null) {
                    if (other$data != null) {
                        return false;
                    }
                } else if (!this$data.equals(other$data)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof ResponseDTO;
    }

    public int hashCode() {
        boolean PRIME = true;
        int result = 1;
        Object $code = this.getCode();
        result = result * 59 + ($code == null ? 43 : $code.hashCode());
        Object $msg = this.getMsg();
        result = result * 59 + ($msg == null ? 43 : $msg.hashCode());
        Object $data = this.getData();
        result = result * 59 + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    public String toString() {
        Integer var10000 = this.getCode();
        return "ResponseDTO(code=" + var10000 + ", msg=" + this.getMsg() + ", data=" + this.getData() + ")";
    }
}