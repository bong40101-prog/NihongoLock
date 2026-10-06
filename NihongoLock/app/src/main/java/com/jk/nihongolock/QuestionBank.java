package com.jk.nihongolock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Offline question bank. The bank deliberately mixes vocabulary and sentences
 * so a short study session does not become sentence-translation-only practice.
 */
public final class QuestionBank {
    public static final String KIND_WORD = "word";
    public static final String KIND_SENTENCE = "sentence";

    public static class Q {
        public final String id;
        public final String prompt;
        public final String[] options;
        public final int answer;
        public final String explanation;
        public final int level;
        public final String audioText;
        public final String kind;
        public final String koreanMeaning;
        public final String reading;
        public final String writingPrompt;
        public final String writingAnswer;

        public Q(String id, String prompt, String[] options, int answer, String explanation,
                 int level, String audioText, String kind, String koreanMeaning,
                 String reading, String writingPrompt, String writingAnswer) {
            this.id = id;
            this.prompt = prompt;
            this.options = options;
            this.answer = answer;
            this.explanation = explanation;
            this.level = level;
            this.audioText = audioText == null ? "" : audioText;
            this.kind = kind == null ? KIND_SENTENCE : kind;
            this.koreanMeaning = koreanMeaning == null ? "" : koreanMeaning;
            this.reading = reading == null ? "" : reading;
            this.writingPrompt = writingPrompt == null ? "일본어로 입력하세요." : writingPrompt;
            this.writingAnswer = writingAnswer == null ? this.audioText : writingAnswer;
        }
    }

