USE persistence_lab;

-- 1. All employee columns.
select * from emp;

-- 2. Name, salary, and department number.
select ename, sal, deptno from emp;

-- 3. Salary greater than 10000.
select * from emp where sal > 10000;

-- 4. Department 20 or 30.
select * from emp where deptno = 20 or deptno = 30;

-- 5. Hired after 2001-01-01.
select * from emp where hiredate > '2001-01-01';

-- 6. Has commission.
select * from emp where comm is not null;

-- 7. Salesperson.
select * from emp where job = 'Salesperson';

-- 8. Salary between 8000 and 20000.
select * from emp where sal between 8000 and 20000;

-- 9. No commission and salary below 15000.
select * from emp where comm is null and sal < 15000;

-- 10. Salary descending.
select * from emp order by sal desc;
