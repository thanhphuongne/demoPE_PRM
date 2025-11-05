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

    // Accept common alternates seen in mock API payloads
    @SerializedName(value = "id", alternate = { "subject_id" })
    private String id;

    @SerializedName(value = "name", alternate = { "subject_name", "subject", "title" })
    private String name;

    @SerializedName(value = "iconUrl", alternate = { "icon", "image", "icon_url", "iconLink", "iconURI" })
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