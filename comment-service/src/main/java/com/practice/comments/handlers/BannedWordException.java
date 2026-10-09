package com.practice.comments.handlers;

public class BannedWordException extends RuntimeException {

    public BannedWordException(String word) {
        super("Comment contains a banned word: " + word);
    }
}
