package entity;



import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Objects;


public class InstagramUser {
    private String username;
    private String userID;
    private String userFullName;
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public InstagramUser(String username, String userID, String userFullName, String token) {
        this.username = username;
        this.userID = userID;
        this.userFullName = userFullName;
        this.token = token;
    }

    public JSONObject getJSONObject() throws JSONException {
        JSONObject userJSon = new JSONObject();
        userJSon.put("username", username.trim());
        userJSon.put("fullName", userFullName);
        return userJSon;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }


    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof InstagramUser)) {
            return false;
        }
        InstagramUser user = (InstagramUser) o;
        return Objects.equals(userID, user.userID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID);
    }

    @Override
    public String toString() {
        return getUserID().trim() +"\t" + getUsername().trim() +"\t"+getUserFullName().trim();
    }
}
