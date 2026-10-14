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
