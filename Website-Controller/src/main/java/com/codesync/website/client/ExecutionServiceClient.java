package com.codesync.website.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "execution-service")
public interface ExecutionServiceClient {

    @PostMapping("/executions")
    Object runCode(@RequestBody Object executionRequest);

    @GetMapping("/executions/{id}/result")
    Object getResult(@PathVariable("id") String id);

    @GetMapping("/executions")
    Object getAllExecutions();

    @PostMapping("/executions/{id}/cancel")
    Object cancelExecution(@PathVariable("id") String id);

    @GetMapping("/executions/stats")
    Object getExecutionStats();
}
