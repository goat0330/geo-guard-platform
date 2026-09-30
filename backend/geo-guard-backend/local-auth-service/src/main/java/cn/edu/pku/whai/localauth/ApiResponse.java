package cn.edu.pku.whai.localauth;

public record ApiResponse<T>(int code, String msg, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static ApiResponse<Void> fail(int code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }
}
