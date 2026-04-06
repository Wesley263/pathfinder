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
 * 面向用户的 SSE RAG 控制器。
 *
 * <p>与同步聊天控制器不同，这个入口会持续返回阶段事件和回答 chunk。
 * 但 memory、trace 和主链编排依旧全部下沉到流式 service 中，而不是在 web 层重写一套流程。</p>
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
     * 启动流式 RAG 主链并立即返回 SSE 发射器。
     *
     * @param request 用户面对话请求
     * @return 后续会持续接收阶段事件、chunk 事件和结束事件的 SSE 发射器
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
     * <p>它把应用层事件模型适配成 Spring 的 SseEmitter 输出形式，使流式 service 不必直接依赖 Web 框架类型。</p>
     */
    private static final class SseRagStreamEventWriter implements RagStreamEventWriter {

        /** 当前请求对应的 SSE 发射器。 */
        private final SseEmitter emitter;

        /**
         * 构造 SSE 事件写入器。
         *
         * @param emitter SSE 发射器
         */
        private SseRagStreamEventWriter(SseEmitter emitter) {
            this.emitter = emitter;
        }

        /**
         * 输出一条 SSE 事件。
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
         * 结束当前 SSE 输出。
         */
        @Override
        public void complete() {
            emitter.complete();
        }
    }
}

