package vishal.project.auth_app.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @SneakyThrows
    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        List<String> allowedRoutes = List.of(
                "/v3/api-docs",
                "/swagger-ui",
                "/webjars",
                "/favicon.ico",
                "/actuator"
        );

        boolean isAllowed = allowedRoutes
                .stream()
                .anyMatch(route -> request.getURI().getPath().contains(route));

        if (body instanceof ApiResponse<?> || isAllowed) {
            return body;
        }

        int status = resolveStatus(response);
        String path = request.getURI().getPath();

        ApiResponse<Object> apiResponse = ApiResponse.success(body, status, path);

        // When the body is a String, Spring uses StringHttpMessageConverter which
        // expects a String return value. Serialize the wrapper to JSON manually.
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return objectMapper.writeValueAsString(apiResponse);
        }

        return apiResponse;
    }

    private int resolveStatus(ServerHttpResponse response) {
        // In servlet-based apps, unwrap and read the real HTTP status.
        if (response instanceof ServletServerHttpResponse servletResponse) {
            int status = servletResponse.getServletResponse().getStatus();
            return status > 0 ? status : 200;
        }

        // Fallback: if we can't determine the status, assume 200 for successful controller returns.
        return 200;
    }
}
