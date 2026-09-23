package com.smarttestai.repository;

import com.smarttestai.entity.TestCase;
import com.smarttestai.entity.TestCaseStatus;
import com.smarttestai.entity.TestCaseType;
import com.smarttestai.entity.TestPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findAllByUserStoryId(Long userStoryId);

    Optional<TestCase> findByIdAndUserStoryId(Long id, Long userStoryId);

    boolean existsByIdAndUserStoryId(Long id, Long userStoryId);

    List<TestCase> findAllByUserStoryIdAndType(Long userStoryId, TestCaseType type);

    List<TestCase> findAllByUserStoryIdAndPriority(Long userStoryId, TestPriority priority);

    List<TestCase> findAllByUserStoryIdAndStatus(Long userStoryId, TestCaseStatus status);

    void deleteAllByUserStoryId(Long userStoryId);
}
