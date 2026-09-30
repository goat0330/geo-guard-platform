package cn.edu.pku.whai.localauth;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class LocalSystemController {
    private final LocalAuthService authService;

    public LocalSystemController(LocalAuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/system/user/getInfo")
    public ApiResponse<?> userInfo(HttpServletRequest request) {
        LocalAuthService.LocalUser user = authService.userForToken(AuthController.tokenFrom(request));
        if (user == null) {
            return ApiResponse.fail(401, "登录已过期，请重新登录");
        }
        return ApiResponse.ok(Map.of(
            "user", Map.of(
                "userId", user.userId(),
                "userName", user.userName(),
                "nickName", user.nickName(),
                "avatar", ""
            ),
            "roles", List.of("local-admin"),
            "permissions", List.of("*:*:*")
        ));
    }

    @GetMapping("/system/menu/getRouters")
    public ApiResponse<?> routes(HttpServletRequest request) {
        if (authService.userForToken(AuthController.tokenFrom(request)) == null) {
            return ApiResponse.fail(401, "登录已过期，请重新登录");
        }
        return ApiResponse.ok(List.of());
    }

    @PostMapping("/dizai/online/heartbeat")
    public ApiResponse<Void> heartbeat(HttpServletRequest request) {
        if (authService.userForToken(AuthController.tokenFrom(request)) == null) {
            return ApiResponse.fail(401, "登录已过期，请重新登录");
        }
        return ApiResponse.ok(null);
    }

    @GetMapping(value = "/dizai/sse/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventStream(HttpServletRequest request) {
        if (authService.userForToken(AuthController.tokenFrom(request)) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        SseEmitter emitter = new SseEmitter(Duration.ofHours(12).toMillis());
        try {
            emitter.send(SseEmitter.event().comment("local development stream connected"));
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("service", "geo-guard-local-auth", "status", "ok"));
    }
}
