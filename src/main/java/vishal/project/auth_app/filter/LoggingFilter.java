package vishal.project.auth_app.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    private static final int MAX_PAYLOAD_LENGTH = 10 * 1024; // 10 KB

    // Endpoints where bodies must never be logged (credentials/tokens)
    private static final List<String> SENSITIVE_PATH_CONTAINS = List.of(
            "/auth/login", "/auth/register", "/auth/refresh", "/auth/logout", "/password"
    );

    // Only log a safe header subset
    private static final Set<String> HEADER_ALLOWLIST = Set.of(
            "content-type", "accept", "user-agent", "x-request-id", "x-correlation-id"
    );

    private static final Set<String> HEADER_DENYLIST = Set.of(
            "authorization", "cookie", "set-cookie"
    );

    // Naive JSON key masking (good enough for logs; not a JSON parser)
    private static final Pattern JSON_SECRET_FIELD = Pattern.compile(
            "(?i)\"(password|passcode|token|accessToken|refreshToken|secret|apiKey)\"\\s*:\\s*\".*?\""
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(req, res);
        } finally {
            long durationMs = System.currentTimeMillis() - start;

            String method = req.getMethod();
            String uri = req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
            int status = res.getStatus();

            String headers = extractHeaders(req);
            log.info("Incoming Request: {} {} | headers={} | durationMs={}", method, uri, headers, durationMs);
            log.info("Outgoing Response: {} {} -> status={}", method, uri, status);

            if (log.isDebugEnabled() && shouldLogBody(req)) {
                String requestBody = maskSecrets(extractPayload(req.getContentAsByteArray(), req.getCharacterEncoding()));
                String responseBody = maskSecrets(extractPayload(res.getContentAsByteArray(), res.getCharacterEncoding()));

                if (!requestBody.isEmpty()) log.debug("Request Body: {}", requestBody);
                if (!responseBody.isEmpty()) log.debug("Response Body: {}", responseBody);
            }

            res.copyBodyToResponse();
        }
    }

    private boolean shouldLogBody(HttpServletRequest request) {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI().toLowerCase(Locale.ROOT);
        for (String sensitive : SENSITIVE_PATH_CONTAINS) {
            if (path.contains(sensitive)) return false;
        }

        String ct = request.getContentType();
        if (ct == null) return true;

        // Skip multipart/binary-ish content
        return ct.startsWith(MediaType.APPLICATION_JSON_VALUE)
                || ct.startsWith(MediaType.TEXT_PLAIN_VALUE)
                || ct.startsWith(MediaType.APPLICATION_XML_VALUE)
                || ct.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }

    private String extractHeaders(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) return "";

        List<String> out = new ArrayList<>();
        for (String name : Collections.list(names)) {
            String n = name.toLowerCase(Locale.ROOT);
            if (HEADER_DENYLIST.contains(n)) {
                out.add(n + "=[REDACTED]");
                continue;
            }
            if (!HEADER_ALLOWLIST.contains(n)) continue;

            List<String> vals = Collections.list(request.getHeaders(name));
            out.add(n + "=" + vals);
        }
        return out.stream().collect(Collectors.joining(", "));
    }

    private String extractPayload(byte[] buf, String enc) {
        if (buf == null || buf.length == 0) return "";

        String charsetName = (enc == null || enc.isBlank()) ? StandardCharsets.UTF_8.name() : enc;
        Charset charset;
        try {
            charset = Charset.forName(charsetName);
        } catch (Exception ex) {
            charset = StandardCharsets.UTF_8;
        }

        int len = Math.min(buf.length, MAX_PAYLOAD_LENGTH);
        String payload = new String(buf, 0, len, charset);

        if (buf.length > len) {
            payload += "...(truncated " + (buf.length - len) + " bytes)";
        }

        // Basic binary check
        long nonPrintable = payload.chars()
                .filter(ch -> ch < 32 && ch != '\n' && ch != '\r' && ch != '\t')
                .count();
        if (!payload.isEmpty() && nonPrintable > payload.length() / 4) return "";

        return payload;
    }

    private String maskSecrets(String s) {
        if (s == null || s.isBlank()) return s;
        return JSON_SECRET_FIELD.matcher(s).replaceAll("\"$1\":\"[REDACTED]\"");
    }
}
