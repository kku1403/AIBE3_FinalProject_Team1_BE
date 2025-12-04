package com.back;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithTestAuthSecurityContextFactory.class)
public @interface WithMockMember {
    String role() default "USER";
}
