package com.ansub.tvhome;

/** Pure-Java request framing checks for the local companion HTTP server. */
final class CompanionRequestPolicy {
    private CompanionRequestPolicy() {}

    static boolean validRequestLine(String method, String target, String version) {
        if (!("GET".equals(method) || "POST".equals(method))) return false;
        if (!"HTTP/1.1".equals(version)) return false;
        if (target == null || !target.startsWith("/") || target.startsWith("//")) return false;
        for (int i = 0; i < target.length(); i++) {
            char c = target.charAt(i);
            if (c <= 0x20 || c == 0x7f || c == '#') return false;
        }
        return true;
    }

    static boolean validHeaderName(String name) {
        if (name == null || name.isEmpty()) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean token = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
            if (!token) return false;
        }
        return true;
    }

    static boolean validHeaderValue(String value) {
        if (value == null) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 && c != '\t' || c == 0x7f) return false;
        }
        return true;
    }

    static boolean validBodyFraming(String method, boolean hasLength, boolean hasTransferEncoding) {
        return !hasTransferEncoding && (!"POST".equals(method) || hasLength);
    }
}
