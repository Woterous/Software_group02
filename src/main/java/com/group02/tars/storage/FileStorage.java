package com.group02.tars.storage;

import com.group02.tars.entity.Application;
import com.group02.tars.entity.Job;
import com.group02.tars.entity.User;

import java.io.IOException;
import java.util.List;

/**
 * 文件存储接口 —— 定义数据持久化的方法签名，由 JsonFileStorage 实现。
 * <p>
 * 隔离目的：如果将来要换成数据库存储，只需新建一个实现类替换 JsonFileStorage，Service 层零改动。
 */
public interface FileStorage {
    List<User> loadUsers() throws IOException;

    void saveUsers(List<User> users) throws IOException;

    List<Job> loadJobs() throws IOException;

    void saveJobs(List<Job> jobs) throws IOException;

    List<Application> loadApplications() throws IOException;

    void saveApplications(List<Application> applications) throws IOException;
}
