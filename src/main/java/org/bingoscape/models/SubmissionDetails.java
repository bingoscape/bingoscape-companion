package org.bingoscape.models;

import lombok.Data;

import java.util.Date;

@Data
public class SubmissionDetails {
    private String id;
    private String imageUrl;
    private User submittedBy;
    private Date createdAt;
}
