
package com.poorvi.url_shortener.integration;

import com.poorvi.url_shortener.url.entity.ShortUrl;
import com.poorvi.url_shortener.url.repository.ShortUrlRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ShortUrlRepositoryIntegrationTest {

    @Autowired
    private ShortUrlRepository repository;

    private ShortUrl createTestUrl() {
        return ShortUrl.builder()
                .originalUrl("https://www.google.com")
                .shortCode("TEST123")
                .createdAt(LocalDateTime.now())
                .expiresAt(null)
                .owner(null)
                .build();
    }

    @Test
    void shouldSaveShortUrlInPostgreSQL() {
        ShortUrl savedUrl =
                repository.saveAndFlush(createTestUrl());

        assertThat(savedUrl.getId()).isNotNull();
        assertThat(savedUrl.getShortCode()).isEqualTo("TEST123");
        assertThat(savedUrl.getOriginalUrl())
                .isEqualTo("https://www.google.com");
    }

    @Test
    void shouldFindShortUrlByShortCode() {
        repository.saveAndFlush(createTestUrl());

        Optional<ShortUrl> result =
                repository.findByShortCode("TEST123");

        assertThat(result).isPresent();
        assertThat(result.get().getOriginalUrl())
                .isEqualTo("https://www.google.com");
    }

    @Test
    void shouldCheckIfShortCodeExists() {
        repository.saveAndFlush(createTestUrl());

        assertThat(repository.existsByShortCode("TEST123"))
                .isTrue();

        assertThat(repository.existsByShortCode("UNKNOWN"))
                .isFalse();
    }

    @Test
    void shouldDeleteShortUrlFromPostgreSQL() {
        ShortUrl savedUrl =
                repository.saveAndFlush(createTestUrl());

        Long id = savedUrl.getId();

        repository.deleteById(id);
        repository.flush();

        assertThat(repository.findById(id)).isEmpty();
        assertThat(repository.existsByShortCode("TEST123"))
                .isFalse();
    }
}
