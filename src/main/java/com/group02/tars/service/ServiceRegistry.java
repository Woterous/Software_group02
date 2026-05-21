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
 * 服务注册中心 —— Servlet 和 Service 之间的连接点（"电话簿"）。
 * <p>
 * 信息流位置：启动时创建所有Service → Servlet通过它找到需要的Service
 * <p>
 * 为什么需要它：避免在每个Servlet里new Service导致混乱，
 * 全局只创建一份Service实例，所有Servlet共享。
 * 创建时机：BaseApiServlet.init() 调用 ServiceRegistry.from()（单例）。
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
     * 构造时创建所有Service和底层的FileStorage。
     * 每个Service都拿到同一个storage实例，保证数据一致性。
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
     * 获取全局唯一的ServiceRegistry实例（单例模式，用ServletContext做锁）。
     * 第一次调用时创建并缓存，之后直接返回已创建的实例。
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

    public CvAccessService cvAccessService() {
        return cvAccessService;
    }

    public AiAssistantService aiAssistantService() {
        return aiAssistantService;
    }

    public FileStorage storage() {
        return storage;
    }
}
