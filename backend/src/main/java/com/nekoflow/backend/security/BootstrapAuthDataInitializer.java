package com.nekoflow.backend.security;

import java.util.Set;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nekoflow.backend.config.AppProperties;
import com.nekoflow.backend.domain.entity.RoleEntity;
import com.nekoflow.backend.domain.entity.UserEntity;
import com.nekoflow.backend.domain.enums.AuthProvider;
import com.nekoflow.backend.domain.enums.RoleCode;
import com.nekoflow.backend.domain.repository.RoleRepository;
import com.nekoflow.backend.domain.repository.UserRepository;

@Component
public class BootstrapAuthDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public BootstrapAuthDataInitializer(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    public void run(String... args) {
        if (appProperties.bootstrap() == null || !appProperties.bootstrap().enabled()) {
            return;
        }

        createUserIfMissing(
            appProperties.bootstrap().admin(),
            Set.of(requiredRole(RoleCode.ADMIN), requiredRole(RoleCode.EDITOR))
        );
        createUserIfMissing(
            appProperties.bootstrap().user(),
            Set.of(requiredRole(RoleCode.USER))
        );
    }

    private void createUserIfMissing(AppProperties.User config, Set<RoleEntity> roles) {
        if (userRepository.existsByEmailIgnoreCase(config.email())) {
            return;
        }

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setName(config.name());
        user.setEmail(config.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(config.password()));
        user.setProvider(AuthProvider.LOCAL.name());
        user.setActive(true);
        user.setRoles(roles);
        userRepository.save(user);
    }

    private RoleEntity requiredRole(RoleCode code) {
        return roleRepository.findByCode(code)
            .orElseThrow(() -> new IllegalStateException("Role not found: " + code));
    }
}
