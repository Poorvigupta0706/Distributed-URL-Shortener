
package com.poorvi.url_shortener.analytics.repository;

import com.poorvi.url_shortener.analytics.entity.UrlClick;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlClickRepository
        extends JpaRepository<UrlClick, Long> {

    boolean existsByEventId(String eventId);

    long countByShortCode(String shortCode);
}

