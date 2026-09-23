package com.smarttestai.ai;

import com.smarttestai.dto.ai.AiGeneratedTestCaseDto;
import com.smarttestai.entity.UserStory;

import java.util.List;

public interface TestCaseGeneratorService {

    List<AiGeneratedTestCaseDto> generateTestCases(UserStory userStory);
}
