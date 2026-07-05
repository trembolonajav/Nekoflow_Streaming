package com.nekoflow.backend.testsupport;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.nekoflow.backend.domain.entity.RoleEntity;
import com.nekoflow.backend.domain.entity.UserEntity;
import com.nekoflow.backend.domain.enums.RoleCode;

/**
 * Fabrica de entidades para testes. RoleEntity nao expoe setters (so getters),
 * entao usamos reflexao para preencher os campos em memoria — sem tocar em
 * codigo de producao.
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static RoleEntity role(RoleCode code) {
        RoleEntity role = new RoleEntity();
        setField(role, "id", UUID.randomUUID());
        setField(role, "code", code);
        setField(role, "description", code.name());
        return role;
    }

    public static UserEntity user(UUID id, String email, RoleCode... codes) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setName("Test User");
        user.setActive(true);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        user.setProvider("LOCAL");
        Set<RoleEntity> roles = new LinkedHashSet<>();
        for (RoleCode code : codes) {
            roles.add(role(code));
        }
        user.setRoles(roles);
        return user;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Nao foi possivel preencher o campo de teste: " + name, exception);
        }
    }
}
