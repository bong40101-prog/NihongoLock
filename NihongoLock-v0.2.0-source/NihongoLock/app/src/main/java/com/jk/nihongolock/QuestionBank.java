package com.jk.nihongolock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class QuestionBank {
    public static class Q {
        public final String prompt;
        public final String[] options;
        public final int answer;
        public final String explanation;
        public final int level;

        public Q(String prompt, String[] options, int answer, String explanation, int level) {
            this.prompt = prompt;
            this.options = options;
            this.answer = answer;
            this.explanation = explanation;
            this.level = level;
        }
    }

    private static final List<Q> ALL = new ArrayList<>();
    static {
        ALL.add(new Q("『おはようございます』의 뜻은?", new String[]{"안녕하세요(아침)", "잘 자요", "감사합니다", "다녀오겠습니다"}, 0, "아침 인사로 쓰는 정중한 표현입니다.", 1));
        ALL.add(new Q("『友達』의 읽기는?", new String[]{"ともだち", "かぞく", "せんせい", "りょこう"}, 0, "友達(ともだち) = 친구", 1));
        ALL.add(new Q("『ビール』는 무엇일까요?", new String[]{"맥주", "물", "소주", "차"}, 0, "ビール = beer = 맥주", 1));
        ALL.add(new Q("『日本へ行きます』에서 へ의 역할은?", new String[]{"방향", "소유", "목적어", "과거"}, 0, "장소 + へ는 '~로/에'라는 이동 방향을 나타냅니다.", 2));
        ALL.add(new Q("'친구와 이자카야에 갑니다'에 가장 가까운 문장은?", new String[]{"友達と居酒屋に行きます", "友達が居酒屋を食べます", "居酒屋と友達を見ます", "友達に居酒屋がいます"}, 0, "友達と = 친구와, 居酒屋に行きます = 이자카야에 갑니다.", 2));
        ALL.add(new Q("『何時ですか』의 뜻은?", new String[]{"몇 시예요?", "얼마예요?", "어디예요?", "누구예요?"}, 0, "何時(なんじ) = 몇 시", 2));
        ALL.add(new Q("호텔에서 '체크인 부탁드립니다'로 자연스러운 것은?", new String[]{"チェックインをお願いします", "チェックインを食べます", "チェックインが嫌いです", "チェックインを帰ります"}, 0, "～をお願いします = ~을 부탁드립니다.", 2));
        ALL.add(new Q("『一緒に飲みませんか』의 뜻은?", new String[]{"같이 한잔하지 않을래요?", "혼자 마실게요", "술은 싫어요", "계산해주세요"}, 0, "～ませんか는 정중한 권유 표현입니다.", 3));
        ALL.add(new Q("'최근에 아오모리에도 갔습니다'로 자연스러운 것은?", new String[]{"最近は青森にも行きました", "最近が青森を行きます", "青森に最近を食べました", "最近も青森が来ません"}, 0, "最近は + 장소にも + 行きました가 자연스럽습니다.", 3));
        ALL.add(new Q("『おすすめは何ですか』는 언제 쓰기 좋을까요?", new String[]{"추천 메뉴를 물을 때", "화장실 위치를 물을 때", "계산할 때", "예약을 취소할 때"}, 0, "おすすめ = 추천/추천 메뉴", 3));
        ALL.add(new Q("『もう少しゆっくり話してもらえますか』의 의미는?", new String[]{"조금 더 천천히 말해 주실 수 있나요?", "조금 더 싸게 해주세요", "조금 더 주세요", "다시 오겠습니다"}, 0, "ゆっくり話す = 천천히 말하다", 3));
        ALL.add(new Q("『予約しているクォンです』의 자연스러운 해석은?", new String[]{"예약한 권입니다", "예약을 취소한 권입니다", "권 씨를 예약합니다", "예약 시간이 권입니다"}, 0, "호텔/식당에서 예약자 이름을 말할 때 유용합니다.", 3));
        ALL.add(new Q("『せっかく日本に来たので、地元の料理を食べたいです』의 뉘앙스는?", new String[]{"모처럼 일본에 왔으니 현지 음식을 먹고 싶다", "일본 음식은 먹기 싫다", "이미 현지 음식을 먹었다", "일본에 오지 못했다"}, 0, "せっかく～ので = 모처럼 ~했으니/한 만큼", 4));
        ALL.add(new Q("『もし時間があれば』에 이어지기 자연스러운 것은?", new String[]{"一緒に行きませんか", "昨日でした", "高くありません", "駅があります"}, 0, "もし時間があれば = 만약 시간이 있다면", 4));
        ALL.add(new Q("『思ったより混んでいますね』의 의미는?", new String[]{"생각보다 붐비네요", "생각보다 싸네요", "아무도 없네요", "길을 잃었네요"}, 0, "思ったより = 생각했던 것보다", 4));
        ALL.add(new Q("『飲みすぎないようにします』의 의미는?", new String[]{"너무 많이 마시지 않도록 할게요", "많이 마시겠습니다", "술을 주문하겠습니다", "술이 부족합니다"}, 0, "～ないようにする = ~하지 않도록 하다", 4));
        ALL.add(new Q("『行けたら行きます』가 대화에서 가질 수 있는 뉘앙스는?", new String[]{"갈 수 있으면 갈게요", "반드시 갑니다", "절대 안 갑니다", "이미 갔습니다"}, 0, "직역은 '갈 수 있다면 갈게요'이며 확정 약속보다 약한 표현입니다.", 4));
        ALL.add(new Q("『せっかくだから、もう一軒行こう』의 자연스러운 뜻은?", new String[]{"이왕 이렇게 된 거 한 군데 더 가자", "이제 집에 가자", "한 잔만 마시자", "가게가 문을 닫았다"}, 0, "もう一軒 = 가게 한 곳 더 / 2차 느낌으로 자주 씁니다.", 5));
        ALL.add(new Q("『日本語はまだまだですが、旅行で困らない程度には話せるようになりたいです』의 핵심 의미는?", new String[]{"여행에서 곤란하지 않을 정도로 말하고 싶다", "일본어를 이미 완벽하게 한다", "여행 일본어는 필요 없다", "일본에서 살 계획이다"}, 0, "～程度には = ~정도까지는, ～ようになりたい = 할 수 있게 되고 싶다.", 5));
        ALL.add(new Q("『おすすめしてもらった店、すごく良かったです』의 의미는?", new String[]{"추천받은 가게가 정말 좋았어요", "추천을 거절했어요", "가게를 찾지 못했어요", "추천 메뉴가 없었어요"}, 0, "おすすめしてもらった = 추천을 받은", 5));
        ALL.add(new Q("『話せば話すほど慣れてくる気がします』의 의미는?", new String[]{"말하면 말할수록 익숙해지는 것 같아요", "말할수록 어려워져요", "말하지 않는 편이 좋아요", "이미 완전히 익숙해요"}, 0, "～ば～ほど = ~하면 할수록", 5));
        ALL.add(new Q("『言いたいことは分かるけど、もう少し自然に言うなら…』는 어떤 상황?", new String[]{"뜻은 통하지만 더 자연스러운 표현을 제안할 때", "전혀 이해 못할 때", "대화를 끝낼 때", "가격을 흥정할 때"}, 0, "자유회화 첨삭에서 자주 쓰는 피드백 표현입니다.", 6));
        ALL.add(new Q("『別に嫌いなわけじゃない』의 뉘앙스는?", new String[]{"딱히 싫어하는 건 아니야", "정말 싫어", "매우 좋아", "관심이 전혀 없어"}, 0, "～わけじゃない = 반드시/꼭 ~인 것은 아니다.", 6));
        ALL.add(new Q("『それ、さすがに無理じゃない？』의 자연스러운 느낌은?", new String[]{"그건 아무리 그래도 무리 아니야?", "그건 정말 쉽지?", "그걸 꼭 사고 싶어", "그건 이미 끝났어"}, 0, "さすがに는 '아무리 그래도/역시 그 정도는' 같은 뉘앙스로 자주 씁니다.", 6));
    }

    private QuestionBank() {}

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
        return new Q(q.prompt, opts, newAnswer, q.explanation, q.level);
    }

    public static List<Q> forLevel(int level) {
        List<Q> out = new ArrayList<>();
        int low = Math.max(1, level - 1);
        int high = Math.min(6, level + 1);
        for (Q q : ALL) if (q.level >= low && q.level <= high) out.add(q);
        Collections.shuffle(out);
        return out;
    }

    public static List<Q> levelTest() {
        List<Q> pool = new ArrayList<>(ALL);
        Collections.shuffle(pool, new Random());
        // Ensure a spread of difficulty rather than only random beginner questions.
        List<Q> out = new ArrayList<>();
        for (int level = 1; level <= 6; level++) {
            int added = 0;
            for (Q q : pool) {
                if (q.level == level && added < 2) {
                    out.add(q);
                    added++;
                }
            }
        }
        return out;
    }
}
