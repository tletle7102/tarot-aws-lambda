package com.tarot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TarotDeck {

    private static final List<TarotCard> MAJOR_ARCANA = List.of(
        new TarotCard(0, "The Fool", "광대", "새로운 시작, 순수함, 모험", "무모함, 경솔함"),
        new TarotCard(1, "The Magician", "마법사", "창조력, 의지력, 기술", "속임수, 능력 낭비"),
        new TarotCard(2, "The High Priestess", "여사제", "직관, 무의식, 내면의 지혜", "비밀, 감정 억압"),
        new TarotCard(3, "The Empress", "여황제", "풍요, 모성, 자연", "의존, 과잉보호"),
        new TarotCard(4, "The Emperor", "황제", "권위, 안정, 리더십", "독재, 경직됨"),
        new TarotCard(5, "The Hierophant", "교황", "전통, 가르침, 신념", "독단, 형식주의"),
        new TarotCard(6, "The Lovers", "연인", "사랑, 조화, 선택", "불화, 잘못된 선택"),
        new TarotCard(7, "The Chariot", "전차", "승리, 결단력, 전진", "방향 상실, 공격성"),
        new TarotCard(8, "Strength", "힘", "용기, 인내, 내면의 힘", "자기 의심, 나약함"),
        new TarotCard(9, "The Hermit", "은둔자", "내면 탐색, 고독, 지혜", "고립, 외로움"),
        new TarotCard(10, "Wheel of Fortune", "운명의 수레바퀴", "변화, 행운, 순환", "불운, 저항"),
        new TarotCard(11, "Justice", "정의", "공정, 진실, 균형", "불공정, 부정직"),
        new TarotCard(12, "The Hanged Man", "매달린 사람", "희생, 새로운 관점, 기다림", "지연, 무의미한 희생"),
        new TarotCard(13, "Death", "죽음", "끝과 새로운 시작, 변환", "변화에 대한 저항"),
        new TarotCard(14, "Temperance", "절제", "균형, 조화, 인내", "불균형, 과잉"),
        new TarotCard(15, "The Devil", "악마", "유혹, 집착, 물질주의", "해방, 속박에서 벗어남"),
        new TarotCard(16, "The Tower", "탑", "갑작스러운 변화, 파괴, 깨달음", "변화 회피, 재앙 지속"),
        new TarotCard(17, "The Star", "별", "희망, 영감, 치유", "절망, 희망 상실"),
        new TarotCard(18, "The Moon", "달", "환상, 불안, 잠재의식", "혼란 해소, 두려움 극복"),
        new TarotCard(19, "The Sun", "태양", "성공, 기쁨, 활력", "일시적 우울, 과도한 낙관"),
        new TarotCard(20, "Judgement", "심판", "재생, 부름, 자기 평가", "자기 비판, 후회"),
        new TarotCard(21, "The World", "세계", "완성, 성취, 통합", "미완성, 지연")
    );

    public static List<DrawnCard> drawByIds(List<Integer> cardIds) {
        List<DrawnCard> drawn = new ArrayList<>();
        for (int id : cardIds) {
            if (id < 0 || id >= MAJOR_ARCANA.size()) {
                throw new IllegalArgumentException("Invalid card id: " + id);
            }
            TarotCard card = MAJOR_ARCANA.get(id);
            boolean isReversed = ThreadLocalRandom.current().nextBoolean();
            drawn.add(new DrawnCard(card, isReversed));
        }
        return drawn;
    }

    public record DrawnCard(TarotCard card, boolean reversed) {
        public String displayName() {
            return card.nameKo() + (reversed ? " (역방향)" : " (정방향)");
        }

        public String displayMeaning() {
            return reversed ? card.reversed() : card.meaning();
        }
    }
}
