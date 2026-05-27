package com.taskmanager.taskmanager.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.taskmanager.taskmanager.exception.UnauthorizedException;
import com.taskmanager.taskmanager.user.User;

@Component
public class AuthUtils {
    public User getCurrentUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication==null || !authentication.isAuthenticated()){
            throw new UnauthorizedException("User not authenticated");
        }
        Object prinicipal = authentication.getPrincipal();
        if(prinicipal instanceof User user){
            return user;
        }
        throw  new UnauthorizedException("Unable to resolve current user");
    }
}
