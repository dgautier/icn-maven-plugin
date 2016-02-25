package com.github.dgautier.navigator.model;

/**
 * Created by DGA on 16/02/2015.
 */
/**
 * Created by DGA on 22/01/2015.
 */
public class Message {

    private String adminResponse;
    private String moreInformation;
    private String explanation;
    private int number;
    private String userResponse;
    private String text;

    public String getAdminResponse() {
        return adminResponse;
    }
    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }
    public String getMoreInformation() {
        return moreInformation;
    }
    public void setMoreInformation(String moreInformation) {
        this.moreInformation = moreInformation;
    }
    public String getExplanation() {
        return explanation;
    }
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getNumber() {
        return number;
    }
    public void setNumber(int number) {
        this.number = number;
    }
    public String getUserResponse() {
        return userResponse;
    }
    public void setUserResponse(String userResponse) {
        this.userResponse = userResponse;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
