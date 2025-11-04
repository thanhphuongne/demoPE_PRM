package com.thunderboarsolution.MVVMretrofiltrequest.network;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for subject metadata returned by the mockapi endpoint.
 * Expected fields:
 * - id: unique string identifier
 * - name: subject display name
 * - iconUrl: URL of the subject icon image
 */
public class Subject {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("iconUrl")
    private String iconUrl;

    public Subject() {
    }

    public Subject(String id, String name, String iconUrl) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIconUrl() {
        return iconUrl;
    }
}