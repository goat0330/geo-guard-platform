package cn.edu.pku.whai.localauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginRequest(
    String username,
    String password,
    String name,
    String pwd,
    String code,
    String uuid
) {
    public String account() {
        return username != null && !username.isBlank() ? username.trim() : name;
    }

    public String secret() {
        return password != null ? password : pwd;
    }
}
