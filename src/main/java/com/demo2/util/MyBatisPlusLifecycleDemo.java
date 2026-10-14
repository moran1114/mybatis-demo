package com.demo2.util;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo2.entity.Emp;
import com.demo2.entity.EmpLifecycle;
import com.demo2.mapper.EmpLifecycleMapper;
import com.demo2.mapper.EmpMapper;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 第三节：先执行 init_emp_lifecycle.sql，再运行 main；事务最终回滚。 */
public final class MyBatisPlusLifecycleDemo {
    private MyBatisPlusLifecycleDemo() {
    }

    public static void main(String[] args) throws SQLException {
        try (SqlSession session = MyBatisUtil.openSession()) {
            run(session);
            session.rollback();
        }
        System.out.println("演示事务已回滚，自增序列可能推进。");
    }

    public static void run(SqlSession session) throws SQLException {
        System.out.println("8.3.1 插件链：乐观锁在前，分页在最后，见 MyBatisUtil.build");
        System.out.println("8.3.2 物理分页：第 2 页，每页 3 条，观察日志中的 COUNT 和 LIMIT");
        EmpMapper empMapper = session.getMapper(EmpMapper.class);
        Page<Emp> page = empMapper.selectPage(Page.of(2, 3),
                Wrappers.lambdaQuery(Emp.class).orderByAsc(Emp::getEmpno));
        System.out.println("current=" + page.getCurrent() + ", size=" + page.getSize()
                + ", total=" + page.getTotal() + ", pages=" + page.getPages());
        page.getRecords().forEach(System.out::println);

        EmpLifecycleMapper mapper = session.getMapper(EmpLifecycleMapper.class);
        System.out.println("8.3.3 自动填充：插入时没有设置 createTime / updateTime");
        EmpLifecycle employee = new EmpLifecycle();
        employee.setEname("Lifecycle Demo");
        employee.setDeptno(20);
        employee.setSal(new BigDecimal("12000"));
        employee.setVersion(0);
        mapper.insert(employee);
        System.out.println(mapper.selectById(employee.getEmpno()));

        System.out.println("8.3.4 逻辑删除：BaseMapper 查不到，JDBC 验证行仍存在");
        EmpLifecycle disposable = new EmpLifecycle();
        disposable.setEname("Logical Delete Demo");
        disposable.setDeptno(20);
        disposable.setSal(new BigDecimal("9000"));
        mapper.insert(disposable);
        System.out.println("删除影响行数：" + mapper.deleteById(disposable.getEmpno()));
        System.out.println("selectById：" + mapper.selectById(disposable.getEmpno()));
        // JDBC 仅用于观察 SQL 边界；业务查询应明确处理 deleted 条件。
        try (PreparedStatement statement = session.getConnection().prepareStatement(
                "SELECT deleted FROM emp_lifecycle WHERE empno = ?")) {
            statement.setLong(1, disposable.getEmpno());
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    System.out.println("数据库物理行仍存在，deleted=" + rows.getInt("deleted"));
                }
            }
        }

        System.out.println("8.3.5 乐观锁：两个独立旧快照，先成功一次，再演示版本冲突");
        EmpLifecycle first = mapper.selectById(employee.getEmpno());
        // 防止同一 SqlSession 的一级缓存返回同一个 Java 对象。
        session.clearCache();
        EmpLifecycle stale = mapper.selectById(employee.getEmpno());
        first.setSal(new BigDecimal("13000"));
        System.out.println("第一次更新影响行数：" + mapper.updateById(first)
                + "，回填版本：" + first.getVersion());
        stale.setSal(new BigDecimal("14000"));
        int changed = mapper.updateById(stale);
        System.out.println("旧快照更新影响行数：" + changed);
        if (changed == 0) {
            System.out.println("检测到冲突，重新读取后应由业务决定是否合并和重试："
                    + mapper.selectById(employee.getEmpno()));
        }
    }
}
