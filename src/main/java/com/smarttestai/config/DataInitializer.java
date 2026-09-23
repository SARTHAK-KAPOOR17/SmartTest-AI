package com.smarttestai.config;

import com.smarttestai.entity.Project;
import com.smarttestai.entity.Role;
import com.smarttestai.entity.User;
import com.smarttestai.repository.ProjectRepository;
import com.smarttestai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEFAULT_ADMIN_EMAIL = "admin@smarttestai.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "AdminInitialPassword123!";

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ProjectRepository projectRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User adminUser = ensureAdminUserExists();
        migrateOrphanedProjects(adminUser);
    }

    private User ensureAdminUserExists() {
        return userRepository.findByEmail(DEFAULT_ADMIN_EMAIL)
                .orElseGet(() -> {
                    log.info("Bootstrapping default administrative account: {}", DEFAULT_ADMIN_EMAIL);
                    User admin = User.builder()
                            .name("SmartTest Administrator")
                            .email(DEFAULT_ADMIN_EMAIL)
                            .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                            .role(Role.ROLE_ADMIN)
                            .build();
                    return userRepository.save(admin);
                });
    }

    private void migrateOrphanedProjects(User adminUser) {
        List<Project> orphanedProjects = projectRepository.findAllByOwnerIsNull();
        if (!orphanedProjects.isEmpty()) {
            log.info("Migrating {} orphaned Phase 1 projects to default administrator owner", orphanedProjects.size());
            for (Project project : orphanedProjects) {
                project.setOwner(adminUser);
                projectRepository.save(project);
            }
            log.info("Successfully migrated all orphaned projects.");
        }
    }
}
