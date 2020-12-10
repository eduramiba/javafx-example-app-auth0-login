package com.github.eduramiba.javafx.auth0.utils;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * I18N utility class..
 */
public final class I18N {

    private static final List<Locale> SUPPORTED_LOCALES = Collections.unmodifiableList(
            Arrays.asList(Locale.ENGLISH, new Locale("es"))
    );

    /**
     * get the supported Locales.
     *
     * @return List of Locale objects.
     */
    public static List<Locale> getSupportedLocales() {
        return SUPPORTED_LOCALES;
    }

    /**
     * get the default locale. This is the systems default if contained in the supported locales, english otherwise.
     *
     * @return
     */
    public static Locale getDefaultLocale() {
        final Locale sysDefault = Locale.getDefault();

        if (sysDefault == null) {
            return Locale.ENGLISH;
        }

        if (SUPPORTED_LOCALES.contains(sysDefault)) {
            return sysDefault;
        }

        return SUPPORTED_LOCALES
                .stream()
                .filter(l -> l.getLanguage().equals(sysDefault.getLanguage()))
                .findFirst()
                .orElse(Locale.ENGLISH);
    }

    public static ResourceBundle getBundle(final String bundle) {
        return ResourceBundle.getBundle(bundle, I18N.getDefaultLocale());
    }

    public static void setLocale(Locale locale) {
        Locale.setDefault(locale);
    }
}
