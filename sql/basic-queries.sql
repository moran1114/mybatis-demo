USE persistence_lab;

-- 1.查询所有员工的所有信息（empno, ename, job, mgr, hiredate, sal, comm, deptno）。
select * from emp;

-- 2.查询所有员工的姓名 (ename)、工资 (sal) 和部门编号 (deptno)。
select ename,sal,deptno from emp;

-- 3.查询工资 (sal) 大于 10000 的所有员工信息。
select * from emp where sal>10000;

-- 4.查询部门编号 (deptno) 为 20 或 30 的所有员工信息。
select * from emp where deptno = 20 or deptno = 30;

-- 5.查询入职日期 (hiredate) 在 2001-01-01 之后的所有员工信息。
select * from emp where hiredate > '2001-01-01';

-- 6.查询奖金 (comm) 不为 NULL 的所有员工信息。
select * from emp where comm is not null;

-- 7.查询职位 (job) 为 '销售员' 的所有员工信息。
select * from emp where job ='销售员';

-- 8.查询工资 (sal) 在 8000 到 20000 之间的所有员工信息。
select * from emp where sal between 8000 and 20000;

-- 9.查询没有奖金 (comm 为 NULL) 且工资 (sal) 小于 15000 的所有员工信息。
select * from emp where comm is null and sal<15000;

-- 10.查询所有员工信息，按工资 (sal) 降序排序。
select * from emp order by sal desc;