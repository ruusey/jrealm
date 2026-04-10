package com.openrealm.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.openrealm.account.dto.AccountProvision;

/**
 * Marks a server command as restricted. The invoking player's account must hold
 * at least one provision that satisfies any of the listed provisions.
 * ADMIN and SYS_ADMIN implicitly satisfy all lower provisions.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminRestrictedCommand {
    AccountProvision[] provisions() default { AccountProvision.OPENREALM_ADMIN };
}
