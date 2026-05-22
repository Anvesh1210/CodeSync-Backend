package com.codesync.execution.repository;

import com.codesync.execution.entity.LanguageConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<LanguageConfig, Long> {
    Optional<LanguageConfig> findByNameIgnoreCase(String name);
}
