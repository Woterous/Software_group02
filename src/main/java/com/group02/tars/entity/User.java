package com.group02.tars.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    public String userId;
    public String name;
    public String email;
    public String password;
    public String role;
    public List<String> skills = new ArrayList<>();
    public String major;
    public String contact;
    public String cvPath;

    public User safeCopy() {
        User copy = new User();
        copy.userId = userId;
        copy.name = name;
        copy.email = email;
        copy.role = role;
        copy.skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
        copy.major = major;
        copy.contact = contact;
        copy.cvPath = cvPath;
        return copy;
    }
}
