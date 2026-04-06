package com.flightpathfinder.rag.controller;

import com.flightpathfinder.framework.context.RequestIdHolder;
import com.flightpathfinder.rag.controller.request.RagChatRequest;
import com.flightpathfinder.rag.service.RagStreamService;
import com.flightpathfinder.rag.service.model.RagStreamCommand;
import com.flightpathfinder.rag.service.model.RagStreamEvent;
import com.flightpathfinder.rag.service.model.RagStreamEventWriter;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
@RestController
@RequestMapping("/api/rag")
public class RagChatStreamController {

    /** 流式主链应用服务。 */
    private final RagStreamService ragStreamService;

    /**
     * 构造流式聊天控制器。
     *
     * @param ragStreamService 流式主链应用服务
     */
    public RagChatStreamController(RagStreamService ragStreamService) {
        this.ragStreamService = ragStreamService;
    }

    /**
     * 说明。
     *
     * @param request 用户面对话请求
     * @return 返回结果。
     */
    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody RagChatRequest request) {
        String requestId = RequestIdHolder.getOrCreate();
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onTimeout(emitter::complete);
        ragStreamService.stream(
                new RagStreamCommand(request.question(), request.conversationId(), requestId),
                new SseRagStreamEventWriter(emitter));
        return emitter;
    }

    /**
     * 流式事件写入器实现。
     *
     * 说明。
     */
    private static final class SseRagStreamEventWriter implements RagStreamEventWriter {

        /** 注释说明。 */
        private final SseEmitter emitter;

        /**
         * 说明。
         *
         * @param emitter 参数说明。
         */
        private SseRagStreamEventWriter(SseEmitter emitter) {
            this.emitter = emitter;
        }

        /**
         * 说明。
         *
         * @param event 待发送事件
         */
        @Override
        public void emit(RagStreamEvent event) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(event.sequence()))
                        .name(event.event())
                        .data(event));
            } catch (IOException exception) {
                throw new IllegalStateException("failed to send SSE event", exception);
            }
        }

        /**
         * 说明。
         */
        @Override
        public void complete() {
            emitter.complete();
        }
    }
}

