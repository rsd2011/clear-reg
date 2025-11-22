package com.example.server.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.sensitive-api")
public class SensitiveApiProperties {

    private boolean validationEnabled = true;
    private String reasonParameter = "reasonCode";
    private String legalBasisParameter = "legalBasisCode";
    private List<String> endpoints = new ArrayList<>();

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    public String getReasonParameter() {
        return reasonParameter;
    }

    public void setReasonParameter(String reasonParameter) {
        this.reasonParameter = reasonParameter;
    }

    public String getLegalBasisParameter() {
        return legalBasisParameter;
    }

    public void setLegalBasisParameter(String legalBasisParameter) {
        this.legalBasisParameter = legalBasisParameter;
    }

    public List<String> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }
}
