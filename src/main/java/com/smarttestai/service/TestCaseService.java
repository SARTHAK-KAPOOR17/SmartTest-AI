package com.smarttestai.service;

import com.smarttestai.ai.PromptBuilderService;
import com.smarttestai.ai.TestCaseGeneratorService;
import com.smarttestai.dto.ai.AiGeneratedTestCaseDto;
import com.smarttestai.dto.ai.AiTestStepDto;
import com.smarttestai.dto.request.CreateTestCaseRequest;
import com.smarttestai.dto.request.TestStepDto;
import com.smarttestai.dto.request.UpdateTestCaseRequest;
import com.smarttestai.dto.request.UpdateTestCaseStatusRequest;
import com.smarttestai.dto.response.GenerateTestCasesResponse;
import com.smarttestai.dto.response.TestCaseResponse;
import com.smarttestai.entity.Project;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.TestCase;
import com.smarttestai.entity.TestCaseStatus;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import com.smarttestai.entity.TestStep;
import com.smarttestai.entity.UserStory;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.exception.UserStoryNotReadyException;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.TestCaseRepository;
import com.smarttestai.repository.UserStoryRepository;
import com.smarttestai.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TestCaseService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseService.class);

    private final TestCaseRepository testCaseRepository;
    private final UserStoryRepository userStoryRepository;
    private final ProjectRepository projectRepository;
    private final TestCaseGeneratorService testCaseGeneratorService;

    public TestCaseService(TestCaseRepository testCaseRepository,
                           UserStoryRepository userStoryRepository,
                           ProjectRepository projectRepository,
                           TestCaseGeneratorService testCaseGeneratorService) {
        this.testCaseRepository = testCaseRepository;
        this.userStoryRepository = userStoryRepository;
        this.projectRepository = projectRepository;
        this.testCaseGeneratorService = testCaseGeneratorService;
    }

    public GenerateTestCasesResponse generateTestCases(Long projectId, Long storyId, CustomUserDetails currentUser) {
        log.info("Generating AI test cases for user story id: {} in project id: {} by user: {}",
                storyId, projectId, currentUser.getEmail());

        getProjectOrThrow(projectId, currentUser);
        UserStory story = getStoryOrThrow(storyId, projectId);

        if (story.getStatus() != StoryStatus.READY) {
            log.warn("Cannot generate test cases: story id: {} is in status: {}", storyId, story.getStatus());
            throw new UserStoryNotReadyException(
                    "User Story id: " + storyId + " is in '" + story.getStatus() + "' status. " +
                            "Only stories in 'READY' status can generate test cases."
            );
        }

        // Call AI generation service outside DB transaction to avoid connection exhaustion
        List<AiGeneratedTestCaseDto> generatedDtos = testCaseGeneratorService.generateTestCases(story);

        // Deduplicate and persist in an atomic transaction
        return persistGeneratedTestCases(story, generatedDtos);
    }

    @Transactional
    public GenerateTestCasesResponse persistGeneratedTestCases(UserStory story, List<AiGeneratedTestCaseDto> generatedDtos) {
        Set<String> seenTitles = new HashSet<>();
        List<TestCase> testCasesToSave = new ArrayList<>();

        for (AiGeneratedTestCaseDto dto : generatedDtos) {
            String titleKey = dto.getTitle() != null ? dto.getTitle().trim().toLowerCase() : "";
            if (titleKey.isEmpty() || seenTitles.contains(titleKey)) {
                continue;
            }
            seenTitles.add(titleKey);

            TestCaseType type;
            try {
                type = TestCaseType.valueOf(dto.getType().toUpperCase());
            } catch (Exception e) {
                type = TestCaseType.POSITIVE;
            }

            TestPriority priority;
            try {
                priority = TestPriority.valueOf(dto.getPriority().toUpperCase());
            } catch (Exception e) {
                priority = TestPriority.MEDIUM;
            }

            List<TestStep> steps = new ArrayList<>();
            if (dto.getSteps() != null) {
                for (int i = 0; i < dto.getSteps().size(); i++) {
                    AiTestStepDto stepDto = dto.getSteps().get(i);
                    int stepNum = stepDto.getStepNumber() != null ? stepDto.getStepNumber() : i + 1;
                    steps.add(new TestStep(stepNum, stepDto.getAction().trim(),
                            stepDto.getExpectedResult() != null ? stepDto.getExpectedResult().trim() : null));
                }
            }

            TestCase testCase = TestCase.builder()
                    .userStory(story)
                    .title(dto.getTitle().trim())
                    .description(dto.getDescription() != null ? dto.getDescription().trim() : "")
                    .type(type)
                    .priority(priority)
                    .status(TestCaseStatus.GENERATED)
                    .preconditions(dto.getPreconditions() != null ? dto.getPreconditions().trim() : null)
                    .steps(steps)
                    .expectedResult(dto.getExpectedResult() != null ? dto.getExpectedResult().trim() : "")
                    .promptVersion(PromptBuilderService.PROMPT_VERSION)
                    .build();

            testCasesToSave.add(testCase);
        }

        List<TestCase> saved = testCaseRepository.saveAll(testCasesToSave);
        log.info("Persisted {} generated test cases for story id: {}", saved.size(), story.getId());

        List<TestCaseResponse> responses = saved.stream()
                .map(TestCaseResponse::fromEntity)
                .collect(Collectors.toList());

        return new GenerateTestCasesResponse(story.getId(), PromptBuilderService.PROMPT_VERSION, responses.size(), responses);
    }

    @Transactional
    public TestCaseResponse createTestCase(Long projectId, Long storyId, CreateTestCaseRequest request, CustomUserDetails currentUser) {
        log.info("Creating manual test case '{}' under story id: {} by user: {}", request.getTitle(), storyId, currentUser.getEmail());
        getProjectOrThrow(projectId, currentUser);
        UserStory story = getStoryOrThrow(storyId, projectId);

        List<TestStep> steps = new ArrayList<>();
        if (request.getSteps() != null) {
            for (TestStepDto stepDto : request.getSteps()) {
                steps.add(new TestStep(stepDto.getStepNumber(), stepDto.getAction().trim(),
                        stepDto.getExpectedResult() != null ? stepDto.getExpectedResult().trim() : null));
            }
        }

        TestCase testCase = TestCase.builder()
                .userStory(story)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .type(request.getType())
                .priority(request.getPriority() != null ? request.getPriority() : TestPriority.MEDIUM)
                .status(TestCaseStatus.REVIEWED)
                .preconditions(request.getPreconditions() != null ? request.getPreconditions().trim() : null)
                .steps(steps)
                .expectedResult(request.getExpectedResult().trim())
                .promptVersion("manual")
                .build();

        TestCase saved = testCaseRepository.save(testCase);
        log.info("Successfully created test case id: {} under story id: {}", saved.getId(), storyId);

        return TestCaseResponse.fromEntity(saved);
    }

    public List<TestCaseResponse> getAllTestCases(Long projectId, Long storyId,
                                                  TestCaseType type, TestPriority priority, TestCaseStatus status,
                                                  CustomUserDetails currentUser) {
        log.info("Listing test cases for story id: {} in project id: {} by user: {}", storyId, projectId, currentUser.getEmail());
        getProjectOrThrow(projectId, currentUser);
        getStoryOrThrow(storyId, projectId);

        List<TestCase> testCases;
        if (type != null) {
            testCases = testCaseRepository.findAllByUserStoryIdAndType(storyId, type);
        } else if (priority != null) {
            testCases = testCaseRepository.findAllByUserStoryIdAndPriority(storyId, priority);
        } else if (status != null) {
            testCases = testCaseRepository.findAllByUserStoryIdAndStatus(storyId, status);
        } else {
            testCases = testCaseRepository.findAllByUserStoryId(storyId);
        }

        return testCases.stream()
                .map(TestCaseResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public TestCaseResponse getTestCaseById(Long projectId, Long storyId, Long testCaseId, CustomUserDetails currentUser) {
        log.info("Fetching test case id: {} under story id: {} by user: {}", testCaseId, storyId, currentUser.getEmail());
        getProjectOrThrow(projectId, currentUser);
        getStoryOrThrow(storyId, projectId);

        TestCase testCase = testCaseRepository.findByIdAndUserStoryId(testCaseId, storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Case", testCaseId));

        return TestCaseResponse.fromEntity(testCase);
    }

    @Transactional
    public TestCaseResponse updateTestCase(Long projectId, Long storyId, Long testCaseId,
                                           UpdateTestCaseRequest request, CustomUserDetails currentUser) {
        log.info("Updating test case id: {} under story id: {} by user: {}", testCaseId, storyId, currentUser.getEmail());
        getProjectOrThrow(projectId, currentUser);
        getStoryOrThrow(storyId, projectId);

        TestCase testCase = testCaseRepository.findByIdAndUserStoryId(testCaseId, storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Case", testCaseId));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            testCase.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            testCase.setDescription(request.getDescription().trim());
        }
        if (request.getType() != null) {
            testCase.setType(request.getType());
        }
        if (request.getPriority() != null) {
            testCase.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            testCase.setStatus(request.getStatus());
        }
        if (request.getPreconditions() != null) {
            testCase.setPreconditions(request.getPreconditions().trim());
        }
        if (request.getSteps() != null) {
            List<TestStep> updatedSteps = request.getSteps().stream()
                    .map(s -> new TestStep(s.getStepNumber(), s.getAction().trim(),
                            s.getExpectedResult() != null ? s.getExpectedResult().trim() : null))
                    .collect(Collectors.toList());
            testCase.setSteps(updatedSteps);
        }
        if (request.getExpectedResult() != null && !request.getExpectedResult().isBlank()) {
            testCase.setExpectedResult(request.getExpectedResult().trim());
        }

        TestCase updated = testCaseRepository.save(testCase);
        log.info("Successfully updated test case id: {}", testCaseId);

        return TestCaseResponse.fromEntity(updated);
    }

    @Transactional
    public TestCaseResponse updateTestCaseStatus(Long projectId, Long storyId, Long testCaseId,
                                                 UpdateTestCaseStatusRequest request, CustomUserDetails currentUser) {
        log.info("Updating status to {} for test case id: {} by user: {}", request.getStatus(), testCaseId, currentUser.getEmail());
        getProjectOrThrow(projectId, currentUser);
        getStoryOrThrow(storyId, projectId);

        TestCase testCase = testCaseRepository.findByIdAndUserStoryId(testCaseId, storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Case", testCaseId));

        testCase.setStatus(request.getStatus());
        TestCase updated = testCaseRepository.save(testCase);
        log.info("Successfully updated status to {} for test case id: {}", request.getStatus(), testCaseId);

        return TestCaseResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteTestCase(Long projectId, Long storyId, Long testCaseId, CustomUserDetails currentUser) {
        log.info("Deleting test case id: {} under story id: {} by user: {}", testCaseId, storyId, currentUser.getEmail());
        getProjectOrThrow(projectId, currentUser);
        getStoryOrThrow(storyId, projectId);

        TestCase testCase = testCaseRepository.findByIdAndUserStoryId(testCaseId, storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Test Case", testCaseId));

        testCaseRepository.delete(testCase);
        log.info("Successfully deleted test case id: {} from story id: {}", testCaseId, storyId);
    }

    private Project getProjectOrThrow(Long projectId, CustomUserDetails currentUser) {
        if (currentUser.isAdmin()) {
            return projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        } else {
            return projectRepository.findByIdAndOwnerId(projectId, currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        }
    }

    private UserStory getStoryOrThrow(Long storyId, Long projectId) {
        return userStoryRepository.findByIdAndProjectId(storyId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("User Story", storyId));
    }
}