    // id, Japanese, reading, Korean meaning, JLPT-style level
    private static final String[][] WORDS = {
            {"w01", "友達", "ともだち", "친구", "1"},
            {"w02", "家族", "かぞく", "가족", "1"},
            {"w03", "先生", "せんせい", "선생님", "1"},
            {"w04", "学生", "がくせい", "학생", "1"},
            {"w05", "会社", "かいしゃ", "회사", "1"},
            {"w06", "電車", "でんしゃ", "전철", "1"},
            {"w07", "駅", "えき", "역", "1"},
            {"w08", "水", "みず", "물", "1"},
            {"w09", "今日", "きょう", "오늘", "1"},
            {"w10", "明日", "あした", "내일", "1"},
            {"w11", "朝", "あさ", "아침", "1"},
            {"w12", "夜", "よる", "밤", "1"},
            {"w13", "名前", "なまえ", "이름", "1"},
            {"w14", "日本", "にほん", "일본", "1"},

            {"w15", "旅行", "りょこう", "여행", "2"},
            {"w16", "飛行機", "ひこうき", "비행기", "2"},
            {"w17", "空港", "くうこう", "공항", "2"},
            {"w18", "予約", "よやく", "예약", "2"},
            {"w19", "注文", "ちゅうもん", "주문", "2"},
            {"w20", "会計", "かいけい", "계산", "2"},
            {"w21", "店", "みせ", "가게", "2"},
            {"w22", "食事", "しょくじ", "식사", "2"},
            {"w23", "美味しい", "おいしい", "맛있다", "2"},
            {"w24", "安い", "やすい", "싸다", "2"},
            {"w25", "高い", "たかい", "비싸다", "2"},
            {"w26", "一緒", "いっしょ", "함께", "2"},
            {"w27", "時間", "じかん", "시간", "2"},
            {"w28", "荷物", "にもつ", "짐", "2"},
            {"w29", "地図", "ちず", "지도", "2"},

            {"w30", "居酒屋", "いざかや", "이자카야", "3"},
            {"w31", "料理", "りょうり", "요리", "3"},
            {"w32", "地元", "じもと", "현지", "3"},
            {"w33", "景色", "けしき", "경치", "3"},
            {"w34", "温泉", "おんせん", "온천", "3"},
            {"w35", "観光", "かんこう", "관광", "3"},
            {"w36", "最近", "さいきん", "최근", "3"},
            {"w37", "約束", "やくそく", "약속", "3"},
            {"w38", "必要", "ひつよう", "필요", "3"},
            {"w39", "大丈夫", "だいじょうぶ", "괜찮다", "3"},
            {"w40", "便利", "べんり", "편리하다", "3"},
            {"w41", "特別", "とくべつ", "특별하다", "3"},
            {"w42", "人気", "にんき", "인기가 있다", "3"},
            {"w43", "失礼", "しつれい", "실례", "3"},

            {"w44", "相談", "そうだん", "상담", "4"},
            {"w45", "連絡", "れんらく", "연락", "4"},
            {"w46", "選ぶ", "えらぶ", "고르다", "4"},
            {"w47", "比べる", "くらべる", "비교하다", "4"},
            {"w48", "確認", "かくにん", "확인", "4"},
            {"w49", "変更", "へんこう", "변경", "4"},
            {"w50", "到着", "とうちゃく", "도착", "4"},
            {"w51", "出発", "しゅっぱつ", "출발", "4"},
            {"w52", "混雑", "こんざつ", "혼잡", "4"},
            {"w53", "予定", "よてい", "예정", "4"},
            {"w54", "理由", "りゆう", "이유", "4"},
            {"w55", "経験", "けいけん", "경험", "4"},

            {"w56", "事情", "じじょう", "사정", "5"},
            {"w57", "判断", "はんだん", "판단", "5"},
            {"w58", "提案", "ていあん", "제안", "5"},
            {"w59", "解決", "かいけつ", "해결", "5"},
            {"w60", "準備", "じゅんび", "준비", "5"},
            {"w61", "必ず", "かならず", "반드시", "5"},
            {"w62", "改めて", "あらためて", "다시·새삼", "5"},
            {"w63", "納得", "なっとく", "납득", "5"},
            {"w64", "丁寧", "ていねい", "정중함", "5"},
            {"w65", "自然", "しぜん", "자연스러움", "5"},

            {"w66", "違和感", "いわかん", "위화감", "6"},
            {"w67", "状況", "じょうきょう", "상황", "6"},
            {"w68", "傾向", "けいこう", "경향", "6"},
            {"w69", "見解", "けんかい", "견해", "6"},
            {"w70", "適切", "てきせつ", "적절함", "6"},
            {"w71", "影響", "えいきょう", "영향", "6"},
            {"w72", "解釈", "かいしゃく", "해석", "6"},
            {"w73", "複雑", "ふくざつ", "복잡함", "6"},
            {"w74", "改善", "かいぜん", "개선", "6"}
    };

