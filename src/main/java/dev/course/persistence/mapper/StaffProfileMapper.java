package dev.course.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.course.persistence.entity.StaffProfile;
import dev.course.persistence.query.StaffSearch;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface StaffProfileMapper extends BaseMapper<StaffProfile> {

    List<StaffProfile> selectAllProfiles();

    StaffProfile selectProfileById(@Param("id") Long id);

    int insertProfile(StaffProfile profile);

    int updateProfile(StaffProfile profile);

    int deleteProfileById(@Param("id") Long id);

    List<StaffProfile> selectBySearch(@Param("search") StaffSearch search);

    List<StaffProfile> selectBySearchWithTrim(@Param("search") StaffSearch search);

    int updatePartial(StaffProfile profile);

    int insertBatch(@Param("items") Collection<StaffProfile> items);

    int deleteBatch(@Param("ids") Collection<Long> ids);

    List<StaffProfile> selectPreferred(@Param("exactName") String exactName,
                                       @Param("unitName") String unitName);
}
