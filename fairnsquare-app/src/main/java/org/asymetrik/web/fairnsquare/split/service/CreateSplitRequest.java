package org.asymetrik.web.fairnsquare.split.service;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new split.
 */
public class CreateSplitRequest {

    @NotBlank(message = "Split name is required")
    private String name;

    public CreateSplitRequest() {
    }

    public CreateSplitRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
