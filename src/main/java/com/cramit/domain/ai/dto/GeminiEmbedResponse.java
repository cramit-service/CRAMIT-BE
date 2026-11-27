package com.cramit.domain.ai.dto;

import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;

import java.util.List;

public record GeminiEmbedResponse(Embedding embedding) {

    public record Embedding(List<Double> values) {}

    public float[] toFloatArray() {
        if (embedding == null || embedding.values() == null || embedding.values().isEmpty()) {
            throw new BusinessException(ErrorCode.EMBEDDING_GENERATION_FAILED);
        }
        List<Double> values = embedding.values();
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }
}
