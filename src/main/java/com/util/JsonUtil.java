package com.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 无第三方依赖的 JSON 工具。只实现本项目需要的序列化能力，
 * 统一输出 {code, message, data} 结构。
 */
public final class JsonUtil {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private JsonUtil() {
    }

    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder(256);
        append(sb, value);
        return sb.toString();
    }

    /**
     * 向浏览器输出统一 JSON 响应。
     */
    public static void write(HttpServletResponse resp, int httpStatus, int code, String message, Object data)
            throws IOException {
        resp.setStatus(httpStatus);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        resp.getWriter().write(toJson(body));
    }

    public static void ok(HttpServletResponse resp, Object data) throws IOException {
        write(resp, HttpServletResponse.SC_OK, 0, "ok", data);
    }

    public static void fail(HttpServletResponse resp, String message) throws IOException {
        write(resp, HttpServletResponse.SC_OK, 1, message, null);
    }

    @SuppressWarnings("unchecked")
    private static void append(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof CharSequence || value instanceof Character) {
            quote(sb, value.toString());
        } else if (value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Number) {
            Number number = (Number) value;
            if (number instanceof Double && (((Double) number).isNaN() || ((Double) number).isInfinite())) {
                sb.append("null");
            } else if (number instanceof Float && (((Float) number).isNaN() || ((Float) number).isInfinite())) {
                sb.append("null");
            } else {
                sb.append(number);
            }
        } else if (value instanceof LocalDateTime) {
            quote(sb, ((LocalDateTime) value).format(DATE_TIME));
        } else if (value instanceof LocalDate) {
            quote(sb, ((LocalDate) value).format(DATE));
        } else if (value instanceof Enum) {
            quote(sb, ((Enum<?>) value).name());
        } else if (value instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                quote(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                append(sb, entry.getValue());
            }
            sb.append('}');
        } else if (value instanceof Iterable) {
            sb.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                append(sb, item);
            }
            sb.append(']');
        } else if (value.getClass().isArray()) {
            sb.append('[');
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                append(sb, Array.get(value, i));
            }
            sb.append(']');
        } else {
            quote(sb, String.valueOf(value));
        }
    }

    private static void quote(StringBuilder sb, String text) {
        sb.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}