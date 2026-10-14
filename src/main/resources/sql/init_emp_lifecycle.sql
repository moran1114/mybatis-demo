-- 第三节独立实验表。先执行 reload_emp_dept.sql 创建 persistence_lab。
-- 只创建不存在的表，不清空已有实验数据。
USE persistence_lab;

CREATE TABLE IF NOT EXISTS emp_lifecycle (
    empno BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'employee number',
    ename VARCHAR(50) NOT NULL COMMENT 'employee name',
    sal DECIMAL(12, 2) NOT NULL COMMENT 'salary',
    deptno INT NOT NULL COMMENT 'department number',
    create_time DATETIME(6) NOT NULL COMMENT 'filled on insert',
    update_time DATETIME(6) NOT NULL COMMENT 'filled on insert and update',
    deleted INT NOT NULL DEFAULT 0 COMMENT '0 active, 1 deleted',
    version INT NOT NULL DEFAULT 0 COMMENT 'optimistic lock version',
    CONSTRAINT ck_emp_lifecycle_sal CHECK (sal >= 0),
    CONSTRAINT ck_emp_lifecycle_deleted CHECK (deleted IN (0, 1)),
    INDEX idx_emp_lifecycle_dept (deptno, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'MyBatis-Plus lifecycle lesson';
