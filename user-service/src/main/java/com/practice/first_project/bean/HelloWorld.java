package com.practice.first_project.bean;

// Returned by HelloWorldController; Jackson turns getMessage() into {"message":"..."}
public class HelloWorld {

    String message;

    public HelloWorld(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
