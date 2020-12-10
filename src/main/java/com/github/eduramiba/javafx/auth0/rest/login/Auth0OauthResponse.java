package com.github.eduramiba.javafx.auth0.rest.login;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.eduramiba.javafx.auth0.utils.Utils;
import java.beans.ConstructorProperties;

public class Auth0OauthResponse {

    private final String accessToken;
    private final String idToken;
    private final String tokenType;
    private final long expiresIn;

    @JsonCreator
    @ConstructorProperties({"access_token", "id_token", "token_type", "expires_in"})
    public Auth0OauthResponse(String access_token, String id_token, String token_type, long expires_in) {
        this.accessToken = access_token;
        this.idToken = id_token;
        this.tokenType = token_type;
        this.expiresIn = expires_in;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    @Override
    public String toString() {
        return Utils.toJSONPrettyPrint(this);
    }

}
