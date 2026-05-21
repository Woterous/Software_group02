package com.group02.tars.service;

import com.group02.tars.entity.User;

import java.io.IOException;

/**
 * 用户服务接口 —— 定义用户相关操作的方法签名，由 UserServiceImpl 实现。
 * 上层 Servlet 只依赖这个接口，不直接依赖实现类。
 */
public interface UserService {
    User register(String name, String email, String password, String role, String skillsCsv, String cvPath) throws IOException, ServiceException;

    User login(String email, String password, String role) throws IOException, ServiceException;

    User findById(String userId) throws IOException, ServiceException;

    User updateProfile(String userId, String name, String email, String skillsCsv, String major, String contact) throws IOException, ServiceException;

    String updateCvPath(String userId, String cvPath) throws IOException, ServiceException;
}
