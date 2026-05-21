package com.group02.tars.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户实体 —— 对应 users.json 文件中的一条记录，在各个层之间传递的数据载体。
 * safeCopy() 返回不含 password 的拷贝，用于返回给前端时脱敏。
 */
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
