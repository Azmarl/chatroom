package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.repository.SensitiveWordRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SensitiveWordService {

    private final SensitiveWordRepository sensitiveWordRepository;

    // A cache to hold the sensitive words in memory.
    // Using a Set provides fast O(1) average time complexity for lookups.
    private Set<String> sensitiveWordCache = Collections.emptySet();

    @Autowired
    public SensitiveWordService(SensitiveWordRepository sensitiveWordRepository) {
        this.sensitiveWordRepository = sensitiveWordRepository;
    }

    /**
     * This method is automatically called by Spring after the bean is created.
     * It populates the cache with words from the database at application startup.
     */
    @PostConstruct
    public void loadWordsIntoCache() {
        List<String> words = sensitiveWordRepository.findAllWords();
        if (words != null && !words.isEmpty()) {
            this.sensitiveWordCache = words.stream()
                    .map(String::toLowerCase) // Store in lowercase for case-insensitive matching
                    .collect(Collectors.toSet());
            System.out.println("Loaded " + sensitiveWordCache.size() + " sensitive words into cache.");
        } else {
            System.out.println("No sensitive words found in the database to load into cache.");
            this.sensitiveWordCache = Collections.emptySet();
        }
        // In a production environment, you might want to add a @Scheduled task
        // to refresh this cache periodically if the database table changes frequently.
    }

    /**
     * Checks if the given text contains any of the cached sensitive words.
     * The comparison is case-insensitive.
     *
     * @param text The text content to validate.
     * @return true if a sensitive word is found, false otherwise.
     */
    public boolean containsSensitiveWords(String text) {
        if (text == null || text.isEmpty() || sensitiveWordCache.isEmpty()) {
            return false;
        }

        String lowerCaseText = text.toLowerCase();

        // Check if any word from our cache is present in the input text.
        return sensitiveWordCache.stream()
                .anyMatch(lowerCaseText::contains);
    }
}