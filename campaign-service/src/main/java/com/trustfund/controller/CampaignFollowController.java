package com.trustfund.controller;

import com.trustfund.service.CampaignFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaign-follows")
@RequiredArgsConstructor
@Tag(name = "Campaign Follow", description = "API theo dõi (follow) chiến dịch")
public class CampaignFollowController {

    private final CampaignFollowService campaignFollowService;

    @PostMapping("/{campaignId}")
    @Operation(summary = "Follow a campaign", description = "Follow a campaign by current authenticated user")
    public ResponseEntity<Void> follow(@PathVariable Long campaignId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        campaignFollowService.follow(campaignId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{campaignId}")
    @Operation(summary = "Unfollow a campaign", description = "Unfollow a campaign by current authenticated user")
    public ResponseEntity<Void> unfollow(@PathVariable Long campaignId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        campaignFollowService.unfollow(campaignId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{campaignId}/me")
    @Operation(summary = "Check if I follow a campaign", description = "Return true/false if current user follows campaign")
    public ResponseEntity<Map<String, Boolean>> isFollowing(@PathVariable Long campaignId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        boolean following = campaignFollowService.isFollowing(campaignId, userId);
        return ResponseEntity.ok(Map.of("following", following));
    }

    @GetMapping("/{campaignId}/count")
    @Operation(summary = "Count followers of a campaign", description = "Public endpoint to get follower count")
    public ResponseEntity<Map<String, Long>> countFollowers(@PathVariable Long campaignId) {
        long count = campaignFollowService.countFollowers(campaignId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/{campaignId}/followers")
    @Operation(summary = "Get followers by campaignId", description = "Public endpoint to get follower list (userId + followedAt)")
    public ResponseEntity<List<com.trustfund.model.response.CampaignFollowerResponse>> getFollowers(@PathVariable Long campaignId) {
        return ResponseEntity.ok(campaignFollowService.getFollowersByCampaignId(campaignId));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my followed campaigns", description = "Get list of campaignIds followed by current user")
    public ResponseEntity<List<Long>> getMyFollowedCampaignIds() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());

        return ResponseEntity.ok(campaignFollowService.getMyFollowedCampaignIds(userId));
    }
}

