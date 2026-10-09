package com.practice.comments.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * app.comments.* settings. A mutable class with setters on purpose: on POST /actuator/refresh,
 * Spring Cloud re-binds @ConfigurationProperties beans in place, which needs setters
 * (an immutable record would keep its old values until restart).
 */
@ConfigurationProperties(prefix = "app.comments")
public class CommentRules {

    private List<String> bannedWords = new ArrayList<>();

    public List<String> getBannedWords() {
        return bannedWords;
    }

    public void setBannedWords(List<String> bannedWords) {
        this.bannedWords = bannedWords;
    }

    /** Returns the first banned word found in the text, or null. */
    public String findBannedWord(String text) {
        String lower = text.toLowerCase();
        return bannedWords.stream()
                .filter(word -> !word.isBlank() && lower.contains(word.toLowerCase()))
                .findFirst()
                .orElse(null);
    }
}
