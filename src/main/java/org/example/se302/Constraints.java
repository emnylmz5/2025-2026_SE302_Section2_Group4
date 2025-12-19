package org.example.se302;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Constraints {

    private int minMinutesBetweenExams;
    private int maxExamsPerDay;
    private List<DayOfWeek> allowedDays;
    private List<TimeRange> allowedTimeRanges;
    private Map<String, String> roomSpecificRules;

    private LocalDate examWeekStartDate;
    private LocalDate examWeekEndDate;

    private int roomTurnoverMinutes;

    private int slotStepMinutes;
    private int baseExamDurationMinutes;
    private int creditDurationCoefficientMinutes;
    private int durationRoundingMinutes;
    private int minExamDurationMinutes;

    public Constraints() {
        this.minMinutesBetweenExams = 60;
        this.maxExamsPerDay = 2;
        this.allowedDays = new ArrayList<>(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        this.allowedTimeRanges = new ArrayList<>();
        this.allowedTimeRanges.add(new TimeRange(LocalTime.of(9, 0), LocalTime.of(19, 0)));
        this.roomSpecificRules = new HashMap<>();
        this.examWeekStartDate = null;
        this.examWeekEndDate = null;
        this.roomTurnoverMinutes = 10;
        this.slotStepMinutes = 5;
        this.baseExamDurationMinutes = 90;
        this.creditDurationCoefficientMinutes = 15;
        this.durationRoundingMinutes = 5;
        this.minExamDurationMinutes = 120;
    }

    public Constraints(int minMinutesBetweenExams,
                       int maxExamsPerDay,
                       List<DayOfWeek> allowedDays,
                       List<TimeRange> allowedTimeRanges,
                       Map<String, String> roomSpecificRules,
                       LocalDate examWeekStartDate,
                       LocalDate examWeekEndDate,
                       int roomTurnoverMinutes) {
        setMinMinutesBetweenExams(minMinutesBetweenExams);
        setMaxExamsPerDay(maxExamsPerDay);
        setAllowedDays(allowedDays);
        setAllowedTimeRanges(allowedTimeRanges);
        setRoomSpecificRules(roomSpecificRules);
        setExamWeekStartDate(examWeekStartDate);
        setExamWeekEndDate(examWeekEndDate);
        setRoomTurnoverMinutes(roomTurnoverMinutes);
        // Defaults
        this.slotStepMinutes = 5;
        this.baseExamDurationMinutes = 90;
        this.creditDurationCoefficientMinutes = 15;
        this.durationRoundingMinutes = 5;
        this.minExamDurationMinutes = 120;
    }

    public int getMinMinutesBetweenExams() {
        return minMinutesBetweenExams;
    }

    public void setMinMinutesBetweenExams(int minMinutesBetweenExams) {
        if (minMinutesBetweenExams < 0) {
            throw new IllegalArgumentException("minMinutesBetweenExams cannot be negative");
        }
        this.minMinutesBetweenExams = minMinutesBetweenExams;
    }

    public int getMaxExamsPerDay() {
        return maxExamsPerDay;
    }

    public void setMaxExamsPerDay(int maxExamsPerDay) {
        if (maxExamsPerDay < 1) {
            throw new IllegalArgumentException("maxExamsPerDay must be at least 1");
        }
        this.maxExamsPerDay = maxExamsPerDay;
    }

    public List<DayOfWeek> getAllowedDays() {
        return allowedDays == null ? List.of() : Collections.unmodifiableList(allowedDays);
    }

    public void setAllowedDays(List<DayOfWeek> allowedDays) {
        this.allowedDays = (allowedDays == null) ? new ArrayList<>() : new ArrayList<>(allowedDays);
    }

    public List<TimeRange> getAllowedTimeRanges() {
        return allowedTimeRanges == null ? List.of() : Collections.unmodifiableList(allowedTimeRanges);
    }

    public void setAllowedTimeRanges(List<TimeRange> allowedTimeRanges) {
        this.allowedTimeRanges = (allowedTimeRanges == null) ? new ArrayList<>() : new ArrayList<>(allowedTimeRanges);
    }

    public void setRoomSpecificRules(Map<String, String> roomSpecificRules) {
        this.roomSpecificRules = (roomSpecificRules == null) ? new HashMap<>() : new HashMap<>(roomSpecificRules);
    }

    public LocalDate getExamWeekStartDate() {
        return examWeekStartDate;
    }

    public void setExamWeekStartDate(LocalDate examWeekStartDate) {
        this.examWeekStartDate = examWeekStartDate;
        validateExamWeekDates();
    }

    public LocalDate getExamWeekEndDate() {
        return examWeekEndDate;
    }

    public void setExamWeekEndDate(LocalDate examWeekEndDate) {
        this.examWeekEndDate = examWeekEndDate;
        validateExamWeekDates();
    }

    public boolean isWithinExamWeek(LocalDate date) {
        if (date == null) return false;
        if (examWeekStartDate == null || examWeekEndDate == null) return true;
        return (!date.isBefore(examWeekStartDate)) && (!date.isAfter(examWeekEndDate));
    }

    private void validateExamWeekDates() {
        if (examWeekStartDate == null || examWeekEndDate == null) return;
        if (examWeekStartDate.isAfter(examWeekEndDate)) {
            throw new IllegalArgumentException("examWeekStartDate cannot be after examWeekEndDate");
        }
    }

    public int getRoomTurnoverMinutes() {
        return roomTurnoverMinutes;
    }

    public void setRoomTurnoverMinutes(int roomTurnoverMinutes) {
        if (roomTurnoverMinutes < 0) {
            throw new IllegalArgumentException("roomTurnoverMinutes cannot be negative");
        }
        this.roomTurnoverMinutes = roomTurnoverMinutes;
    }

    public int getSlotStepMinutes() {
        return slotStepMinutes;
    }

    public void setSlotStepMinutes(int slotStepMinutes) {
        if (slotStepMinutes < 1) {
            throw new IllegalArgumentException("slotStepMinutes must be at least 1");
        }
        this.slotStepMinutes = slotStepMinutes;
    }

    public int getBaseExamDurationMinutes() {
        return baseExamDurationMinutes;
    }

    public void setBaseExamDurationMinutes(int baseExamDurationMinutes) {
        if (baseExamDurationMinutes < 1) {
            throw new IllegalArgumentException("baseExamDurationMinutes must be at least 1");
        }
        this.baseExamDurationMinutes = baseExamDurationMinutes;
    }

    public int getCreditDurationCoefficientMinutes() {
        return creditDurationCoefficientMinutes;
    }

    public void setCreditDurationCoefficientMinutes(int creditDurationCoefficientMinutes) {
        if (creditDurationCoefficientMinutes < 0) {
            throw new IllegalArgumentException("creditDurationCoefficientMinutes cannot be negative");
        }
        this.creditDurationCoefficientMinutes = creditDurationCoefficientMinutes;
    }

    public int getDurationRoundingMinutes() {
        return durationRoundingMinutes;
    }

    public void setDurationRoundingMinutes(int durationRoundingMinutes) {
        if (durationRoundingMinutes < 1) {
            throw new IllegalArgumentException("durationRoundingMinutes must be at least 1");
        }
        this.durationRoundingMinutes = durationRoundingMinutes;
    }

    public int getMinExamDurationMinutes() {
        return minExamDurationMinutes;
    }

    public void setMinExamDurationMinutes(int minExamDurationMinutes) {
        if (minExamDurationMinutes < 1) {
            throw new IllegalArgumentException("minExamDurationMinutes must be at least 1");
        }
        this.minExamDurationMinutes = minExamDurationMinutes;
    }

    // --- Aliases to make UI wiring easier (same underlying fields) ---
    public int getExamBaseDurationMinutes() {
        return getBaseExamDurationMinutes();
    }

    public void setExamBaseDurationMinutes(int minutes) {
        setBaseExamDurationMinutes(minutes);
    }

    public int getMinutesPerCredit() {
        return getCreditDurationCoefficientMinutes();
    }

    public void setMinutesPerCredit(int minutes) {
        setCreditDurationCoefficientMinutes(minutes);
    }

    public int getRoundingMinutes() {
        return getDurationRoundingMinutes();
    }

    public void setRoundingMinutes(int minutes) {
        setDurationRoundingMinutes(minutes);
    }

    public int getRoundToMinutes() {
        return getDurationRoundingMinutes();
    }

    public void setRoundToMinutes(int minutes) {
        setDurationRoundingMinutes(minutes);
    }

    @Override
    public String toString() {
        return "Constraints{" +
                "minMinutesBetweenExams=" + minMinutesBetweenExams +
                ", maxExamsPerDay=" + maxExamsPerDay +
                ", allowedDays=" + (allowedDays == null ? 0 : allowedDays.size()) +
                ", allowedTimeRanges=" + (allowedTimeRanges == null ? 0 : allowedTimeRanges.size()) +
                ", roomSpecificRules=" + (roomSpecificRules == null ? 0 : roomSpecificRules.size()) +
                ", examWeekStartDate=" + examWeekStartDate +
                ", examWeekEndDate=" + examWeekEndDate +
                ", roomTurnoverMinutes=" + roomTurnoverMinutes +
                ", slotStepMinutes=" + slotStepMinutes +
                ", baseExamDurationMinutes=" + baseExamDurationMinutes +
                ", creditDurationCoefficientMinutes=" + creditDurationCoefficientMinutes +
                ", durationRoundingMinutes=" + durationRoundingMinutes +
                ", minExamDurationMinutes=" + minExamDurationMinutes +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Constraints that)) return false;
        return minMinutesBetweenExams == that.minMinutesBetweenExams
                && maxExamsPerDay == that.maxExamsPerDay
                && Objects.equals(allowedDays, that.allowedDays)
                && Objects.equals(allowedTimeRanges, that.allowedTimeRanges)
                && Objects.equals(roomSpecificRules, that.roomSpecificRules)
                && Objects.equals(examWeekStartDate, that.examWeekStartDate)
                && Objects.equals(examWeekEndDate, that.examWeekEndDate)
                && roomTurnoverMinutes == that.roomTurnoverMinutes
                && slotStepMinutes == that.slotStepMinutes
                && baseExamDurationMinutes == that.baseExamDurationMinutes
                && creditDurationCoefficientMinutes == that.creditDurationCoefficientMinutes
                && durationRoundingMinutes == that.durationRoundingMinutes
                && minExamDurationMinutes == that.minExamDurationMinutes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minMinutesBetweenExams, maxExamsPerDay, allowedDays, allowedTimeRanges, roomSpecificRules, examWeekStartDate, examWeekEndDate, roomTurnoverMinutes, slotStepMinutes, baseExamDurationMinutes, creditDurationCoefficientMinutes, durationRoundingMinutes, minExamDurationMinutes);
    }

    public static class TimeRange {
        private LocalTime start;
        private LocalTime end;

        public TimeRange() {
        }

        public TimeRange(LocalTime start, LocalTime end) {
            setStart(start);
            setEnd(end);
            validate();
        }

        public LocalTime getStart() {
            return start;
        }

        public void setStart(LocalTime start) {
            this.start = start;
        }

        public LocalTime getEnd() {
            return end;
        }

        public void setEnd(LocalTime end) {
            this.end = end;
        }

        public boolean contains(LocalTime t) {
            if (t == null || start == null || end == null) return false;
            return !t.isBefore(start) && t.isBefore(end);
        }

        private void validate() {
            if (start == null || end == null) return;
            if (!start.isBefore(end)) {
                throw new IllegalArgumentException("TimeRange start must be before end");
            }
        }

        @Override
        public String toString() {
            return "TimeRange{" +
                    "start=" + start +
                    ", end=" + end +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TimeRange timeRange)) return false;
            return Objects.equals(start, timeRange.start) && Objects.equals(end, timeRange.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }
}