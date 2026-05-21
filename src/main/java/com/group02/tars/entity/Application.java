package com.group02.tars.entity;

/**
 * 申请实体 —— 对应 applications.json 文件中的一条记录。
 * 通过 userId 关联 User，通过 jobId 关联 Job。
 */
public class Application {
    public String applicationId;
    public String userId;
    public String jobId;
    public String status;
    public String reviewNote;
    public String updatedAt;
}
