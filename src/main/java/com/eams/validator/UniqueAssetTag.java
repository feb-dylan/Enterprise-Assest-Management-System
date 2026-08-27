package com.eams.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueAssetTagValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueAssetTag {
    String message() default "Asset tag already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}