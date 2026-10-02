import cn.dev33.satoken.secure.BCrypt;

/** Uses the same offline BCrypt implementation as PasswordAuthStrategy. */
@SuppressWarnings("deprecation")
class HashLocalPassword {
    public static void main(String[] args) {
        String password = System.getenv("GEO_ADMIN_PASSWORD");
        if (password == null || password.length() < 12 || password.length() > 30) {
            throw new IllegalArgumentException("GEO_ADMIN_PASSWORD must contain 12 to 30 characters");
        }
        System.out.print(BCrypt.hashpw(password, BCrypt.gensalt()));
    }
}
