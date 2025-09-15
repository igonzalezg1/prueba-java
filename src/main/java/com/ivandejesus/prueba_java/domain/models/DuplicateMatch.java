package com.ivandejesus.prueba_java.domain.models;

public class DuplicateMatch {

    private final Long sourceId;
    private final Long targetId;
    private final int precision;
    private final String firstNameMatch;
    private final String lastNameMatch;
    private final String emailMatch;
    private final String zipMatch;
    private final String addressMatch;

    public DuplicateMatch(Long sourceId, Long targetId, int precision, String firstNameMatch, String lastNameMatch, String emailMatch, String zipMatch, String addressMatch) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.precision = precision;
        this.firstNameMatch = firstNameMatch;
        this.lastNameMatch = lastNameMatch;
        this.emailMatch = emailMatch;
        this.zipMatch = zipMatch;
        this.addressMatch = addressMatch;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public int getPrecision() {
        return precision;
    }

    public String getFirstNameMatch() {
        return firstNameMatch;
    }

    public String getLastNameMatch() {
        return lastNameMatch;
    }

    public String getEmailMatch() {
        return emailMatch;
    }

    public String getZipMatch() {
        return zipMatch;
    }

    public String getAddressMatch() {
        return addressMatch;
    }
}
