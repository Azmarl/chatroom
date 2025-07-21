package com.chatroom.chatroombackend.repository;

import com.chatroom.chatroombackend.entity.SensitiveWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Integer> {

    /**
     * Efficiently fetches only the 'word' column from all entries.
     * This is more memory-efficient than fetching the entire SensitiveWord object.
     * @return A list of all sensitive word strings.
     */
    @Query("SELECT sw.word FROM SensitiveWord sw")
    List<String> findAllWords();
}