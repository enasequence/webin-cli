package uk.ac.ebi.ena.entity;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Date;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Version {
    public Boolean valid;
    public Boolean update;
    public Boolean expire;
    public String minVersion;
    public String latestVersion;
    public String nextMinVersion;
    public Date nextMinVersionDate;
    public String comment;
}
