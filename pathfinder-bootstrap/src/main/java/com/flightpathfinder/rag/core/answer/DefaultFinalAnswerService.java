package com.flightpathfinder.rag.core.answer;

import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import org.springframework.stereotype.Service;

/**
 * 最终回答阶段的默认编排器。
 *
 * 说明。
 * 说明。
 */
@Service
public class DefaultFinalAnswerService implements FinalAnswerService {

    /** 注释说明。 */
    private final FinalAnswerAssembler finalAnswerAssembler;
    /** 负责生成最终回答文本。 */
    private final FinalAnswerTextComposer finalAnswerTextComposer;

    /**
     * 说明。
     *
     * @param finalAnswerAssembler 回答输入装配器
     * @param finalAnswerTextComposer 回答文本生成器
     */
    public DefaultFinalAnswerService(FinalAnswerAssembler finalAnswerAssembler,
                                     FinalAnswerTextComposer finalAnswerTextComposer) {
        this.finalAnswerAssembler = finalAnswerAssembler;
        this.finalAnswerTextComposer = finalAnswerTextComposer;
    }

    /**
     * 说明。
     *
     * @param retrievalResult 参数说明。
     * @return 结构化回答结果
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

    /**
     * 说明。
     *
     * @param promptInput 回答输入模型
     * @return 回答状态
     */
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