    // id, Japanese sentence, Korean meaning, JLPT-style level
    private static final String[][] SENTENCES = {
            {"s01", "おはようございます。", "좋은 아침입니다.", "1"},
            {"s02", "ありがとうございます。", "감사합니다.", "1"},
            {"s03", "これは何ですか。", "이것은 무엇입니까?", "1"},
            {"s04", "いくらですか。", "얼마입니까?", "1"},
            {"s05", "水をお願いします。", "물 부탁합니다.", "1"},
            {"s06", "トイレはどこですか。", "화장실은 어디입니까?", "1"},
            {"s07", "日本へ行きます。", "일본에 갑니다.", "1"},
            {"s08", "友達と飲みます。", "친구와 마십니다.", "1"},
            {"s09", "今日は休みです。", "오늘은 쉽니다.", "1"},
            {"s10", "名前を書いてください。", "이름을 써 주세요.", "1"},

            {"s11", "何時ですか。", "몇 시예요?", "2"},
            {"s12", "チェックインをお願いします。", "체크인 부탁합니다.", "2"},
            {"s13", "予約しているクォンです。", "예약한 권입니다.", "2"},
            {"s14", "写真を撮ってもいいですか。", "사진을 찍어도 됩니까?", "2"},
            {"s15", "もう一度お願いします。", "다시 한 번 부탁합니다.", "2"},
            {"s16", "ゆっくり話してください。", "천천히 말해 주세요.", "2"},
            {"s17", "ここで食べます。", "여기서 먹습니다.", "2"},
            {"s18", "駅までお願いします。", "역까지 부탁합니다.", "2"},
            {"s19", "これは私の荷物です。", "이것은 제 짐입니다.", "2"},
            {"s20", "一緒に行きましょう。", "같이 갑시다.", "2"},

            {"s21", "一緒に飲みませんか。", "같이 한잔하지 않을래요?", "3"},
            {"s22", "最近は青森にも行きました。", "최근에는 아오모리에도 갔습니다.", "3"},
            {"s23", "おすすめは何ですか。", "추천은 무엇인가요?", "3"},
            {"s24", "もう少しゆっくり話してもらえますか。", "조금 더 천천히 말해 주실 수 있나요?", "3"},
            {"s25", "地元の料理を食べたいです。", "현지 음식을 먹고 싶습니다.", "3"},
            {"s26", "この店は人気があります。", "이 가게는 인기가 있습니다.", "3"},
            {"s27", "予定を確認します。", "일정을 확인합니다.", "3"},
            {"s28", "荷物を預けてもいいですか。", "짐을 맡겨도 될까요?", "3"},
            {"s29", "温泉に入りたいです。", "온천에 들어가고 싶습니다.", "3"},
            {"s30", "旅行の写真を見せてください。", "여행 사진을 보여 주세요.", "3"},

            {"s31", "せっかく日本に来たので、地元の料理を食べたいです。", "모처럼 일본에 왔으니 현지 음식을 먹고 싶습니다.", "4"},
            {"s32", "もし時間があれば、一緒に行きませんか。", "만약 시간이 있으면 같이 가지 않을래요?", "4"},
            {"s33", "思ったより混んでいますね。", "생각보다 붐비네요.", "4"},
            {"s34", "飲みすぎないようにします。", "너무 많이 마시지 않도록 할게요.", "4"},
            {"s35", "行けたら行きます。", "갈 수 있으면 갈게요.", "4"},
            {"s36", "店員さんにおすすめを聞きました。", "점원에게 추천을 물었습니다.", "4"},
            {"s37", "予定が変更になりました。", "일정이 변경되었습니다.", "4"},
            {"s38", "旅行の準備がまだ終わっていません。", "여행 준비가 아직 끝나지 않았습니다.", "4"},
            {"s39", "必要なものを確認しておきます。", "필요한 것을 미리 확인해 둘게요.", "4"},
            {"s40", "この店は駅から近くて便利です。", "이 가게는 역에서 가깝고 편리합니다.", "4"},

            {"s41", "せっかくだから、もう一軒行こう。", "이왕 이렇게 된 거 한 군데 더 가자.", "5"},
            {"s42", "日本語はまだまだですが、旅行で困らない程度には話せるようになりたいです。", "일본어는 아직 부족하지만 여행에서 곤란하지 않을 정도로 말할 수 있게 되고 싶습니다.", "5"},
            {"s43", "おすすめしてもらった店、すごく良かったです。", "추천받은 가게가 정말 좋았어요.", "5"},
            {"s44", "話せば話すほど慣れてくる気がします。", "말하면 말할수록 익숙해지는 것 같아요.", "5"},
            {"s45", "予定どおりに到着できて安心しました。", "예정대로 도착해서 안심했습니다.", "5"},
            {"s46", "失礼にならないように丁寧に話します。", "실례가 되지 않도록 정중하게 말하겠습니다.", "5"},
            {"s47", "何か困ったことがあれば連絡してください。", "뭔가 곤란한 일이 있으면 연락해 주세요.", "5"},
            {"s48", "自分の経験を簡単に説明しました。", "자신의 경험을 간단히 설명했습니다.", "5"},
            {"s49", "準備ができたら、改めて連絡します。", "준비가 되면 다시 연락하겠습니다.", "5"},
            {"s50", "相手の提案に納得しました。", "상대의 제안에 납득했습니다.", "5"},

            {"s51", "言いたいことは分かるけど、もう少し自然に言うなら…。", "말하고 싶은 것은 알겠지만 좀 더 자연스럽게 말하자면…", "6"},
            {"s52", "別に嫌いなわけじゃない。", "딱히 싫어하는 것은 아니야.", "6"},
            {"s53", "それ、さすがに無理じゃない？", "그건 아무리 그래도 무리 아니야?", "6"},
            {"s54", "状況を確認してから判断したいです。", "상황을 확인한 후 판단하고 싶습니다.", "6"},
            {"s55", "なるべく早く解決できるように努力します。", "가능한 한 빨리 해결할 수 있도록 노력하겠습니다.", "6"},
            {"s56", "この表現には少し違和感があります。", "이 표현에는 조금 위화감이 있습니다.", "6"},
            {"s57", "事情を説明して、相手に納得してもらいました。", "사정을 설명해서 상대를 납득시켰습니다.", "6"},
            {"s58", "自分の考えを自然な日本語で伝えられるようになりたいです。", "자신의 생각을 자연스러운 일본어로 전달할 수 있게 되고 싶습니다.", "6"},
            {"s59", "複雑な問題ほど、落ち着いて考える必要があります。", "복잡한 문제일수록 침착하게 생각할 필요가 있습니다.", "6"},
            {"s60", "状況に応じて、適切な方法を選ぶべきです。", "상황에 따라 적절한 방법을 골라야 합니다.", "6"}
    };

