package com.planify.frontend.models.auth;

public class SecurityAnswerVerification {
    private String email;
    private String answer;
    public SecurityAnswerVerification(String email, String answer){
        this.email  = email;
        this.answer = answer;
    }
}
