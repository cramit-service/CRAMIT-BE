package com.cramit.domain.summary;

import java.util.ArrayList;
import java.util.List;

/**
 * 스크립트 구간(segment)들을 순서대로 이어 붙이되, 한 덩어리가 maxChars를 넘지 않도록 나눈다.
 * 구간 하나가 maxChars보다 길면 그 구간만 글자 수 기준으로 잘라서 넣는다.
 */
final class SummaryChunker {

    private static final String SEPARATOR = "\n\n";

    private SummaryChunker() {
    }

    static List<String> split(List<String> segments, int maxChars) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String segment : segments) {
            for (String piece : cut(segment, maxChars)) {
                int lengthAfterAppend = current.length() + (current.isEmpty() ? 0 : SEPARATOR.length()) + piece.length();
                if (lengthAfterAppend > maxChars && !current.isEmpty()) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
                if (!current.isEmpty()) {
                    current.append(SEPARATOR);
                }
                current.append(piece);
            }
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private static List<String> cut(String segment, int maxChars) {
        List<String> pieces = new ArrayList<>();
        for (int start = 0; start < segment.length(); start += maxChars) {
            pieces.add(segment.substring(start, Math.min(segment.length(), start + maxChars)));
        }
        return pieces;
    }
}