    private static final List<Q> ALL = new ArrayList<>();

    static {
        for (int i = 0; i < WORDS.length; i++) {
            String[] row = WORDS[i];
            String japanese = row[1];
            String reading = row[2];
            String meaning = row[3];
            int level = Integer.parseInt(row[4]);
            ALL.add(new Q(
                    row[0],
                    "『" + japanese + "』의 뜻은?",
                    options(meaning, i, WORDS, 3),
                    0,
                    japanese + "(" + reading + ") = " + meaning,
                    level,
                    japanese,
                    KIND_WORD,
                    meaning,
                    reading,
                    "다음 뜻의 일본어 단어를 입력하세요: " + meaning,
                    japanese));
        }
        for (int i = 0; i < SENTENCES.length; i++) {
            String[] row = SENTENCES[i];
            String japanese = row[1];
            String meaning = row[2];
            int level = Integer.parseInt(row[3]);
            ALL.add(new Q(
                    row[0],
                    "『" + japanese + "』의 뜻은?",
                    options(meaning, i, SENTENCES, 2),
                    0,
                    japanese + " = " + meaning,
                    level,
                    japanese,
                    KIND_SENTENCE,
                    meaning,
                    "",
                    "다음 뜻을 일본어 문장으로 입력하세요: " + meaning,
                    japanese));
        }
    }

    private QuestionBank() {}

    private static String[] options(String correct, int index, String[][] rows, int meaningIndex) {
        String[] out = new String[]{correct, "", "", ""};
        int added = 1;
        for (int offset = 1; added < out.length && offset < rows.length + 2; offset++) {
            String candidate = rows[(index + offset * 7) % rows.length][meaningIndex];
            boolean duplicate = false;
            for (int i = 0; i < added; i++) {
                if (candidate.equals(out[i])) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) out[added++] = candidate;
        }
        return out;
    }

