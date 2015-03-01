package com.github.dgautier.navigator.model;

/**
 * Created by DGA on 16/02/2015.
 * @deprecated should use model class from icn-tools instead
 */
public class JsonResponse {

    private String userid;
    private String user_displayname;
    private String security_token;
    private Message[] errors;
    private Message[] messages;


    public String getUserid() {
        return userid;
    }
    protected void setUserid(String userid) {
        this.userid = userid;
    }
    public String getUser_displayname() {
        return user_displayname;
    }
    protected void setUser_displayname(String user_displayname) {
        this.user_displayname = user_displayname;
    }
    public String getSecurity_token() {
        return security_token;
    }
    protected void setSecurity_token(String security_token) {
        this.security_token = security_token;
    }
    public Message[] getErrors() {
        return errors;
    }
    protected void setErrors(Message[] errors) {
        this.errors = errors;
    }

    public boolean hasErrors(){
        if (errors != null && errors.length > 0){
            return true;
        }
        return false;
    }

    public boolean hasMessages(){
        if (messages != null && messages.length > 0){
            return true;
        }
        return false;
    }

    public String getErrorMessage(){
        if (errors == null){
            return "No errors found";
        }
        return getMessages(errors);
    }


    public String getInfoMessage(){
        if (messages == null){
            return "No messages found";
        }
        return getMessages(messages);
    }

    private String getMessages(Message[] messages) {

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < messages.length; i++){
            Message message = messages[i];
            builder.append(message.getText());
            builder.append(System.lineSeparator());
            if (message.getUserResponse() != null){
                builder.append(message.getUserResponse());
                builder.append(System.lineSeparator());
            }
        }

        return builder.toString();
    }
}