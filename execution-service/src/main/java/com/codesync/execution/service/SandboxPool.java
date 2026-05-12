package com.codesync.execution.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SandboxPool {
    
    private final ExecutorService workerPool = Executors.newFixedThreadPool(10);
    
    public void submitTask(Runnable task) {
        workerPool.submit(task);
    }
}
