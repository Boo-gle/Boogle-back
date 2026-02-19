package com.boogle.boogle.batch.util;

public class ChosungUtil {

    private static final String[] CHOSUNG_LIST = {
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ",
            "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    public static String extractChosung(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (char ch : text.toCharArray()) {
            // 한글 유니코드 영역(가~힣)인 경우에만 초성 추출
            if (ch >= 0xAC00 && ch <= 0xD7A3) {
                int chosungIndex = (ch - 0xAC00) / (21 * 28);
                result.append(CHOSUNG_LIST[chosungIndex]);
            } else {
                // 영어, 숫자, 띄어쓰기 등 그대로 유지
                result.append(ch);
            }
        }
        return result.toString();
    }
}
