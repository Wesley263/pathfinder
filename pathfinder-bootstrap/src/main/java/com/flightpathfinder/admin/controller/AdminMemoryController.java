package com.flightpathfinder.admin.controller;

import com.flightpathfinder.admin.controller.request.AdminConversationListRequest;
import com.flightpathfinder.admin.controller.vo.AdminConversationDetailBodyVO;
import com.flightpathfinder.admin.controller.vo.AdminConversationDetailVO;
import com.flightpathfinder.admin.controller.vo.AdminConversationListVO;
import com.flightpathfinder.admin.controller.vo.AdminConversationMessageVO;
import com.flightpathfinder.admin.controller.vo.AdminConversationSummaryDetailVO;
import com.flightpathfinder.admin.controller.vo.AdminConversationSummaryVO;
import com.flightpathfinder.admin.service.AdminConversationDetail;
import com.flightpathfinder.admin.service.AdminConversationDetailResult;
import com.flightpathfinder.admin.service.AdminConversationListResult;
import com.flightpathfinder.admin.service.AdminConversationMessageItem;
import com.flightpathfinder.admin.service.AdminConversationSummaryDetail;
import com.flightpathfinder.admin.service.AdminConversationSummaryItem;
import com.flightpathfinder.admin.service.AdminMemoryService;
import com.flightpathfinder.framework.convention.Result;
import com.flightpathfinder.framework.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话记忆巡检的管理端 API。
 *
 * <p>该控制器提供面向管理场景的持久化记忆读模型，
 * 使运维能够查看会话而不与运行时记忆模型耦合。
 */
@RestController
@RequestMapping("/api/admin/conversations")
public class AdminMemoryController {

    private final AdminMemoryService adminMemoryService;

    public AdminMemoryController(AdminMemoryService adminMemoryService) {
        this.adminMemoryService = adminMemoryService;
    }

    /**
        * 列出持久化会话供管理端巡检。
     *
     * @param request optional list filter and limit
     * @return admin-facing conversation list view
     */
    @GetMapping
    public Result<AdminConversationListVO> list(@ModelAttribute AdminConversationListRequest request) {
        AdminConversationListResult result = adminMemoryService.listConversations(request.getConversationId(), request.getLimit());
        return Results.success(toListVO(result));
    }

    /**
        * 加载单个持久化会话详情视图。
     *
     * @param conversationId exact conversation id to inspect
     * @return admin-facing conversation detail
     */
    @GetMapping("/{conversationId}")
    public Result<AdminConversationDetailVO> detail(@PathVariable String conversationId) {
        AdminConversationDetailResult result = adminMemoryService.findConversationDetail(conversationId, 20);
        return Results.success(toDetailVO(result));
    }

    private AdminConversationListVO toListVO(AdminConversationListResult result) {
        return new AdminConversationListVO(
                result.status(),
                result.requestedConversationId(),
                result.message(),
                result.limit(),
                result.count(),
                result.conversations().stream()
                        .map(this::toSummaryVO)
                        .toList());
    }

    private AdminConversationDetailVO toDetailVO(AdminConversationDetailResult result) {
        return new AdminConversationDetailVO(
                result.conversationId(),
                result.status(),
                result.message(),
                result.detail() == null ? null : toDetailBodyVO(result.detail()));
    }

    private AdminConversationSummaryVO toSummaryVO(AdminConversationSummaryItem item) {
        return new AdminConversationSummaryVO(
                item.conversationId(),
                item.lastRequestId(),
                item.turnCount(),
                item.messageCount(),
                item.hasSummary(),
                item.summaryUpdatedAt(),
                item.createdAt(),
                item.updatedAt());
    }

    private AdminConversationDetailBodyVO toDetailBodyVO(AdminConversationDetail detail) {
        return new AdminConversationDetailBodyVO(
                detail.conversationId(),
                detail.lastRequestId(),
                detail.turnCount(),
                detail.messageCount(),
                detail.createdAt(),
                detail.updatedAt(),
                detail.hasSummary(),
                toSummaryDetailVO(detail.summary()),
                detail.recentMessages().stream()
                        .map(this::toMessageVO)
                        .toList());
    }

    private AdminConversationSummaryDetailVO toSummaryDetailVO(AdminConversationSummaryDetail detail) {
        return new AdminConversationSummaryDetailVO(
                detail.hasSummary(),
                detail.summaryText(),
                detail.summarizedTurnCount(),
                detail.updatedAt());
    }

    private AdminConversationMessageVO toMessageVO(AdminConversationMessageItem messageItem) {
        return new AdminConversationMessageVO(
                messageItem.messageId(),
                messageItem.requestId(),
                messageItem.messageIndex(),
                messageItem.role(),
                messageItem.content(),
                messageItem.rewrittenQuestion(),
                messageItem.answerStatus(),
                messageItem.createdAt());
    }
}
