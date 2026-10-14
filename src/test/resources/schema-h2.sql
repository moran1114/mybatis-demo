DROP TABLE IF EXISTS emp_project;
DROP TABLE IF EXISTS emp_lifecycle;
DROP TABLE IF EXISTS emp_detail;
DROP TABLE IF EXISTS project;
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

CREATE TABLE emp_lifecycle (
    empno BIGINT AUTO_INCREMENT PRIMARY KEY,
    ename VARCHAR(50) NOT NULL,
    sal DECIMAL(12, 2) NOT NULL CHECK (sal >= 0),
    deptno INT NOT NULL,
    create_time TIMESTAMP(6) NOT NULL,
    update_time TIMESTAMP(6) NOT NULL,
    deleted INT NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    version INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_emp_lifecycle_dept ON emp_lifecycle(deptno, deleted);

CREATE TABLE project (
    project_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    start_date DATE NOT NULL,
    budget DECIMAL(14, 2) NOT NULL CHECK (budget >= 0)
);
CREATE INDEX idx_project_name ON project(name);

CREATE TABLE emp_detail (
    empno BIGINT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    address VARCHAR(200) NULL,
    emergency_contact VARCHAR(50) NULL,
    CONSTRAINT fk_emp_detail_emp FOREIGN KEY (empno) REFERENCES emp (empno)
);
CREATE INDEX idx_emp_detail_email ON emp_detail(email);

CREATE TABLE emp_project (
    empno BIGINT NOT NULL,
    project_id INT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (empno, project_id),
    CONSTRAINT fk_emp_project_emp FOREIGN KEY (empno) REFERENCES emp (empno),
    CONSTRAINT fk_emp_project_project FOREIGN KEY (project_id) REFERENCES project (project_id)
);
CREATE INDEX idx_emp_project_project ON emp_project(project_id);
