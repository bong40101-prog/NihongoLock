package com.jk.nihongolock;

import java.util.LinkedHashMap;
import java.util.Map;

/** Adds small ruby readings to the Japanese expressions used by the question bank. */
public final class Furigana {
    private static final Map<String, String> READINGS = new LinkedHashMap<>();

    static {
        // Longer expressions are listed first so the annotation stays natural.
        add("飲みすぎないようにします", "のみすぎないようにします");
        add("困らない程度には話せるようになりたいです", "こまらないていどにはなせるようになりたいです");
        add("話せば話すほど", "はなせばはなすほど");
        add("おすすめしてもらった店", "おすすめしてもらったみせ");
        add("思ったより", "おもったより");
        add("混んでいますね", "こんでいますね");
        add("言いたいこと", "いいたいこと");
        add("日本語", "にほんご");
        add("飲みませんか", "のみませんか");
        add("チェックインをお願いします", "チェックインをおねがいします");
        add("友達", "ともだち");
        add("日本", "にほん");
        add("行きました", "いきました");
        add("行きます", "いきます");
        add("行けたら", "いけたら");
        add("行こう", "いこう");
        add("居酒屋", "いざかや");
        add("何時", "なんじ");
        add("一緒", "いっしょ");
        add("最近", "さいきん");
        add("青森", "あおもり");
        add("予約", "よやく");
        add("話して", "はなして");
        add("料理", "りょうり");
        add("地元", "じもと");
        add("来た", "きた");
        add("時間", "じかん");
        add("思った", "おもった");
        add("一軒", "いっけん");
        add("旅行", "りょこう");
        add("困らない", "こまらない");
        add("程度", "ていど");
        add("話せる", "はなせる");
        add("慣れてくる", "なれてくる");
        add("気", "き");
        add("分かる", "わかる");
        add("自然", "しぜん");
        add("言う", "いう");
        add("別に", "べつに");
        add("嫌い", "きらい");
        add("無理", "むり");
        add("良かった", "よかった");
        add("食べたい", "たべたい");
    }

    private Furigana() {}

    private static void add(String base, String reading) {
        READINGS.put(base, reading);
    }

    /**
     * Encodes ruby spans as [base|reading]. RubyTextView turns this into text drawn
     * above the base characters. Text without a dictionary entry is left unchanged.
     */
    public static String annotate(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        StringBuilder out = new StringBuilder(text.length() + 24);
        int index = 0;
        while (index < text.length()) {
            String match = null;
            String reading = null;
            for (Map.Entry<String, String> entry : READINGS.entrySet()) {
                String candidate = entry.getKey();
                if (text.startsWith(candidate, index)
                        && (match == null || candidate.length() > match.length())) {
                    match = candidate;
                    reading = entry.getValue();
                }
            }
            if (match != null) {
                out.append('[').append(match).append('|').append(reading).append(']');
                index += match.length();
            } else {
                int codePoint = text.codePointAt(index);
                out.appendCodePoint(codePoint);
                index += Character.charCount(codePoint);
            }
        }
        return out.toString();
    }
}
