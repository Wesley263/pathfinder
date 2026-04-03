package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.retrieve.RetrievalResult;

/**
 * Final-answer stage boundary.
 *
 * <p>This service consumes retrieval output and produces the answer contract for user-facing
 * APIs. It does not own retrieval or request orchestration.</p>
 */
public interface FinalAnswerService {

    /**
     * Builds the final answer from retrieval output.
     *
     * @param retrievalResult retrieval-stage output containing KB and MCP contexts
     * @return structured answer result, including partial and empty semantics
     */
    AnswerResult answer(RetrievalResult retrievalResult);
}
