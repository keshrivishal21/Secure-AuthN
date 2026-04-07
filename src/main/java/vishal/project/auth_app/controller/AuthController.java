package vishal.project.auth_app.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vishal.project.auth_app.dto.LoginRequestDto;
import vishal.project.auth_app.dto.LoginResponseDto;
import vishal.project.auth_app.dto.SignUpRequestDto;
import vishal.project.auth_app.dto.SignUpResponseDto;
import vishal.project.auth_app.security.AuthService;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${security.jwt.refresh-ttl-ms:604800000}")
    private long refreshTtlMs;

    @Operation(summary = "Register a new user")
    @PostMapping(value = "/signup")
    public ResponseEntity<SignUpResponseDto> signup(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        SignUpResponseDto signUpResponseDto = authService.signUp(signUpRequestDto);
        return ResponseEntity.ok(signUpResponseDto);
    }

    @Operation(summary = "Login and get JWT token")
    @PostMapping(value = "/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto loginRequestDto,
            HttpServletResponse response
    ) {
        LoginResponseDto login = authService.login(loginRequestDto);
        if(login.getRefreshToken() != null && !login.getRefreshToken().isBlank()){
            setRefreshCookie(response,login.getRefreshToken());
        }
        return ResponseEntity.ok(new LoginResponseDto(login.getId(), login.getAccessToken(), null));
    }

    @PostMapping(value = "/refresh")
    public ResponseEntity<LoginResponseDto> refresh(HttpServletRequest request,HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookies(request);
        if(refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationServiceException("Refresh token is missing");
        }
        LoginResponseDto loginResponseDto = authService.refreshToken(refreshToken);
        if(loginResponseDto.getRefreshToken() != null && !loginResponseDto.getRefreshToken().isBlank()){
            setRefreshCookie(response,loginResponseDto.getRefreshToken());
        }
        return ResponseEntity.ok(new LoginResponseDto(loginResponseDto.getId(), loginResponseDto.getAccessToken(), null));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookies(request);

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private String extractRefreshTokenFromCookies(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) return null;

        return Arrays.stream(cookies)
                .filter(c -> "refreshToken".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken){
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge((int) (refreshTtlMs / 1000));
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
