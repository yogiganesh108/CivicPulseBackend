package com.example.backend.dto;

import java.time.LocalDate;

public class AssignRequest {
    private Long officer_id;
    private String priority;
    private LocalDate deadline;

    public Long getOfficer_id() { return officer_id; }
    public void setOfficer_id(Long officer_id) { this.officer_id = officer_id; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
}
