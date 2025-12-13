package org.example.se302;

import java.util.List;

public class Conflict {

    private ConflictType type;
    private List<ExamSession> sessions;
    private String description;

    public Conflict(ConflictType type, List<ExamSession> sessions, String description) {
        this.type = type;
        this.sessions = sessions;
        this.description = description;
    }

    public ConflictType getType() {
        return type;
    }

    public void setType(ConflictType type) {
        this.type = type;
    }

    public List<ExamSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<ExamSession> sessions) {
        this.sessions = sessions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
