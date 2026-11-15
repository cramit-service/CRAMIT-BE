package com.cramit.domain.stt;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PageMappingServiceTest {

    @Test
    void 교정은_배치_범위_안의_id에만_적용하고_범위_밖_id는_버린다() {
        List<String> texts = new ArrayList<>(List.of("비고 표기", "그대로", "다음 배치"));

        PageMappingService.applyCorrections(texts, Map.of(0, "Big-O 표기", 2, "남의 문장", 99, "없는 구간"), 0, 2);

        assertThat(texts).containsExactly("Big-O 표기", "그대로", "다음 배치");
    }
}
