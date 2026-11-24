package com.aistudymentor.ai.service;

import com.aistudymentor.ai.dto.AnswerRequest;
import com.aistudymentor.ai.dto.GeneratedQuiz;
import com.aistudymentor.ai.dto.QuizRequest;
import com.aistudymentor.ai.dto.StructuredAnswer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiProvider implements AiProvider {

    private final GeminiAdapterService adapter;
    private final PromptTemplateService prompts;

    public GeminiProvider(GeminiAdapterService adapter, PromptTemplateService prompts) {
        this.adapter = adapter;
        this.prompts = prompts;
    }

    @Override
    public StructuredAnswer answer(AnswerRequest request) {
        return adapter.generateAnswer(request, prompts.answer(request));
    }

    @Override
    public GeneratedQuiz generateQuiz(QuizRequest request) {
        return adapter.generateQuiz(request, prompts.quiz(request));
    }

    @Override
    public String modelId() {
        return adapter.getModelId();
    }
}
