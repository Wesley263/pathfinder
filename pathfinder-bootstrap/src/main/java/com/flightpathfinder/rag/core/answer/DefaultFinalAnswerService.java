package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import org.springframework.stereotype.Service;

/**
 * Default final-answer orchestrator.
 *
 * <p>This class keeps final-answer assembly and text composition together without reaching
 * back into retrieval or stage-one logic.</p>
 */
@Service
public class DefaultFinalAnswerService implements FinalAnswerService {

    private final FinalAnswerAssembler finalAnswerAssembler;
    private final FinalAnswerTextComposer finalAnswerTextComposer;

    public DefaultFinalAnswerService(FinalAnswerAssembler finalAnswerAssembler,
                                     FinalAnswerTextComposer finalAnswerTextComposer) {
        this.finalAnswerAssembler = finalAnswerAssembler;
        this.finalAnswerTextComposer = finalAnswerTextComposer;
    }

    /**
     * Builds the final answer from retrieval output.
     *
     * @param retrievalResult retrieval-stage output containing KB and MCP contexts
     * @return structured answer result, including partial and empty semantics
     */
    @Override
    public AnswerResult answer(RetrievalResult retrievalResult) {
        FinalAnswerPromptInput promptInput = finalAnswerAssembler.assemble(retrievalResult);
        if (promptInput.empty()) {
            return AnswerResult.empty(
                    "No final answer can be assembled yet because neither KB nor MCP returned usable context.",
                    promptInput.evidenceSummaries());
        }

        String answerText = finalAnswerTextComposer.compose(promptInput);
        return new AnswerResult(
                resolveStatus(promptInput),
                answerText,
                promptInput.partial(),
                promptInput.snapshotMissAffected(),
                answerText.isBlank(),
                promptInput.evidenceSummaries());
    }

    private String resolveStatus(FinalAnswerPromptInput promptInput) {
        if (promptInput.empty()) {
            return "EMPTY";
        }
        if (promptInput.snapshotMissAffected() && promptInput.partial()) {
            return "PARTIAL";
        }
        if (promptInput.snapshotMissAffected()) {
            return "SNAPSHOT_MISS";
        }
        if ("DATA_NOT_FOUND".equals(promptInput.mcpContext().status()) && promptInput.kbContext().empty()) {
            return "DATA_NOT_FOUND";
        }
        if (promptInput.partial()) {
            return "PARTIAL";
        }
        return "SUCCESS";
    }
}