    public static Q shuffled(Q q) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < q.options.length; i++) order.add(i);
        Collections.shuffle(order);
        String[] opts = new String[q.options.length];
        int newAnswer = 0;
        for (int i = 0; i < order.size(); i++) {
            int old = order.get(i);
            opts[i] = q.options[old];
            if (old == q.answer) newAnswer = i;
        }
        return new Q(q.id, q.prompt, opts, newAnswer, q.explanation, q.level, q.audioText,
                q.kind, q.koreanMeaning, q.reading, q.writingPrompt, q.writingAnswer);
    }

    public static List<Q> forLevel(int level) {
        return forLevel(level, new HashSet<>());
    }

    /** Returns a shuffled, word/sentence-interleaved session without recent questions. */
    public static List<Q> forLevel(int level, Set<String> excludedIds) {
        int low = Math.max(1, level - 1);
        int high = Math.min(6, level + 1);
        Set<String> excluded = excludedIds == null ? new HashSet<>() : excludedIds;
        List<Q> words = new ArrayList<>();
        List<Q> sentences = new ArrayList<>();
        for (Q q : ALL) {
            if (q.level < low || q.level > high || excluded.contains(q.id)) continue;
            if (KIND_WORD.equals(q.kind)) words.add(q);
            else sentences.add(q);
        }
        // If the recent window covers a whole small range, start a fresh pool.
        if (words.size() + sentences.size() < 12) {
            words.clear();
            sentences.clear();
            for (Q q : ALL) {
                if (q.level < low || q.level > high) continue;
                if (KIND_WORD.equals(q.kind)) words.add(q);
                else sentences.add(q);
            }
        }
        Collections.shuffle(words);
        Collections.shuffle(sentences);
        List<Q> out = new ArrayList<>();
        int wi = 0;
        int si = 0;
        while (wi < words.size() || si < sentences.size()) {
            boolean wantWord = out.size() % 2 == 0;
            if (wantWord && wi < words.size()) out.add(words.get(wi++));
            else if (!wantWord && si < sentences.size()) out.add(sentences.get(si++));
            else if (wi < words.size()) out.add(words.get(wi++));
            else if (si < sentences.size()) out.add(sentences.get(si++));
        }
        return out;
    }

    public static List<Q> writingForLevel(int level, Set<String> excludedIds) {
        return forLevel(level, excludedIds);
    }

    public static List<Q> speakingForLevel(int level, Set<String> excludedIds) {
        int low = Math.max(1, level - 1);
        int high = Math.min(6, level + 1);
        Set<String> excluded = excludedIds == null ? new HashSet<>() : excludedIds;
        List<Q> out = new ArrayList<>();
        for (Q q : ALL) {
            if (KIND_SENTENCE.equals(q.kind) && q.level >= low && q.level <= high && !excluded.contains(q.id)) {
                out.add(q);
            }
        }
        if (out.size() < 8) {
            out.clear();
            for (Q q : ALL) if (KIND_SENTENCE.equals(q.kind) && q.level >= low && q.level <= high) out.add(q);
        }
        Collections.shuffle(out);
        return out;
    }

    /** Exactly ten questions for a manually started promotion test. */
    public static List<Q> levelTest(int currentLevel) {
        int safeLevel = Math.max(1, Math.min(6, currentLevel));
        int target = Math.min(6, safeLevel + 1);
        List<Q> words = new ArrayList<>();
        List<Q> sentences = new ArrayList<>();
        for (Q q : ALL) {
            if ((q.level == target || q.level == safeLevel) && KIND_WORD.equals(q.kind)) words.add(q);
            if ((q.level == target || q.level == safeLevel) && KIND_SENTENCE.equals(q.kind)) sentences.add(q);
        }
        Collections.shuffle(words);
        Collections.shuffle(sentences);
        List<Q> out = new ArrayList<>();
        addUpTo(out, words, 5);
        addUpTo(out, sentences, 5);
        if (out.size() < 10) {
            List<Q> fallback = new ArrayList<>(ALL);
            Collections.shuffle(fallback);
            for (Q q : fallback) {
                if (out.size() >= 10) break;
                boolean exists = false;
                for (Q picked : out) if (picked.id.equals(q.id)) exists = true;
                if (!exists) out.add(q);
            }
        }
        Collections.shuffle(out);
        return out;
    }

    public static List<Q> levelTest() {
        return levelTest(1);
    }

    private static void addUpTo(List<Q> out, List<Q> source, int count) {
        for (Q q : source) {
            if (kindCount(out, q.kind) >= count) break;
            out.add(q);
        }
    }

    private static int kindCount(List<Q> list, String kind) {
        int count = 0;
        for (Q q : list) if (kind.equals(q.kind)) count++;
        return count;
    }

    public static String kindLabel(String kind) {
        return KIND_WORD.equals(kind) ? "단어" : "문장";
    }
}
