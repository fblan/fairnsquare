package org.asymetrik.web.fairnsquare.split.service;

import org.asymetrik.web.fairnsquare.sharedkernel.validation.Violations;

/**
 * Request DTO for creating a new split.
 */
public class CreateSplitRequest implements Violations.Validated {

    @Override
    public void validate() {
        Violations.check().notBlank(name, "Split name is required").orThrow();
    }

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
