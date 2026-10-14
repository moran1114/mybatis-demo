package com.demo2.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/** 控制台项目手动注册；Clock 参数用于确定性地验证更新时间。 */
public final class AuditMetaObjectHandler implements MetaObjectHandler {
    private final Clock clock;

    public AuditMetaObjectHandler() {
        this(Clock.systemDefaultZone());
    }

    public AuditMetaObjectHandler(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now(clock);
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // strictUpdateFill 默认不覆盖非 null 值，读取后更新的实体会带着旧时间。
        // 审计规则要求每次更新都刷新 updateTime，因此这里显式覆盖。
        if (metaObject.hasSetter("updateTime")) {
            setFieldValByName("updateTime", LocalDateTime.now(clock), metaObject);
        }
    }
}
