package com.group02.tars.service;

import com.group02.tars.entity.User;

import java.io.IOException;

public interface UserService {
    User register(String name, String email, String password, String role, String skillsCsv, String cvPath) throws IOException, ServiceException;

    User login(String email, String password, String role) throws IOException, ServiceException;

    User findById(String userId) throws IOException, ServiceException;

    User updateProfile(String userId, String name, String email, String skillsCsv, String major, String contact) throws IOException, ServiceException;

    String updateCvPath(String userId, String cvPath) throws IOException, ServiceException;
}
