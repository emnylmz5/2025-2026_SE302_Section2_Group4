
package org.example.se302;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Conflict {

    private ConflictType type;
    private List<ExamSession> sessions;
    private String description;

    public Conflict() {
        this.sessions = new ArrayList<>();
    }

    public Conflict(ConflictType type, List<ExamSession> sessions, String description) {
        this.type = type;
        this.sessions = (sessions == null) ? new ArrayList<>() : new ArrayList<>(sessions);
        this.description = description;
    }

    public ConflictType getType() {
        return type;
    }

    public void setType(ConflictType type) {
        this.type = type;
    }

    public List<ExamSession> getSessions() {
        return sessions == null ? List.of() : Collections.unmodifiableList(sessions);
    }

    public void setSessions(List<ExamSession> sessions) {
        this.sessions = (sessions == null) ? new ArrayList<>() : new ArrayList<>(sessions);
    }

    public void addSession(ExamSession session) {
        if (session == null) return;
        if (this.sessions == null) this.sessions = new ArrayList<>();
        this.sessions.add(session);
    }

    public void removeSession(ExamSession session) {
        if (this.sessions != null) {
            this.sessions.remove(session);
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        int count = (sessions == null) ? 0 : sessions.size();
        return "Conflict{" +
                "type=" + type +
                ", sessions=" + count +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conflict conflict)) return false;
        return type == conflict.type
                && Objects.equals(sessions, conflict.sessions)
                && Objects.equals(description, conflict.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, sessions, description);
    }
}
