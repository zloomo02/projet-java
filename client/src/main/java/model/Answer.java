package model;

import java.time.LocalDateTime;

public class Answer {
    private int questionId;
    private String selectedOption;
    private boolean correct;
    private int pointsEarned;
    private LocalDateTime answeredAt;

    public Answer() {
    }

    public Answer(int questionId, String selectedOption, boolean correct, int pointsEarned, LocalDateTime answeredAt) {
        this.questionId = questionId;
        this.selectedOption = selectedOption;
        this.correct = correct;
        this.pointsEarned = pointsEarned;
        this.answeredAt = answeredAt;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
}
