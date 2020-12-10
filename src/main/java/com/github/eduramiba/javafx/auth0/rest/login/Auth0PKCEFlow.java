package com.github.eduramiba.javafx.auth0.rest.login;

import com.github.eduramiba.javafx.auth0.UserInfo;
import com.github.eduramiba.javafx.auth0.rest.ClientAuthenticationCredentials;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;

/**
 * https://auth0.com/docs/flows/guides/auth-code-pkce/add-login-auth-code-pkce#example-authorization-url
 *
 * @author Eduardo
 */
public class Auth0PKCEFlow {

    public static class FlowInfo {

        private final String verifier;
        private final String challenge;
        private final String state;
        private final String authorizeUrl;

        public FlowInfo(String verifier, String challenge, String state, String authorizeUrl) {
            this.verifier = verifier;
            this.challenge = challenge;
            this.state = state;
            this.authorizeUrl = authorizeUrl;
        }

        public String getVerifier() {
            return verifier;
        }

        public String getChallenge() {
            return challenge;
        }

        public String getState() {
            return state;
        }

        public String getAuthorizeUrl() {
            return authorizeUrl;
        }

    }

    private static final String DOMAIN = "your-domain.eu.auth0.com";
    private static final String CLIENT_ID = "your_client_id";
    private static final String REDIRECT_URI = "https://" + DOMAIN + "/mobile";

    public static String createCodeVerifier() {
        final SecureRandom sr = new SecureRandom();
        final byte[] code = new byte[32];
        sr.nextBytes(code);

        return Base64.encodeBase64URLSafeString(code);
    }

    public static String createCodeChallenge(final String codeVerifier) {
        final byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
        final MessageDigest md = Unchecked.supplier(() -> MessageDigest.getInstance("SHA-256")).get();
        md.update(bytes, 0, bytes.length);

        return Base64.encodeBase64URLSafeString(md.digest());
    }

    public static String createAuthorizationURL(final String challenge, final String state) {

        return "https://" + DOMAIN + "/authorize"
                + "?client_id=" + CLIENT_ID
                + "&response_type=code"
                + "&code_challenge_method=S256"
                + "&code_challenge=" + challenge
                + "&scope=openid profile email"
                + "&state=" + state
                + "&redirect_uri=" + REDIRECT_URI;
    }

    public static FlowInfo createAuthorizationFlow() {
        final String verifier = createCodeVerifier();
        final String challenge = createCodeChallenge(verifier);
        final String state = RandomStringUtils.randomAlphanumeric(8);
        final String authorizationURL = createAuthorizationURL(challenge, state);

        return new FlowInfo(verifier, challenge, state, authorizationURL);
    }

    public static Optional<UserInfo> checkURLForLoginSuccess(final FlowInfo flowInfo, final String url) throws IOException {
        final URI uri = URI.create(url);

        if (url.startsWith(REDIRECT_URI)) {
            var fragmentParams = getQueryParams(uri);

            final String code = fragmentParams.get("code");
            final String state = fragmentParams.get("state");

            if (flowInfo.getState().equals(state) && !StringUtils.isEmpty(code)) {
                final Auth0OauthResponse tokenInfo;

                try (RESTAuth0Client client = buildAuth0Client(null)) {
                    tokenInfo = client.getOauthToken(CLIENT_ID, code, flowInfo.getVerifier(), REDIRECT_URI);

                    return JWTUtils.verifyToken(tokenInfo.getIdToken()).map(jwt -> {
                        return UserInfo.builder()
                                .jwtToken(tokenInfo.getIdToken())
                                .email(jwt.getClaim("email").asString())
                                .name(jwt.getClaim("name").asString())
                                .avatarURL(jwt.getClaim("picture").asString())
                                .build();
                    });
                }
            }
        }

        return Optional.empty();
    }

    private static RESTAuth0Client buildAuth0Client(final String accessToken) {
        return new RESTAuth0Client(URI.create("https://" + DOMAIN + "/"), accessToken != null ? new ClientAuthenticationCredentials("Bearer " + accessToken) : null);
    }

    private static Map<String, String> getQueryParams(URI uri) {
        final Map<String, String> fragmentParams = new LinkedHashMap<>();
        final String query = uri.getQuery();
        if (query == null) {
            return fragmentParams;
        }

        final String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            fragmentParams.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
        }

        return fragmentParams;
    }
}
