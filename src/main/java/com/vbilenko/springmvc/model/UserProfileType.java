package com.vbilenko.springmvc.model;

import javax.persistence.Embedded;
import java.io.Serializable;

public enum UserProfileType implements Serializable {

    USER("USER"),
    DBA("DBA"),
    ADMIN("ADMIN");

    @Embedded
    String userProfileType;

    UserProfileType(String userProfileType) {
        this.userProfileType = userProfileType;
    }

    public String getUserProfileType() {
        return userProfileType;
    }

}
