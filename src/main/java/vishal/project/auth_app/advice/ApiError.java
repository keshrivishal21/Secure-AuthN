package vishal.project.auth_app.advice;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApiError {

    /** Stable machine-readable error identifier (e.g. AUTH_BAD_CREDENTIALS) */
    private String code;

    /** Human readable message */
    private String message;

    /** Optional list of details (validation messages etc.) */
    private List<String> details;
}
