package com.system.hotel_room_booking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.system.hotel_room_booking.model.entity.Role;
import com.system.hotel_room_booking.model.entity.RoleName;
import com.system.hotel_room_booking.repository.RoleRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    
    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
    }
    
    private void initializeRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = Role.builder()
                    .name(roleName)
                    .description(getDescription(roleName))
                    .build();
                
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        }
    }
    
    private String getDescription(RoleName roleName) {
        return switch (roleName) {
            case ROLE_USER -> "Standard user role";
            case ROLE_ADMIN -> "Administrator role with full access";
            case ROLE_MODERATOR -> "Moderator role with limited admin access";
        };
    }
}

