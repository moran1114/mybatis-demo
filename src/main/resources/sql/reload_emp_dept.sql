CREATE DATABASE IF NOT EXISTS persistence_lab
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE persistence_lab;

-- Drop tables referencing emp/dept first (added for relation tables).
DROP TABLE IF EXISTS emp_project;
DROP TABLE IF EXISTS emp_detail;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS emp;
DROP TABLE IF EXISTS dept;

CREATE TABLE dept (
    deptno INT PRIMARY KEY COMMENT 'department number',
    dname VARCHAR(50) NOT NULL COMMENT 'department name',
    loc VARCHAR(80) NOT NULL COMMENT 'location'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'department';

CREATE TABLE emp (
    empno BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'employee number',
    ename VARCHAR(50) NOT NULL COMMENT 'employee name',
    job VARCHAR(60) NOT NULL COMMENT 'job',
    mgr BIGINT NULL COMMENT 'manager employee number',
    hiredate DATE NOT NULL COMMENT 'hire date',
    sal DECIMAL(12, 2) NOT NULL COMMENT 'salary',
    comm DECIMAL(12, 2) NULL COMMENT 'commission',
    deptno INT NOT NULL COMMENT 'department number',
    CONSTRAINT ck_emp_sal CHECK (sal >= 0),
    CONSTRAINT ck_emp_comm CHECK (comm IS NULL OR comm >= 0),
    CONSTRAINT fk_emp_dept FOREIGN KEY (deptno) REFERENCES dept (deptno),
    INDEX idx_emp_ename (ename),
    INDEX idx_emp_deptno (deptno)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'employee';

INSERT INTO dept (deptno, dname, loc)
VALUES
    (10, 'Accounting', 'Beijing'),
    (20, 'Technology', 'Shanghai'),
    (30, 'Sales', 'Shenzhen'),
    (40, 'QA', 'Hangzhou'),
    (50, 'Support', 'Chengdu');

INSERT INTO emp (empno, ename, job, mgr, hiredate, sal, comm, deptno)
VALUES
    (1, 'Lin Han', 'Engineer', 6, '2023-10-09', 14500.00, NULL, 20),
    (2, 'Tang Ke', 'Salesperson', 6, '2001-01-01', 8000.00, 1500.00, 30),
    (3, 'Xu Shu', 'Analyst', 6, '2002-08-08', 10000.00, NULL, 20),
    (4, 'Zhou Qi', 'Salesperson', 6, '1999-05-20', 7000.00, 0.00, 30),
    (5, 'Chen An', 'Accountant', 6, '2000-03-11', 15000.00, NULL, 10),
    (6, 'Shen Zhi', 'Manager', NULL, '2000-11-23', 22000.00, 3000.00, 20),
    (7, 'Zhao Ning', 'Salesperson', 6, '2024-06-01', 20000.00, 2000.00, 30),
    (8, 'Wu Tong', 'Clerk', 6, '2024-07-15', 7500.00, NULL, 10);
