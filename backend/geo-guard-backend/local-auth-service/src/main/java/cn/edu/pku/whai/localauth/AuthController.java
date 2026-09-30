package cn.edu.pku.whai.localauth;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final LocalAuthService authService;

    public AuthController(LocalAuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/auth/code")
    public ApiResponse<LocalAuthService.CaptchaPayload> captcha() {
        return ApiResponse.ok(authService.issueCaptcha());
    }

    @PostMapping("/auth/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        try {
            return ApiResponse.ok(Map.of("access_token", authService.login(request), "token_type", "Bearer"));
        } catch (LocalAuthService.LocalAuthException exception) {
            return ApiResponse.fail(exception.code(), exception.getMessage());
        }
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(tokenFrom(request));
        return ApiResponse.ok(null);
    }

    static String tokenFrom(HttpServletRequest request) {
        String token = request.getHeader("bwy-token");
        if (token == null || token.isBlank()) {
            token = request.getHeader("Authorization");
            if (token != null && token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                token = token.substring(7).trim();
            }
        }
        return token;
    }
}
