package com.aistudymentor.ai.service;

import com.aistudymentor.ai.dto.AnswerRequest;
import com.aistudymentor.ai.dto.GeneratedQuiz;
import com.aistudymentor.ai.dto.QuizRequest;
import com.aistudymentor.ai.dto.StructuredAnswer;

public interface AiProvider {

    StructuredAnswer answer(AnswerRequest request);

    GeneratedQuiz generateQuiz(QuizRequest request);

    String modelId();
}
