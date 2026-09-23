package com.smarttestai.service;

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
import com.smarttestai.entity.Role;
import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.TestCase;
import com.smarttestai.entity.TestCaseStatus;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import com.smarttestai.entity.TestStep;
import com.smarttestai.entity.User;
import com.smarttestai.entity.UserStory;
import com.smarttestai.exception.ResourceNotFoundException;
import com.smarttestai.exception.UserStoryNotReadyException;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.TestCaseRepository;
import com.smarttestai.repository.UserStoryRepository;
import com.smarttestai.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCaseServiceTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private UserStoryRepository userStoryRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TestCaseGeneratorService testCaseGeneratorService;

    @InjectMocks
    private TestCaseService testCaseService;

    private User testUser;
    private CustomUserDetails userDetails;
    private Project testProject;
    private UserStory readyStory;
    private UserStory draftStory;
    private TestCase testCase;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John QA")
                .email("john@example.com")
                .password("encoded_pass")
                .role(Role.ROLE_USER)
                .build();

        userDetails = CustomUserDetails.fromUser(testUser);

        testProject = Project.builder()
                .id(10L)
                .name("E-Commerce QA")
                .description("Platform testing")
                .owner(testUser)
                .build();

        readyStory = UserStory.builder()
                .id(100L)
                .project(testProject)
                .title("Cart Checkout")
                .description("As a user, I want to checkout cart items.")
                .acceptanceCriteria("Given cart has items, when checkout clicked, order created.")
                .priority(StoryPriority.HIGH)
                .status(StoryStatus.READY)
                .build();

        draftStory = UserStory.builder()
                .id(101L)
                .project(testProject)
                .title("Wishlist")
                .description("Wishlist feature in progress")
                .acceptanceCriteria("WIP")
                .priority(StoryPriority.LOW)
                .status(StoryStatus.DRAFT)
                .build();

        testCase = TestCase.builder()
                .id(500L)
                .userStory(readyStory)
                .title("Successful Checkout")
                .description("Verifies standard checkout")
                .type(TestCaseType.POSITIVE)
                .priority(TestPriority.CRITICAL)
                .status(TestCaseStatus.GENERATED)
                .preconditions("Cart has items")
                .steps(Arrays.asList(new TestStep(1, "Click checkout", "Checkout page displayed")))
                .expectedResult("Order confirmation displayed")
                .promptVersion("v1.0")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("generateTestCases should succeed for READY story and persist generated test cases")
    void generateTestCases_readyStory_success() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(testProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(readyStory));

        AiGeneratedTestCaseDto aiDto = new AiGeneratedTestCaseDto(
                "Successful Checkout",
                "Verifies checkout",
                "POSITIVE",
                "CRITICAL",
                "Cart loaded",
                Arrays.asList(new AiTestStepDto(1, "Click checkout", "Confirmation shown")),
                "Order created"
        );
        when(testCaseGeneratorService.generateTestCases(readyStory)).thenReturn(Arrays.asList(aiDto));
        when(testCaseRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        GenerateTestCasesResponse response = testCaseService.generateTestCases(10L, 100L, userDetails);

        assertNotNull(response);
        assertEquals(100L, response.getUserStoryId());
        assertEquals(1, response.getGeneratedCount());
        assertEquals("Successful Checkout", response.getTestCases().get(0).getTitle());
        verify(testCaseRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("generateTestCases should throw UserStoryNotReadyException when story status is DRAFT")
    void generateTestCases_draftStory_throwsUserStoryNotReadyException() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(testProject));
        when(userStoryRepository.findByIdAndProjectId(101L, 10L)).thenReturn(Optional.of(draftStory));

        UserStoryNotReadyException ex = assertThrows(UserStoryNotReadyException.class,
                () -> testCaseService.generateTestCases(10L, 101L, userDetails));

        assertEquals("User Story id: 101 is in 'DRAFT' status. Only stories in 'READY' status can generate test cases.", ex.getMessage());
    }

    @Test
    @DisplayName("generateTestCases should throw ResourceNotFoundException when project not found")
    void generateTestCases_projectNotFound_throwsException() {
        when(projectRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> testCaseService.generateTestCases(99L, 100L, userDetails));
    }

    @Test
    @DisplayName("generateTestCases should deduplicate test cases with identical titles")
    void generateTestCases_deduplicatesIdenticalTitles() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(testProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(readyStory));

        AiGeneratedTestCaseDto duplicate1 = new AiGeneratedTestCaseDto(
                "Duplicate Title", "Desc 1", "POSITIVE", "HIGH", "Pre",
                Arrays.asList(new AiTestStepDto(1, "Action 1", "Result 1")), "Result 1");
        AiGeneratedTestCaseDto duplicate2 = new AiGeneratedTestCaseDto(
                "duplicate title", "Desc 2", "POSITIVE", "HIGH", "Pre",
                Arrays.asList(new AiTestStepDto(1, "Action 2", "Result 2")), "Result 2");

        when(testCaseGeneratorService.generateTestCases(readyStory)).thenReturn(Arrays.asList(duplicate1, duplicate2));
        when(testCaseRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        GenerateTestCasesResponse response = testCaseService.generateTestCases(10L, 100L, userDetails);

        assertEquals(1, response.getGeneratedCount());
    }

    @Test
    @DisplayName("createTestCase should create and persist manual test case")
    void createTestCase_success() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(testProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(readyStory));

        CreateTestCaseRequest request = new CreateTestCaseRequest(
                "Manual Test Scenario",
                "Testing manually",
                TestCaseType.POSITIVE,
                TestPriority.HIGH,
                "Precondition",
                Arrays.asList(new TestStepDto(1, "Step 1 action", "Step 1 result")),
                "Expected final outcome"
        );

        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(invocation -> {
            TestCase tc = invocation.getArgument(0);
            tc.setId(600L);
            return tc;
        });

        TestCaseResponse response = testCaseService.createTestCase(10L, 100L, request, userDetails);

        assertNotNull(response);
        assertEquals("Manual Test Scenario", response.getTitle());
        assertEquals(TestCaseStatus.REVIEWED, response.getStatus());
        assertEquals(1, response.getSteps().size());
    }

    @Test
    @DisplayName("getAllTestCases should filter by type when provided")
    void getAllTestCases_withTypeFilter() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(testProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(readyStory));
        when(testCaseRepository.findAllByUserStoryIdAndType(100L, TestCaseType.POSITIVE))
                .thenReturn(Arrays.asList(testCase));

        List<TestCaseResponse> result = testCaseService.getAllTestCases(10L, 100L, TestCaseType.POSITIVE, null, null, userDetails);

        assertEquals(1, result.size());
        assertEquals(TestCaseType.POSITIVE, result.get(0).getType());
    }

    @Test
    @DisplayName("getTestCaseById should return test case when it exists")
    void getTestCaseById_success() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(testProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(readyStory));
        when(testCaseRepository.findByIdAndUserStoryId(500L, 100L)).thenReturn(Optional.of(testCase));

        TestCaseResponse response = testCaseService.getTestCaseById(10L, 100L, 500L, userDetails);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals("Successful Checkout", response.getTitle());
    }

    @Test
    @DisplayName("updateTestCaseStatus should update status to APPROVED")
    void updateTestCaseStatus_success() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(testProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(readyStory));
        when(testCaseRepository.findByIdAndUserStoryId(500L, 100L)).thenReturn(Optional.of(testCase));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTestCaseStatusRequest request = new UpdateTestCaseStatusRequest(TestCaseStatus.APPROVED, "Verified by QA lead");
        TestCaseResponse response = testCaseService.updateTestCaseStatus(10L, 100L, 500L, request, userDetails);

        assertEquals(TestCaseStatus.APPROVED, response.getStatus());
    }

    @Test
    @DisplayName("deleteTestCase should delete test case from repository")
    void deleteTestCase_success() {
        when(projectRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(testProject));
        when(userStoryRepository.findByIdAndProjectId(100L, 10L)).thenReturn(Optional.of(readyStory));
        when(testCaseRepository.findByIdAndUserStoryId(500L, 100L)).thenReturn(Optional.of(testCase));

        testCaseService.deleteTestCase(10L, 100L, 500L, userDetails);

        verify(testCaseRepository).delete(testCase);
    }
}
