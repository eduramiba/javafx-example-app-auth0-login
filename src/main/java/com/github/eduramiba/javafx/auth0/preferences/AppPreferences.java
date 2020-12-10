package com.github.eduramiba.javafx.auth0.preferences;

import com.github.eduramiba.javafx.auth0.UserInfo;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.prefs.Preferences;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppPreferences {

    private static final Logger LOG = LoggerFactory.getLogger(AppPreferences.class);

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppPreferences.class);

    private static class Key {

        private final String id;

        public Key(String id) {
            this.id = Objects.requireNonNull(StringUtils.trimToNull(id));
        }

        /**
         * <p>
         * Generates a SHA-256 hash of a String. Since {@link Preferences#MAX_KEY_LENGTH} is 80, if the key is over 80 characters, it will lead to an exception while saving.
         * </p>
         * <p>
         * This method generates a SHA-256 hash of the key to save / load as the key in {@link Preferences}, since those are guaranteed to be maximum 64 chars long.
         * </p>
         *
         * @param key the string for which to calculate the hash
         * @return SHA-256 representation of key
         */
        @Override
        public String toString() {
            if (id.length() > Preferences.MAX_KEY_LENGTH) {
                return DigestUtils.sha256Hex(id);
            } else {
                return id;
            }
        }
    }

    public static <T> Optional<T> getGlobalPreference(final String key, final Function<String, T> mapper) {
        return Optional.ofNullable(getPreference(new Key(key), mapper));
    }

    public static void saveGlobalPreference(final String key, final Object value) {
        savePreference(new Key(key), value);
    }

    public static void removeGlobalPreference(final String key) {
        savePreference(new Key(key), null);
    }

    private static String userKey(UserInfo user) {
        return user != null ? user.getEmail() : "NO_USER";
    }

    private static <T> T getPreference(final Key key, final Function<String, T> mapper) {
        try {
            final String value = PREFS.get(key.toString(), null);

            if (StringUtils.isBlank(value)) {
                return null;
            }

            LOG.debug("Read preference {} = {}", key, value);

            return mapper.apply(value);
        } catch (Throwable ex) {
            LOG.error("Error reading preference {}", key, ex);
            return null;
        }
    }

    private static void savePreference(final Key key, final Object value) {
        try {
            LOG.debug("Saving preference {} = {}", key, value);

            if (value != null && !StringUtils.isBlank(value.toString())) {
                final String strValue = value.toString();

                PREFS.put(key.toString(), strValue);
            } else {
                PREFS.remove(key.toString());
            }
        } catch (Throwable ex) {
            LOG.error("Error saving preference {}", key, ex);
        }
    }

    private static Key buildKey(final String userKey, final String key) {
        return new Key(userKey + "." + key);
    }

    private static Key buildKey(final String userKey, final String key, final Object id, final String secondaryKey) {
        return new Key(userKey + "." + key + id + "." + secondaryKey);
    }
}
