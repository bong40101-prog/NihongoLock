package com.jk.nihongolock;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds ruby only to kanji spans. Kana such as ます, です and おすすめ is left
 * untouched, so furigana never floats over characters that do not need it.
 */
public final class Furigana {
    private static final Map<String, String> READINGS = new LinkedHashMap<>();

    static {
        // Longer compounds first; annotate() also checks the longest match.
        add("日本語", "にほんご");
        add("違和感", "いわかん");
        add("大丈夫", "だいじょうぶ");
        add("お勧め", "おすすめ");
        add("おすすめ", "おすすめ"); // harmless fallback for older content; filtered below
        add("友達", "ともだち");
        add("家族", "かぞく");
        add("先生", "せんせい");
        add("学生", "がくせい");
        add("会社", "かいしゃ");
        add("電車", "でんしゃ");
        add("名前", "なまえ");
        add("今日", "きょう");
        add("明日", "あした");
        add("日本", "にほん");
        add("旅行", "りょこう");
        add("飛行機", "ひこうき");
        add("空港", "くうこう");
        add("予約", "よやく");
        add("注文", "ちゅうもん");
        add("会計", "かいけい");
        add("食事", "しょくじ");
        add("美味", "おい");
        add("一緒", "いっしょ");
        add("時間", "じかん");
        add("荷物", "にもつ");
        add("地図", "ちず");
        add("居酒屋", "いざかや");
        add("料理", "りょうり");
        add("地元", "じもと");
        add("景色", "けしき");
        add("温泉", "おんせん");
        add("観光", "かんこう");
        add("最近", "さいきん");
        add("約束", "やくそく");
        add("必要", "ひつよう");
        add("便利", "べんり");
        add("特別", "とくべつ");
        add("人気", "にんき");
        add("失礼", "しつれい");
        add("相談", "そうだん");
        add("連絡", "れんらく");
        add("確認", "かくにん");
        add("変更", "へんこう");
        add("到着", "とうちゃく");
        add("出発", "しゅっぱつ");
        add("混雑", "こんざつ");
        add("予定", "よてい");
        add("理由", "りゆう");
        add("経験", "けいけん");
        add("事情", "じじょう");
        add("判断", "はんだん");
        add("提案", "ていあん");
        add("解決", "かいけつ");
        add("準備", "じゅんび");
        add("納得", "なっとく");
        add("丁寧", "ていねい");
        add("自然", "しぜん");
        add("状況", "じょうきょう");
        add("傾向", "けいこう");
        add("見解", "けんかい");
        add("適切", "てきせつ");
        add("影響", "えいきょう");
        add("解釈", "かいしゃく");
        add("複雑", "ふくざつ");
        add("改善", "かいぜん");
        add("写真", "しゃしん");
        add("一度", "いちど");
        add("店員", "てんいん");
        add("問題", "もんだい");
        add("表現", "ひょうげん");
        add("相手", "あいて");
        add("考え", "かんがえ");
        add("方法", "ほうほう");
        add("必要", "ひつよう");
        // Additional compounds used by the expanded question bank.
        add("青森", "あおもり");
        add("一軒", "いっけん");
        add("程度", "ていど");
        add("安心", "あんしん");
        add("自分", "じぶん");
        add("簡単", "かんたん");
        add("説明", "せつめい");
        add("努力", "どりょく");
        add("無理", "むり");
        add("天気", "てんき");
        add("電話", "でんわ");
        add("電話番号", "でんわばんごう");
        add("入口", "いりぐち");
        add("出口", "でぐち");
        add("切符", "きっぷ");
        add("朝食", "ちょうしょく");
        add("夕食", "ゆうしょく");
        add("名物", "めいぶつ");
        add("宿泊", "しゅくはく");
        add("受付", "うけつけ");
        add("満足", "まんぞく");
        add("期待", "きたい");
        add("優先", "ゆうせん");
        add("無事", "ぶじ");
        add("印象", "いんしょう");
        add("迷惑", "めいわく");
        add("柔軟", "じゅうなん");
        add("効率", "こうりつ");
        add("根拠", "こんきょ");
        add("妥当", "だとう");
        add("把握", "はあく");
        add("慎重", "しんちょう");
        add("左側", "ひだりがわ");
        add("一枚", "いちまい");
        add("七時", "しちじ");
        add("京都", "きょうと");
        add("二泊", "にはく");
        add("平日", "へいじつ");
        add("安全", "あんぜん");
        add("正確", "せいかく");
        add("情報", "じょうほう");
        add("十分", "じゅうぶん");
        add("重視", "じゅうし");
        add("大切", "たいせつ");

        // Single-kanji stems keep kana endings outside the ruby span.
        add("飲", "の");
        add("行", "い");
        add("何", "なん");
        add("時", "じ");
        add("書", "か");
        add("撮", "と");
        add("話", "はな");
        add("食", "た");
        add("預", "あず");
        add("入", "はい");
        add("見", "み");
        add("来", "き");
        add("思", "おも");
        add("混", "こ");
        add("聞", "き");
        add("終", "お");
        add("選", "えら");
        add("比", "くら");
        add("必", "かなら");
        add("改", "あらた");
        add("伝", "つた");
        add("言", "い");
        add("別", "べつ");
        add("嫌", "きら");
        add("無", "む");
        add("落", "お");
        add("応", "おう");
        add("駅", "えき");
        add("水", "みず");
        add("朝", "あさ");
        add("夜", "よる");
        add("店", "みせ");
        add("安", "やす");
        add("高", "たか");
        add("休", "やす");
        add("私", "わたし");
        add("近", "ちか");
        add("困", "こま");
        add("良", "よ");
        add("慣", "な");
        add("気", "き");
        add("願", "ねが");
        add("分", "わか");
        add("少", "すこ");
        add("早", "はや");
        add("考", "かんが");
        add("着", "つ");
        add("右", "みぎ");
        add("左", "ひだり");
        add("道", "みち");
        add("乗", "の");
        add("降", "お");
        add("持", "も");
        add("忘", "わす");
        add("換", "か");
        add("遅", "おく");
        add("間", "ま");
        add("合", "あ");
        add("空", "す");
        add("決", "き");
        add("調", "しら");
        add("曲", "ま");
        add("次", "つぎ");
        add("昼", "ひる");
        add("前", "まえ");
        add("早", "はや");
        add("売", "う");
        add("切", "き");
        add("扱", "あつか");
        add("物", "もの");
        add("答", "こた");
        add("違", "ちが");
        add("支払", "しはら");
        add("後", "うし");
        add("向", "む");
        add("先", "さき");
        add("支", "しはら");
        add("方", "かた");
        add("知", "し");
        add("覚", "おぼ");
        add("使", "つか");
        add("読", "よ");
        add("場面", "ばめん");
    }

