package com.smarttestai.repository;

import com.smarttestai.entity.StoryPriority;
import com.smarttestai.entity.StoryStatus;
import com.smarttestai.entity.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    List<UserStory> findAllByProjectId(Long projectId);

    Optional<UserStory> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByIdAndProjectId(Long id, Long projectId);

    List<UserStory> findAllByProjectIdAndStatus(Long projectId, StoryStatus status);

    List<UserStory> findAllByProjectIdAndPriority(Long projectId, StoryPriority priority);

    List<UserStory> findAllByProjectIdAndStatusAndPriority(Long projectId, StoryStatus status, StoryPriority priority);
}
