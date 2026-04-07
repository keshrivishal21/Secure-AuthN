package vishal.project.auth_app.advice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private LocalDateTime timestamp;

    /** HTTP status code (mirrors response status) */
    private Integer status;

    /** Request path (useful for debugging) */
    private String path;

    private T data;
    private ApiError error;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data, Integer status, String path) {
        ApiResponse<T> res = new ApiResponse<>();
        res.setData(data);
        res.setStatus(status);
        res.setPath(path);
        return res;
    }

    public static <T> ApiResponse<T> failure(ApiError error, Integer status, String path) {
        ApiResponse<T> res = new ApiResponse<>();
        res.setError(error);
        res.setStatus(status);
        res.setPath(path);
        return res;
    }
}