    private Furigana() {}

    private static void add(String base, String reading) {
        READINGS.put(base, reading);
    }

    /** Registers a question-bank word so its reading can be reused in examples. */
    public static void register(String base, String reading) {
        if (base != null && !base.isEmpty() && reading != null && !reading.isEmpty()) {
            READINGS.put(base, reading);
        }
    }

    /** Encodes ruby spans as [base|reading] for RubyTextView. */
    public static String annotate(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        StringBuilder out = new StringBuilder(text.length() + 24);
        int index = 0;
        while (index < text.length()) {
            String match = longestMatch(text, index);
            if (match != null) {
                out.append('[').append(match).append('|').append(READINGS.get(match)).append(']');
                index += match.length();
            } else {
                int codePoint = text.codePointAt(index);
                out.appendCodePoint(codePoint);
                index += Character.charCount(codePoint);
            }
        }
        return out.toString();
    }

    /** Converts known kanji words to a rough kana reading for speech comparison. */
    public static String toReading(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder out = new StringBuilder(text.length() + 8);
        int index = 0;
        while (index < text.length()) {
            String match = longestMatch(text, index);
            if (match != null) {
                out.append(READINGS.get(match));
                index += match.length();
            } else {
                int codePoint = text.codePointAt(index);
                out.appendCodePoint(codePoint);
                index += Character.charCount(codePoint);
            }
        }
        return out.toString();
    }

    private static String longestMatch(String text, int index) {
        String match = null;
        for (Map.Entry<String, String> entry : READINGS.entrySet()) {
            String candidate = entry.getKey();
            if (!isKanjiOnly(candidate)) continue;
            if (text.startsWith(candidate, index)
                    && (match == null || candidate.length() > match.length())) match = candidate;
        }
        return match;
    }

    private static boolean isKanjiOnly(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (!((codePoint >= 0x3400 && codePoint <= 0x4DBF)
                    || (codePoint >= 0x4E00 && codePoint <= 0x9FFF))) return false;
            i += Character.charCount(codePoint);
        }
        return true;
    }
}
