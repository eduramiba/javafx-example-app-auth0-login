package com.github.eduramiba.javafx.auth0.rest;

import org.apache.commons.lang3.StringUtils;

public class ClientAuthenticationCredentials {

    private final String requestHeader;
    private final String secret;

    private static final String DEFAULT_REQUEST_HEADER = "authorization";

    public ClientAuthenticationCredentials(final String secret) {
        this(DEFAULT_REQUEST_HEADER, secret);
    }

    public ClientAuthenticationCredentials(final String requestHeader, final String secret) {
        this.requestHeader = requestHeader;
        this.secret = secret;
    }

    public String getRequestHeader() {
        return requestHeader;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public String toString() {
        String secretPartial = secret;
        if (secretPartial != null) {
            int lengthToShow = Math.min(18, secretPartial.length() / 3);
            secretPartial = StringUtils.abbreviateMiddle(secret, "...", lengthToShow);
        }

        return "ClientCredentials{" + "requestHeader=" + requestHeader + ", secret=" + secretPartial + "}";
    }

    public static ClientAuthenticationCredentials jwtToken(final String secret) {
        if (StringUtils.isBlank(secret)) {
            throw new IllegalArgumentException("JWT token mandatory");
        }

        return new ClientAuthenticationCredentials(HTTPHeaders.HEADER_AUTHORIZATION, "Bearer " + secret);
    }
}
