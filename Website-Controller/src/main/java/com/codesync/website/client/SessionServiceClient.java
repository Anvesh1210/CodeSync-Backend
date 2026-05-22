package com.codesync.website.client;

import com.codesync.website.dto.ParticipantDto;
import com.codesync.website.dto.ParticipantRequestDto;
import com.codesync.website.dto.SessionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "session-service")
public interface SessionServiceClient {

    @PostMapping("/sessions")
    SessionDto startSession(@RequestBody SessionDto session);

    @GetMapping("/sessions/{sessionId}")
    SessionDto getSessionById(@PathVariable("sessionId") String sessionId);

    @PostMapping("/sessions/{sessionId}/join")
    ParticipantDto joinSession(@PathVariable("sessionId") String sessionId, @RequestBody ParticipantRequestDto request);

    @PostMapping("/sessions/{sessionId}/leave")
    void leaveSession(@PathVariable("sessionId") String sessionId, @RequestParam("userId") String userId);

    @PostMapping("/sessions/{sessionId}/end")
    SessionDto endSession(@PathVariable("sessionId") String sessionId, @RequestHeader("X-Caller-Id") String callerId, @RequestHeader("X-Caller-Role") String callerRole);

    @DeleteMapping("/sessions/{sessionId}")
    void endSessionAdmin(@PathVariable("sessionId") String sessionId, @RequestHeader("X-Caller-Id") String callerId, @RequestHeader("X-Caller-Role") String callerRole);

    @GetMapping("/sessions")
    List<SessionDto> getActiveSessions();

    @GetMapping("/sessions/{sessionId}/participants")
    List<ParticipantDto> getParticipants(@PathVariable("sessionId") String sessionId);

    @PostMapping("/sessions/{sessionId}/kick")
    void kickParticipant(@PathVariable("sessionId") String sessionId, @RequestHeader("X-Caller-Id") String callerId, @RequestHeader("X-Caller-Role") String callerRole, @RequestParam("targetUserId") String targetUserId);


    @GetMapping("/sessions/project/{projectId}")
    SessionDto getActiveSessionForProject(@PathVariable("projectId") String projectId);
}
