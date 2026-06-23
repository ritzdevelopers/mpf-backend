package com.mypropertyfact.estate.models;

public final class BlogStatus {
    public static final int INACTIVE = 0;
    public static final int PUBLISHED = 1;
    public static final int DRAFT = 2;
    public static final int SCHEDULED = 3;

    private BlogStatus() {
    }

    public static boolean isPubliclyVisible(int status) {
        return status == PUBLISHED;
    }
}
