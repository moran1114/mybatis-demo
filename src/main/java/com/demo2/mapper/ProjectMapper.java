package com.demo2.mapper;

import com.demo2.entity.Project;
import org.apache.ibatis.annotations.Param;

public interface ProjectMapper {
    Project selectProjectWithMembers(@Param("projectId") Integer projectId);
}
