package com.taskmanager.taskmanager.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoTestWordValidator implements ConstraintValidator<NoTestWord, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context){
        if(value==null) return true;
        return !value.toLowerCase().contains("test");
    }
}
