package com.flightpathfinder.rag.core.answer;

/**
 * Turns assembled answer input into the final answer text.
 *
 * <p>This boundary keeps text composition separate from evidence collection so later LLM or
 * prompt-based generation can replace the current deterministic composer without changing
 * retrieval orchestration.</p>
 */
public interface FinalAnswerTextComposer {

    /**
     * Composes answer text from normalized answer input.
     *
     * @param promptInput normalized answer-generation input
     * @return final answer text shown to the caller
     */
    String compose(FinalAnswerPromptInput promptInput);
}
