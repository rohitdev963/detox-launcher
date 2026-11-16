package com.ansub.tvhome;
final class CompanionRequestPolicyTest {
    private static int passed;
    private static void check(boolean value) { if (!value) throw new AssertionError("case " + (passed + 1)); passed++; }
    public static void main(String[] args) {
        check(CompanionRequestPolicy.validRequestLine("GET", "/?pin=123456", "HTTP/1.1"));
        check(CompanionRequestPolicy.validRequestLine("POST", "/api/key", "HTTP/1.1"));
        check(!CompanionRequestPolicy.validRequestLine("PUT", "/api/key", "HTTP/1.1"));
        check(!CompanionRequestPolicy.validRequestLine("GET", "http://evil.test/", "HTTP/1.1"));
        check(!CompanionRequestPolicy.validRequestLine("GET", "//evil.test/", "HTTP/1.1"));
        check(!CompanionRequestPolicy.validRequestLine("GET", "/api/key", "HTTP/1.0"));
        check(!CompanionRequestPolicy.validRequestLine("GET", "/hello world", "HTTP/1.1"));
        check(CompanionRequestPolicy.validHeaderName("Content-Length"));
        check(!CompanionRequestPolicy.validHeaderName("Bad Header"));
        check(!CompanionRequestPolicy.validHeaderName("bad:name"));
        check(CompanionRequestPolicy.validHeaderValue("Bearer abc123"));
        check(!CompanionRequestPolicy.validHeaderValue("abc\rInjected: yes"));
        check(CompanionRequestPolicy.validBodyFraming("GET", false, false));
        check(CompanionRequestPolicy.validBodyFraming("POST", true, false));
        check(!CompanionRequestPolicy.validBodyFraming("POST", false, false));
        check(!CompanionRequestPolicy.validBodyFraming("POST", true, true));
        System.out.println("Companion request policy: " + passed + " checks passed");
    }
}
