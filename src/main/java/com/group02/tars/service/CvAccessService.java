package com.group02.tars.service;

import java.io.IOException;

/**
 * CV 访问控制接口 —— 定义CV文件访问权限判断的方法签名。
 */
public interface CvAccessService {
    AccessibleCv resolveAccessibleCv(String requesterUserId, String requesterRole, String fileName)
        throws IOException, ServiceException;

    record AccessibleCv(String fileName, String ownerUserId, String ownerName, String cvPath) {
    }
}
