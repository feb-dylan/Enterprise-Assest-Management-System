package com.eams.dto.request;

import jakarta.validation.constraints.Size;

public class AssetReturnRequest {

    @Size(max = 500, message = "Return note must not exceed 500 characters")
    private String returnNote;

    public AssetReturnRequest() {
    }

    public String getReturnNote() {
        return returnNote;
    }

    public void setReturnNote(String returnNote) {
        this.returnNote = returnNote;
    }
}