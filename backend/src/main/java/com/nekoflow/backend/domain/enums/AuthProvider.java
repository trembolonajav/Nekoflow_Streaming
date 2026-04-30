package com.nekoflow.backend.domain.enums;

public enum AuthProvider {
    LOCAL,
    GOOGLE,
    LOCAL_GOOGLE;

    public boolean includesGoogle() {
        return this == GOOGLE || this == LOCAL_GOOGLE;
    }

    public boolean includesLocal() {
        return this == LOCAL || this == LOCAL_GOOGLE;
    }
}
