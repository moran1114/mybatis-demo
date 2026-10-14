CREATE DATABASE IF NOT EXISTS persistence_lab
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE persistence_lab;

-- Child tables must be dropped before parent tables.
DROP TABLE IF EXISTS emp_project;
DROP TABLE IF EXISTS emp_detail;
DROP TABLE IF EXISTS project;

CREATE TABLE project (
    project_id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'project number',
    name VARCHAR(80) NOT NULL COMMENT 'project name',
    start_date DATE NOT NULL COMMENT 'project start date',
    budget DECIMAL(14, 2) NOT NULL COMMENT 'project budget',
    CONSTRAINT ck_project_budget CHECK (budget >= 0),
    INDEX idx_project_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'project';

-- Shared primary key empno: at most one detail row per employee (one-to-one).
CREATE TABLE emp_detail (
    empno BIGINT PRIMARY KEY COMMENT 'employee number, one-to-one with emp',
    phone VARCHAR(20) NOT NULL COMMENT 'phone number',
    email VARCHAR(100) NOT NULL COMMENT 'email address',
    address VARCHAR(200) NULL COMMENT 'home address',
    emergency_contact VARCHAR(50) NULL COMMENT 'emergency contact name',
    CONSTRAINT fk_emp_detail_emp FOREIGN KEY (empno) REFERENCES emp (empno),
    INDEX idx_emp_detail_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'employee detail, one-to-one with emp';

-- Composite primary key prevents duplicate assignments (many-to-many).
CREATE TABLE emp_project (
    empno BIGINT NOT NULL COMMENT 'employee number',
    project_id INT NOT NULL COMMENT 'project number',
    role VARCHAR(50) NOT NULL COMMENT 'role in the project',
    PRIMARY KEY (empno, project_id),
    CONSTRAINT fk_emp_project_emp FOREIGN KEY (empno) REFERENCES emp (empno),
    CONSTRAINT fk_emp_project_project FOREIGN KEY (project_id) REFERENCES project (project_id),
    INDEX idx_emp_project_project (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'employee-project assignment, many-to-many join table';

INSERT INTO project (project_id, name, start_date, budget)
VALUES
    (101, 'Payment Gateway Refactor', '2024-03-01', 800000.00),
    (102, 'Data Platform Construction', '2024-06-15', 1200000.00),
    (103, 'Mobile App', '2025-01-10', 600000.00);

-- empno 8 (Wu Tong) intentionally has no detail row to demo a NULL association.
INSERT INTO emp_detail (empno, phone, email, address, emergency_contact)
VALUES
    (1, '13800000001', 'lin.han@example.com', 'Shanghai, Pudong', 'Wang Lan'),
    (2, '13800000002', 'tang.ke@example.com', 'Shenzhen, Nanshan', 'Tang Li'),
    (3, '13800000003', 'xu.shu@example.com', 'Shanghai, Xuhui', 'Xu Feng'),
    (4, '13800000004', 'zhou.qi@example.com', 'Shenzhen, Futian', 'Zhou Jie'),
    (5, '13800000005', 'chen.an@example.com', 'Beijing, Chaoyang', 'Chen Jing'),
    (6, '13800000006', 'shen.zhi@example.com', 'Shanghai, Jingan', 'Shen Yue'),
    (7, '13800000007', 'zhao.ning@example.com', 'Shenzhen, Baoan', 'Zhao Min');

-- emp 4/5/8 intentionally have no project to demo an empty collection.
INSERT INTO emp_project (empno, project_id, role)
VALUES
    (1, 101, 'Backend Engineer'),
    (1, 102, 'Data Contributor'),
    (3, 102, 'Data Analyst'),
    (3, 103, 'Requirements Analyst'),
    (6, 101, 'Project Manager'),
    (6, 102, 'Tech Lead'),
    (2, 103, 'Sales Support'),
    (7, 103, 'UI Reviewer');
