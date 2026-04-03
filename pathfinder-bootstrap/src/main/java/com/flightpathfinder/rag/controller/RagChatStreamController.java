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
 * SSE user-facing RAG controller.
 *
 * <p>Unlike the synchronous chat controller, this endpoint returns stage and answer progress
 * as SSE events. It still delegates all memory, trace, and mainline orchestration to the
 * streaming service instead of reimplementing pipeline logic in the web layer.</p>
 */
@RestController
@RequestMapping("/api/rag")
public class RagChatStreamController {

    private final RagStreamService ragStreamService;

    public RagChatStreamController(RagStreamService ragStreamService) {
        this.ragStreamService = ragStreamService;
    }

    /**
     * Starts the streaming RAG mainline and returns an SSE emitter immediately.
     *
     * @param request chat request from the user-facing API
     * @return SSE emitter that will receive stage, chunk, and completion events
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

    private static final class SseRagStreamEventWriter implements RagStreamEventWriter {

        private final SseEmitter emitter;

        private SseRagStreamEventWriter(SseEmitter emitter) {
            this.emitter = emitter;
        }

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

        @Override
        public void complete() {
            emitter.complete();
        }
    }
}
