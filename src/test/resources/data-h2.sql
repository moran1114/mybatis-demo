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

INSERT INTO project (project_id, name, start_date, budget)
VALUES
    (101, 'Payment Gateway Refactor', '2024-03-01', 800000.00),
    (102, 'Data Platform Construction', '2024-06-15', 1200000.00),
    (103, 'Mobile App', '2025-01-10', 600000.00);

INSERT INTO emp_detail (empno, phone, email, address, emergency_contact)
VALUES
    (1, '13800000001', 'lin.han@example.com', 'Shanghai, Pudong', 'Wang Lan'),
    (2, '13800000002', 'tang.ke@example.com', 'Shenzhen, Nanshan', 'Tang Li'),
    (3, '13800000003', 'xu.shu@example.com', 'Shanghai, Xuhui', 'Xu Feng'),
    (4, '13800000004', 'zhou.qi@example.com', 'Shenzhen, Futian', 'Zhou Jie'),
    (5, '13800000005', 'chen.an@example.com', 'Beijing, Chaoyang', 'Chen Jing'),
    (6, '13800000006', 'shen.zhi@example.com', 'Shanghai, Jingan', 'Shen Yue'),
    (7, '13800000007', 'zhao.ning@example.com', 'Shenzhen, Baoan', 'Zhao Min');

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
