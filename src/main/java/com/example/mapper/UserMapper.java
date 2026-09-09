package com.example.mapper;

import com.example.entity.User;

import java.util.List;

/**
 * user 表的 Mapper 接口
 */
public interface UserMapper {

    /** 查询所有用户 */
    List<User> findAll();

    /** 根据主键 id 查询用户 */
    User findById(Integer id);

    /** 新增用户（返回影响行数，新增后 id 会回填到 user 对象） */
    int insert(User user);

    /** 修改用户 */
    int update(User user);

    /** 根据主键 id 删除用户 */
    int deleteById(Integer id);
}
