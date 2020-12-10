package com.github.eduramiba.javafx.auth0;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainControllerModel {

    private static final Logger LOG = LoggerFactory.getLogger(MainControllerModel.class);

    private final Property<UserInfo> userInfo = new SimpleObjectProperty<>();

    public MainControllerModel() {
    }

    public Property<UserInfo> getUserInfo() {
        return userInfo;
    }

    public synchronized void setUserInfo(final UserInfo userInfo) {
        if (userInfo != null) {
            LOG.info("Set user info for email: {}", userInfo.getEmail());
        } else {
            LOG.info("Set user info: none");
        }
        this.userInfo.setValue(userInfo);
    }

}
