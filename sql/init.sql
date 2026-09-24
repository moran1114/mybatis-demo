CREATE DATABASE IF NOT EXISTS persistence_lab
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE persistence_lab;

DROP TABLE IF EXISTS emp;

CREATE TABLE emp (
    empno BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '员工编号',
    ename VARCHAR(50) NOT NULL COMMENT '员工姓名',
    job VARCHAR(60) NOT NULL COMMENT '职位',
    mgr BIGINT NULL COMMENT '直属上级员工编号',
    hiredate DATE NOT NULL COMMENT '入职日期',
    sal DECIMAL(12, 2) NOT NULL COMMENT '工资',
    comm DECIMAL(12, 2) NULL COMMENT '奖金，NULL 表示无奖金记录，0 表示奖金为零',
    deptno INT NOT NULL COMMENT '部门编号',
    CONSTRAINT ck_emp_sal CHECK (sal >= 0),
    CONSTRAINT ck_emp_comm CHECK (comm IS NULL OR comm >= 0),
    INDEX idx_emp_ename (ename),
    INDEX idx_emp_deptno (deptno)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '员工表';

INSERT INTO emp (empno, ename, job, mgr, hiredate, sal, comm, deptno)
VALUES
    (1, '林澈', '工程师', 6, '2023-10-09', 14500.00, NULL, 20),
    (2, '唐可', '销售员', 6, '2001-01-01', 8000.00, 1500.00, 30),
    (3, '许舟', '分析师', 6, '2002-08-08', 10000.00, NULL, 20),
    (4, '周禾', '销售员', 6, '1999-05-20', 7000.00, 0.00, 30),
    (5, '陈安', '会计', 6, '2000-03-11', 15000.00, NULL, 10),
    (6, '沈知', '经理', NULL, '2000-11-23', 22000.00, 3000.00, 20),
    (7, '赵宁', '销售员', 6, '2024-06-01', 20000.00, 2000.00, 30),
    (8, '吴桐', '文员', 6, '2024-07-15', 7500.00, NULL, 10);