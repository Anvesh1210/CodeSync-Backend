public class RegexTest {
    public static void main(String[] args) {
        String body = "{\"timestamp\":[2026,5,14,15,16,39,369877200],\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or expired OTP\",\"path\":\"/auth/validate-otp\"}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
        java.util.regex.Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            System.out.println("Match: " + matcher.group(1));
        } else {
            System.out.println("No match");
        }
    }
}
