package com.github.eduramiba.javafx.auth0;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.beans.ConstructorProperties;
import java.util.Objects;

public class UserInfo {

    private final String name;
    private final String email;
    private final String avatarURL;
    private final String jwtToken;

    @JsonCreator
    @ConstructorProperties({"name", "email", "avatarURL", "jwtToken"})
    public UserInfo(String name, String email, String avatarURL, String jwtToken) {
        this.name = name;
        this.email = email;
        this.avatarURL = avatarURL;
        this.jwtToken = jwtToken;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.email);
        hash = 97 * hash + Objects.hashCode(this.avatarURL);
        hash = 97 * hash + Objects.hashCode(this.jwtToken);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserInfo other = (UserInfo) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.email, other.email)) {
            return false;
        }
        if (!Objects.equals(this.avatarURL, other.avatarURL)) {
            return false;
        }
        if (!Objects.equals(this.jwtToken, other.jwtToken)) {
            return false;
        }
        return true;
    }

    public static class Builder {

        private String name;
        private String email;
        private String avatarURL;
        private String jwtToken;

        private Builder() {
        }

        public Builder name(final String value) {
            this.name = value;
            return this;
        }

        public Builder email(final String value) {
            this.email = value;
            return this;
        }

        public Builder avatarURL(final String value) {
            this.avatarURL = value;
            return this;
        }

        public Builder jwtToken(final String value) {
            this.jwtToken = value;
            return this;
        }

        public UserInfo build() {
            return new UserInfo(name, email, avatarURL, jwtToken);
        }
    }

    public static UserInfo.Builder builder() {
        return new UserInfo.Builder();
    }

}
