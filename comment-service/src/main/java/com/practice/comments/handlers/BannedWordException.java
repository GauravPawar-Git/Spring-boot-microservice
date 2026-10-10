package com.practice.comments.handlers;

// Thrown by CommentService.addComment() when CommentRules.findBannedWord() finds one; ErrorResponseHandler -> 400
public class BannedWordException extends RuntimeException {

    public BannedWordException(String word) {
        super("Comment contains a banned word: " + word);
    }
}
