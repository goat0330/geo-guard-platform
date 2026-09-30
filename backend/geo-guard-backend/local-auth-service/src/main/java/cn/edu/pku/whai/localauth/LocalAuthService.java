package cn.edu.pku.whai.localauth;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class LocalAuthService {
    private static final char[] CAPTCHA_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final long CAPTCHA_TTL_MS = Duration.ofMinutes(5).toMillis();
    private static final long SESSION_TTL_MS = Duration.ofHours(12).toMillis();
    private static final int PASSWORD_ITERATIONS = 120_000;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaEntry> captchas = new ConcurrentHashMap<>();
    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final String username;
    private final String displayName;
    private final byte[] passwordSalt = new byte[16];
    private final byte[] passwordHash;

    public LocalAuthService(Environment environment) {
        username = environment.getProperty("GEO_LOCAL_AUTH_USERNAME", "").trim();
        displayName = environment.getProperty("GEO_LOCAL_AUTH_DISPLAY_NAME", "本地开发用户");
        String password = environment.getProperty("GEO_LOCAL_AUTH_PASSWORD", "");
        random.nextBytes(passwordSalt);
        passwordHash = password.isBlank() ? null : hashPassword(password);
    }

    public CaptchaPayload issueCaptcha() {
        expireOldEntries();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode();
        captchas.put(uuid, new CaptchaEntry(code, System.currentTimeMillis() + CAPTCHA_TTL_MS));
        return new CaptchaPayload(true, uuid, renderGif(code));
    }

    public String login(LoginRequest request) {
        if (username.isBlank() || passwordHash == null) {
            throw new LocalAuthException(503, "本地账号未配置，请通过 run-local.ps1 设置账号后重启服务");
        }
        if (request == null || blank(request.uuid()) || blank(request.code())) {
            throw new LocalAuthException(400, "请先获取并填写验证码");
        }

        CaptchaEntry captcha = captchas.remove(request.uuid());
        if (captcha == null || captcha.expiresAt() < System.currentTimeMillis()
            || !constantTimeEquals(captcha.code(), request.code().trim().toUpperCase(Locale.ROOT))) {
            throw new LocalAuthException(400, "验证码错误或已过期，请刷新后重试");
        }

        String account = request.account();
        String secret = request.secret();
        if (blank(account) || blank(secret)
            || !constantTimeEquals(username, account.trim())
            || !MessageDigest.isEqual(passwordHash, hashPassword(secret))) {
            throw new LocalAuthException(400, "账号或密码错误");
        }

        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        sessions.put(token, new SessionEntry(username, System.currentTimeMillis() + SESSION_TTL_MS));
        return token;
    }

    public LocalUser userForToken(String token) {
        if (blank(token)) {
            return null;
        }
        SessionEntry session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.expiresAt() < System.currentTimeMillis()) {
            sessions.remove(token, session);
            return null;
        }
        return new LocalUser("local-dev-user", session.username(), displayName);
    }

    public void logout(String token) {
        if (!blank(token)) {
            sessions.remove(token);
        }
    }

    private void expireOldEntries() {
        long now = System.currentTimeMillis();
        captchas.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
    }

    private String randomCode() {
        StringBuilder result = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            result.append(CAPTCHA_CHARS[random.nextInt(CAPTCHA_CHARS.length)]);
        }
        return result.toString();
    }

    private String renderGif(String code) {
        BufferedImage image = new BufferedImage(132, 46, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(246, 249, 252));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            for (int i = 0; i < 8; i++) {
                graphics.setColor(new Color(75, 125, 180, 100));
                graphics.drawLine(random.nextInt(132), random.nextInt(46), random.nextInt(132), random.nextInt(46));
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
            for (int i = 0; i < code.length(); i++) {
                int x = 10 + i * 24;
                int y = 32 + random.nextInt(7) - 3;
                AffineTransform original = graphics.getTransform();
                graphics.rotate(Math.toRadians(random.nextInt(17) - 8), x + 8, y - 9);
                graphics.setColor(new Color(25 + random.nextInt(70), 68 + random.nextInt(70), 115 + random.nextInt(70)));
                graphics.drawString(String.valueOf(code.charAt(i)), x, y);
                graphics.setTransform(original);
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "gif", output)) {
                throw new IllegalStateException("当前 JDK 没有可用的 GIF 编码器");
            }
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("生成验证码图片失败", exception);
        }
    }

    private byte[] hashPassword(String password) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), passwordSalt, PASSWORD_ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("本地密码校验初始化失败", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record CaptchaPayload(boolean captchaEnabled, String uuid, String img) {}
    public record LocalUser(String userId, String userName, String nickName) {}
    private record CaptchaEntry(String code, long expiresAt) {}
    private record SessionEntry(String username, long expiresAt) {}

    public static class LocalAuthException extends RuntimeException {
        private final int code;

        public LocalAuthException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int code() {
            return code;
        }
    }
}
