package com.example.server.web;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.PermissionDeniedException;
import com.example.auth.permission.RequirePermission;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.request.DraftCreateRequest;
import com.example.draft.application.response.DraftResponse;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/drafts")
public class DraftViewController {

    private final DraftApplicationService draftApplicationService;

    public DraftViewController(DraftApplicationService draftApplicationService) {
        this.draftApplicationService = draftApplicationService;
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_READ)
    public String listPage(Pageable pageable, Model model) {
        AuthContext context = requireContext();
        boolean audit = hasAuditPermission();
        RowScope rowScope = audit ? RowScope.ALL : context.rowScope();
        Collection<String> scopedOrganizations = audit ? List.of() : List.of(context.organizationCode());
        Page<DraftResponse> drafts = draftApplicationService.listDrafts(pageable, rowScope, context.organizationCode(), scopedOrganizations);
        model.addAttribute("drafts", drafts.getContent());
        return "draft/list";
    }

    @GetMapping("/new")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_CREATE)
    public String newDraftForm(Model model) {
        model.addAttribute("draft", new DraftCreateRequest("", "", "", null, null, "{}", List.of()));
        return "draft/form";
    }

    @PostMapping
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_CREATE)
    public String submitDraft(@Valid @ModelAttribute("draft") DraftCreateRequest request,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "draft/form";
        }
        AuthContext context = requireContext();
        DraftResponse created = draftApplicationService.createDraft(request, context.username(), context.organizationCode());
        return "redirect:/drafts/" + created.id();
    }

    @GetMapping("/{id}")
    @RequirePermission(feature = FeatureCode.DRAFT, action = ActionCode.DRAFT_READ)
    public String detailPage(@PathVariable UUID id, Model model) {
        AuthContext context = requireContext();
        boolean audit = hasAuditPermission();
        DraftResponse draft = draftApplicationService.getDraft(id, context.organizationCode(), audit);
        var history = draftApplicationService.listHistory(id, context.organizationCode(), audit);
        var references = draftApplicationService.listReferences(id, context.organizationCode(), audit);
        model.addAttribute("draft", draft);
        model.addAttribute("history", history);
        model.addAttribute("references", references);
        return "draft/detail";
    }

    private AuthContext requireContext() {
        return AuthContextHolder.current()
                .orElseThrow(() -> new PermissionDeniedException("인증 정보가 없습니다."));
    }

    private boolean hasAuditPermission() {
        try {
            return AuthContextHolder.current()
                    .map(AuthContext::rowScope)
                    .map(scope -> scope == RowScope.ALL)
                    .orElse(false);
        } catch (Exception ex) {
            return false;
        }
    }
}
