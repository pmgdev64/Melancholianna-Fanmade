/*
 * Melanchoianna 3D - Fan-made Adventure RPG
 * Copyright (C) 2026 PepperMCGamers Ch. PmgTeam and all Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package vn.pmgteam.games.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser — trả về:
 *   - Object: Map<String, Object>
 *   - Array: List<Object>
 *   - String: String
 *   - Number nguyên: Long
 *   - Number thập phân: Double
 *   - Bool: Boolean
 *   - Null: null
 */
final class JsonParser {
    private final String s;
    private int pos = 0;

    JsonParser(String s) { this.s = s; }

    Object parse() {
        skipWs();
        return parseValue();
    }

    private Object parseValue() {
        skipWs();
        char c = s.charAt(pos);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't' || c == 'f') return parseBool();
        if (c == 'n') { pos += 4; return null; }
        return parseNumber();
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new HashMap<>();
        pos++;
        skipWs();
        if (s.charAt(pos) == '}') { pos++; return map; }
        while (true) {
            skipWs();
            String key = parseString();
            skipWs();
            pos++;  // :
            Object value = parseValue();
            map.put(key, value);
            skipWs();
            char c = s.charAt(pos);
            if (c == ',') { pos++; continue; }
            if (c == '}') { pos++; break; }
            throw new RuntimeException("JSON parse error at " + pos);
        }
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipWs();
        if (s.charAt(pos) == ']') { pos++; return list; }
        while (true) {
            list.add(parseValue());
            skipWs();
            char c = s.charAt(pos);
            if (c == ',') { pos++; continue; }
            if (c == ']') { pos++; break; }
            throw new RuntimeException("JSON parse error at " + pos);
        }
        return list;
    }

    private String parseString() {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(pos);
            if (c == '"') { pos++; break; }
            if (c == '\\') {
                pos++;
                char esc = s.charAt(pos);
                switch (esc) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    default: sb.append(esc);
                }
                pos++;
            } else {
                sb.append(c);
                pos++;
            }
        }
        return sb.toString();
    }

    private Boolean parseBool() {
        if (s.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
        if (s.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
        throw new RuntimeException("Invalid bool at " + pos);
    }

    private Object parseNumber() {
        int start = pos;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E'
                    || (c >= '0' && c <= '9')) {
                pos++;
            } else break;
        }
        String num = s.substring(start, pos);

        // Số có dấu chấm / mũ → Double
        if (num.contains(".") || num.contains("e") || num.contains("E")) {
            return Double.parseDouble(num);
        }
        // Số nguyên → Long
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            return Double.parseDouble(num);
        }
    }

    private void skipWs() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
    }
}