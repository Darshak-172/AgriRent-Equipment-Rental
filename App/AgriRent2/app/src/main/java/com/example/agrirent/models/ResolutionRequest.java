package com.example.agrirent.models;

import com.google.gson.annotations.SerializedName;

public class ResolutionRequest {
    @SerializedName("resolutionNote")
    private String resolutionNote;

    public ResolutionRequest(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
}
