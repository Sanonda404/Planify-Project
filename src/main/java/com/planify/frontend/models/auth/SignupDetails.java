package com.planify.frontend.models.auth;

public class SignupDetails {
    private String name;
    private String email;
    private String password;
    private String securityQues;
    private String quesAnswer;
    public SignupDetails(String name, String email, String password, String securityQues, String quesAnswer){
        this.email = email;
        this.name = name;
        this.password = password;
        this.securityQues = securityQues;
        this.quesAnswer = quesAnswer;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
