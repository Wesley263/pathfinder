package com.flightpathfinder.infra.ai.chat;

import com.flightpathfinder.infra.ai.model.ModelCapability;
import com.flightpathfinder.infra.ai.model.ModelRoutingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
/**
 * 默认占位聊天实现。
 *
 * 当业务侧未提供真实 ChatService Bean 时启用，
 * 用于保持链路可运行并返回明确的占位提示。
 */

@Component
@ConditionalOnMissingBean(ChatService.class)
public class NoopChatService implements ChatService, StreamingChatService {

    private final ModelRoutingService modelRoutingService;

    public NoopChatService(ModelRoutingService modelRoutingService) {
        this.modelRoutingService = modelRoutingService;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String modelName = modelRoutingService.selectPrimary(ModelCapability.CHAT)
                .map(route -> route.modelName())
                .orElse("unconfigured");
        return new ChatResponse("RAG chat has not been migrated into pathfinder2.0 yet.", modelName, true);
    }

    @Override
    public void stream(ChatRequest request, StreamCallback callback) {
        callback.onChunk(chat(request).content());
        callback.onComplete();
    }
}

