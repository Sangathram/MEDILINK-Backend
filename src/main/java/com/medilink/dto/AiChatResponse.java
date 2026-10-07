package com.medilink.dto;

public class AiChatResponse {

    private String response;

    private boolean emergency;

    private String language;

    public AiChatResponse() {
    }

    public AiChatResponse(
            String response,
            boolean emergency,
            String language
    ) {
        this.response = response;
        this.emergency = emergency;
        this.language = language;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isEmergency() {
        return emergency;
    }

    public void setEmergency(boolean emergency) {
        this.emergency = emergency;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}