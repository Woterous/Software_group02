package com.group02.tars.service;

import com.group02.tars.service.impl.ApplicationServiceImpl;
import com.group02.tars.service.impl.AdminServiceImpl;
import com.group02.tars.service.impl.AiAssistantServiceImpl;
import com.group02.tars.service.impl.CvAccessServiceImpl;
import com.group02.tars.service.impl.JobServiceImpl;
import com.group02.tars.service.impl.MoServiceImpl;
import com.group02.tars.service.impl.UserServiceImpl;
import com.group02.tars.storage.FileStorage;
import com.group02.tars.storage.JsonFileStorage;
import com.group02.tars.util.DataDirectoryResolver;
import jakarta.servlet.ServletContext;

import java.io.IOException;

/**
 * Servlet-context scoped holder for storage and service instances.
 *
 * <p>The registry creates one shared {@link FileStorage} instance and wires all
 * service implementations to it. API servlets obtain the registry through
 * {@link #from(ServletContext)} during initialization.</p>
 */
public class ServiceRegistry {

    private static final String REGISTRY_KEY = ServiceRegistry.class.getName() + ".INSTANCE";

    private final FileStorage storage;
    private final UserService userService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final MoService moService;
    private final AdminService adminService;
    private final CvAccessService cvAccessService;
    private final AiAssistantService aiAssistantService;

    /**
     * Creates the storage and service instances for one servlet context.
     *
     * @param context servlet context used for storage initialization
     * @throws IOException if storage cannot be initialized
     */
    private ServiceRegistry(ServletContext context) throws IOException {
        this.storage = new JsonFileStorage(context);
        this.userService = new UserServiceImpl(storage);
        this.jobService = new JobServiceImpl(storage);
        this.applicationService = new ApplicationServiceImpl(storage);
        this.moService = new MoServiceImpl(storage);
        this.adminService = new AdminServiceImpl(storage);
        this.cvAccessService = new CvAccessServiceImpl(storage);
        this.aiAssistantService = new AiAssistantServiceImpl(storage, DataDirectoryResolver.resolveUploadsDir(context));
    }

    /**
     * Returns the shared registry stored in the servlet context, creating it on first use.
     *
     * @param context servlet context used as registry storage
     * @return shared service registry
     * @throws IOException if registry creation fails
     */
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

    /**
     * Returns the user service.
     *
     * @return user service instance
     */
    public UserService userService() {
        return userService;
    }

    /**
     * Returns the job service.
     *
     * @return job service instance
     */
    public JobService jobService() {
        return jobService;
    }

    /**
     * Returns the application service.
     *
     * @return application service instance
     */
    public ApplicationService applicationService() {
        return applicationService;
    }

    /**
     * Returns the module-organizer service.
     *
     * @return module-organizer service instance
     */
    public MoService moService() {
        return moService;
    }

    /**
     * Returns the administrator service.
     *
     * @return administrator service instance
     */
    public AdminService adminService() {
        return adminService;
    }

    /**
     * Returns the CV access service.
     *
     * @return CV access service instance
     */
    public CvAccessService cvAccessService() {
        return cvAccessService;
    }

    /**
     * Returns the AI assistant service.
     *
     * @return AI assistant service instance
     */
    public AiAssistantService aiAssistantService() {
        return aiAssistantService;
    }

    /**
     * Returns the shared storage instance.
     *
     * @return storage instance used by the services
     */
    public FileStorage storage() {
        return storage;
    }
}
