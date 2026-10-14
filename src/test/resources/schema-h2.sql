DROP TABLE IF EXISTS emp;
DROP TABLE IF EXISTS dept;

CREATE TABLE dept (
    deptno INT PRIMARY KEY,
    dname VARCHAR(50) NOT NULL,
    loc VARCHAR(80) NOT NULL
);

CREATE TABLE emp (
    empno BIGINT AUTO_INCREMENT PRIMARY KEY,
    ename VARCHAR(50) NOT NULL,
    job VARCHAR(60) NOT NULL,
    mgr BIGINT NULL,
    hiredate DATE NOT NULL,
    sal DECIMAL(12, 2) NOT NULL CHECK (sal >= 0),
    comm DECIMAL(12, 2) NULL CHECK (comm IS NULL OR comm >= 0),
    deptno INT NOT NULL,
    CONSTRAINT fk_emp_dept FOREIGN KEY (deptno) REFERENCES dept (deptno)
);

CREATE INDEX idx_emp_ename ON emp(ename);
CREATE INDEX idx_emp_deptno ON emp(deptno);
