package com.smarttestai.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_story_id", nullable = false)
    private UserStory userStory;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestCaseType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestCaseStatus status;

    @Column(columnDefinition = "TEXT")
    private String preconditions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "test_case_steps", joinColumns = @JoinColumn(name = "test_case_id"))
    @OrderColumn(name = "step_order")
    private List<TestStep> steps = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String expectedResult;

    @Column(nullable = false, length = 20)
    private String promptVersion = "v1.0";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public TestCase() {
    }

    public TestCase(Long id, UserStory userStory, String title, String description,
                    TestCaseType type, TestPriority priority, TestCaseStatus status,
                    String preconditions, List<TestStep> steps, String expectedResult,
                    String promptVersion, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userStory = userStory;
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.preconditions = preconditions;
        if (steps != null) {
            this.steps = new ArrayList<>(steps);
        }
        this.expectedResult = expectedResult;
        this.promptVersion = promptVersion != null ? promptVersion : "v1.0";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserStory getUserStory() {
        return userStory;
    }

    public void setUserStory(UserStory userStory) {
        this.userStory = userStory;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TestCaseType getType() {
        return type;
    }

    public void setType(TestCaseType type) {
        this.type = type;
    }

    public TestPriority getPriority() {
        return priority;
    }

    public void setPriority(TestPriority priority) {
        this.priority = priority;
    }

    public TestCaseStatus getStatus() {
        return status;
    }

    public void setStatus(TestCaseStatus status) {
        this.status = status;
    }

    public String getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(String preconditions) {
        this.preconditions = preconditions;
    }

    public List<TestStep> getSteps() {
        return steps;
    }

    public void setSteps(List<TestStep> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestCase testCase = (TestCase) o;
        return Objects.equals(id, testCase.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private UserStory userStory;
        private String title;
        private String description;
        private TestCaseType type = TestCaseType.POSITIVE;
        private TestPriority priority = TestPriority.MEDIUM;
        private TestCaseStatus status = TestCaseStatus.GENERATED;
        private String preconditions;
        private List<TestStep> steps = new ArrayList<>();
        private String expectedResult;
        private String promptVersion = "v1.0";
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userStory(UserStory userStory) {
            this.userStory = userStory;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(TestCaseType type) {
            this.type = type;
            return this;
        }

        public Builder priority(TestPriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder status(TestCaseStatus status) {
            this.status = status;
            return this;
        }

        public Builder preconditions(String preconditions) {
            this.preconditions = preconditions;
            return this;
        }

        public Builder steps(List<TestStep> steps) {
            this.steps = steps != null ? steps : new ArrayList<>();
            return this;
        }

        public Builder expectedResult(String expectedResult) {
            this.expectedResult = expectedResult;
            return this;
        }

        public Builder promptVersion(String promptVersion) {
            this.promptVersion = promptVersion;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public TestCase build() {
            return new TestCase(id, userStory, title, description, type, priority, status,
                    preconditions, steps, expectedResult, promptVersion, createdAt, updatedAt);
        }
    }
}
