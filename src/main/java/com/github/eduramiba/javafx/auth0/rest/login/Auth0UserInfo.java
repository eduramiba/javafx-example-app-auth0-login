package com.github.eduramiba.javafx.auth0.rest.login;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.eduramiba.javafx.auth0.utils.Utils;
import java.beans.ConstructorProperties;
import java.net.URI;

public class Auth0UserInfo {

    private final String name;
    private final URI picture;
    private final String email;

    @JsonCreator
    @ConstructorProperties({"name", "picture", "email"})
    public Auth0UserInfo(String name, URI picture, String email) {
        this.name = name;
        this.picture = picture;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public URI getPicture() {
        return picture;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return Utils.toJSONPrettyPrint(this);
    }
}
