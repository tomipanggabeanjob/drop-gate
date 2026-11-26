package com.dropgate.queue;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class QueueController {

    private final QueueAdmissionService queueAdmissionService;

    public QueueController(QueueAdmissionService queueAdmissionService) {
        this.queueAdmissionService = queueAdmissionService;
    }

    @PostMapping("/drops/{dropId}/queue/join")
    public QueueAdmissionResponse admit(@PathVariable String dropId, @RequestBody QueueAdmissionRequest request) {
        return queueAdmissionService.admit(dropId, request);
    }

    @GetMapping("/drops/{dropId}/queue/position")
    public QueuePositionResponse position(@PathVariable String dropId, @RequestParam String accountId) {
        return queueAdmissionService.position(dropId, accountId);
    }

    @GetMapping("/drops/{dropId}/queue/stream")
    public SseEmitter stream(@PathVariable String dropId, @RequestParam String accountId) {
        return queueAdmissionService.stream(dropId, accountId);
    }
}
