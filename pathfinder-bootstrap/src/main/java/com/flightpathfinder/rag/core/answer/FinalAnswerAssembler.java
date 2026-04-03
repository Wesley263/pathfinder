package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.retrieve.RetrievalResult;

/**
 * Assembles answer-generation input from retrieval output.
 *
 * <p>This extra layer keeps evidence gathering and prompt-shape decisions separate from the
 * text composer, which makes the final-answer stage easier to evolve.</p>
 */
public interface FinalAnswerAssembler {

    /**
     * Converts retrieval output into the normalized prompt/input model used by the final
     * answer composer.
     *
     * @param retrievalResult retrieval-stage output
     * @return normalized answer input model
     */
    FinalAnswerPromptInput assemble(RetrievalResult retrievalResult);
}
