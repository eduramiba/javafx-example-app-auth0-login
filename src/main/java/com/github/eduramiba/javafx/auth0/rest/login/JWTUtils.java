package com.github.eduramiba.javafx.auth0.rest.login;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Instant;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

public class JWTUtils {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JWTUtils.class);

    private static final int MIN_VALID_SECONDS_LEFT = 60 * 10;

    public static Optional<DecodedJWT> verifyToken(final String token) {
        if (StringUtils.isBlank(token)) {
            LOG.info("Empty token??");
            return Optional.empty();
        }

        final DecodedJWT jwt = JWT.decode(token);

        if (jwt != null) {
            final Instant now = Instant.now();
            final Instant expiresAt = Instant.ofEpochMilli(jwt.getExpiresAt().getTime());

            //Expired or near to expiry:
            if (now.plusSeconds(MIN_VALID_SECONDS_LEFT).isAfter(expiresAt)) {
                LOG.info("Token expired: {}", expiresAt);
                return Optional.empty();
            } else {
                return Optional.of(jwt);
            }
        } else {
            return Optional.empty();
        }
    }

}
