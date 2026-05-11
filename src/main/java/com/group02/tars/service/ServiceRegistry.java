package com.group02.tars.service;

import com.group02.tars.service.impl.ApplicationServiceImpl;
import com.group02.tars.service.impl.AdminServiceImpl;
import com.group02.tars.service.impl.JobServiceImpl;
import com.group02.tars.service.impl.MoServiceImpl;
import com.group02.tars.service.impl.UserServiceImpl;
import com.group02.tars.storage.FileStorage;
import com.group02.tars.storage.JsonFileStorage;
import jakarta.servlet.ServletContext;

import java.io.IOException;

public class ServiceRegistry {

    private static final String REGISTRY_KEY = ServiceRegistry.class.getName() + ".INSTANCE";

    private final FileStorage storage;
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final MoService moService;
    private final AdminService adminService;

    private ServiceRegistry(ServletContext context) throws IOException {
        this.storage = new JsonFileStorage(context);
        this.userService = new UserServiceImpl(storage);
        this.jobService = new JobServiceImpl(storage);
        this.applicationService = new ApplicationServiceImpl(storage);
        this.moService = new MoServiceImpl(storage);
        this.adminService = new AdminServiceImpl(storage);
    }

    public static ServiceRegistry from(ServletContext context) throws IOException {
        synchronized (context) {
            Object found = context.getAttribute(REGISTRY_KEY);
            if (found instanceof ServiceRegistry registry) {
                return registry;
            }
            ServiceRegistry created = new ServiceRegistry(context);
            context.setAttribute(REGISTRY_KEY, created);
            return created;
        }
    }

    public UserService userService() {
        return userService;
    }

    public JobService jobService() {
        return jobService;
    }

    public ApplicationService applicationService() {
        return applicationService;
    }

    public MoService moService() {
        return moService;
    }

    public AdminService adminService() {
        return adminService;
    }

    public FileStorage storage() {
        return storage;
    }
}
