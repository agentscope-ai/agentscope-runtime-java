package io.agentscope.runtime.sandbox.manager.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterSandbox {
    String imageName();

    String sandboxType() default "base";

    String customType() default "";

    String securityLevel() default "medium";

    int timeout() default 300;

    String description() default "";

    String[] environment() default {};

    String[] resourceLimits() default {};

    String[] runtimeConfig() default {};
}

