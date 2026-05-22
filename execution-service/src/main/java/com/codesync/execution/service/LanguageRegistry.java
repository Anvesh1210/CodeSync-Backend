package com.codesync.execution.service;

import com.codesync.execution.entity.LanguageConfig;
import com.codesync.execution.repository.LanguageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LanguageRegistry {

    private final LanguageRepository languageRepository;
    private final Map<String, LanguageConfig> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        log.info("Refreshing language configuration cache...");
        List<LanguageConfig> configs = languageRepository.findAll();
        cache.clear();
        for (LanguageConfig config : configs) {
            cache.put(config.getName().toLowerCase(), config);
        }
        log.info("Loaded {} languages into cache", cache.size());
    }

    public Optional<LanguageConfig> getConfig(String language) {
        return Optional.ofNullable(cache.get(language.toLowerCase()));
    }

    public List<LanguageConfig> getAllConfigs() {
        return List.copyOf(cache.values());
    }

    public boolean isSupported(String language) {
        return cache.containsKey(language.toLowerCase());
    }
}
