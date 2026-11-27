package com.cramit.domain.chat.retrieval;

public record PageContext(
        Integer pageNumber,
        String title,
        String content
) {
    // 페이지 사이를 구분할 때 쓰는 구분자. 예산 계산과 실제 전송 문자열이 같은 기준을 쓰도록 여기서 고정한다.
    public static final String SEPARATOR = "\n\n";

    // 컨텍스트에 실릴 때의 실제 형태. 예산 계산과 모델에 보내는 문자열이 같은 길이 기준을 쓰도록 여기서 통일한다.
    public String formatted() {
        return "[p." + pageNumber + "]" + (title != null ? " " + title : "") + "\n" + content;
    }
}
