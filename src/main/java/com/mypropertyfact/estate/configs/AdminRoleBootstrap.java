package com.mypropertyfact.estate.configs;

import com.mypropertyfact.estate.entities.MasterRole;
import com.mypropertyfact.estate.repositories.MasterRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ensures the {@code ADMIN} dashboard role exists for assigning staff (separate from {@code SUPERADMIN}).
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class AdminRoleBootstrap implements ApplicationRunner {

    private final MasterRoleRepository masterRoleRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (masterRoleRepository.findByRoleNameIgnoreCase("ADMIN").isPresent()) {
            return;
        }
        MasterRole role = new MasterRole();
        role.setRoleName("ADMIN");
        role.setDescription("Dashboard staff; permissions are set per user by Super Admin");
        role.setIsActive(true);
        masterRoleRepository.save(role);
    }
}
