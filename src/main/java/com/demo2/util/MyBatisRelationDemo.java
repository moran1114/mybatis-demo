package com.demo2.util;

import com.demo2.entity.Dept;
import com.demo2.entity.Emp;
import com.demo2.entity.Project;
import com.demo2.mapper.DeptMapper;
import com.demo2.mapper.EmpMapper;
import com.demo2.mapper.ProjectMapper;
import java.util.Scanner;
import org.apache.ibatis.session.SqlSession;

/**
 * 交互终端:演示 MyBatis 的一对多、一对一、多对多关联查询。
 * 只读演示,所有查询不修改数据,结尾统一 rollback。
 */
public final class MyBatisRelationDemo {
    private MyBatisRelationDemo() {
    }

    public static void main(String[] args) {
        try (SqlSession session = MyBatisUtil.openMyBatisSession()) {
            runInteractive(session);
            session.rollback();
        }
    }

    private static void runInteractive(SqlSession session) {
        EmpMapper empMapper = session.getMapper(EmpMapper.class);
        DeptMapper deptMapper = session.getMapper(DeptMapper.class);
        ProjectMapper projectMapper = session.getMapper(ProjectMapper.class);

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMenu();
                switch (scanner.nextLine().trim()) {
                    case "1":
                        runDeptWithEmps(scanner, deptMapper);
                        break;
                    case "2":
                        runEmpWithDetail(scanner, empMapper);
                        break;
                    case "3":
                        runEmpWithProjects(scanner, empMapper);
                        break;
                    case "4":
                        runProjectWithMembers(scanner, projectMapper);
                        break;
                    case "0":
                        running = false;
                        break;
                    default:
                        System.out.println("无效选项,请输入 0-4 之间的数字。");
                }
            }
        }
        System.out.println("演示结束,再见!");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("========== MyBatis 关联查询演示 ==========");
        System.out.println("1. 一对多:查询部门及其员工列表(输入部门编号)");
        System.out.println("2. 一对一:查询员工及其详情档案(输入员工编号)");
        System.out.println("3. 多对多:查询员工参与的项目(输入员工编号)");
        System.out.println("4. 多对多:查询项目成员(输入项目编号)");
        System.out.println("0. 退出");
        System.out.print("请选择(0-4):");
    }

    private static void runDeptWithEmps(Scanner scanner, DeptMapper mapper) {
        int deptno = (int) readPositiveLong(scanner, "请输入部门编号: ");
        Dept dept = mapper.selectDeptWithEmps(deptno);
        if (dept == null) {
            System.out.println("未找到部门编号 " + deptno + " 的部门。");
            return;
        }
        System.out.println("部门: " + dept);
        if (dept.getEmps().isEmpty()) {
            System.out.println("  该部门暂无员工。");
            return;
        }
        System.out.println("  共有 " + dept.getEmps().size() + " 名员工:");
        for (Emp emp : dept.getEmps()) {
            System.out.println("    - " + emp);
        }
    }

    private static void runEmpWithDetail(Scanner scanner, EmpMapper mapper) {
        long empno = readPositiveLong(scanner, "请输入员工编号: ");
        Emp emp = mapper.selectEmpWithDetail(empno);
        if (emp == null) {
            System.out.println("未找到员工编号 " + empno + " 的员工。");
            return;
        }
        System.out.println("员工: " + emp);
        if (emp.getDetail() == null) {
            System.out.println("  该员工暂无详情档案。");
            return;
        }
        System.out.println("  详情: " + emp.getDetail());
    }

    private static void runEmpWithProjects(Scanner scanner, EmpMapper mapper) {
        long empno = readPositiveLong(scanner, "请输入员工编号: ");
        Emp emp = mapper.selectEmpWithProjects(empno);
        if (emp == null) {
            System.out.println("未找到员工编号 " + empno + " 的员工。");
            return;
        }
        System.out.println("员工: " + emp);
        if (emp.getProjects().isEmpty()) {
            System.out.println("  该员工未参与任何项目。");
            return;
        }
        System.out.println("  共参与 " + emp.getProjects().size() + " 个项目:");
        for (Project project : emp.getProjects()) {
            System.out.println("    - " + project);
        }
    }

    private static void runProjectWithMembers(Scanner scanner, ProjectMapper mapper) {
        int projectId = (int) readPositiveLong(scanner, "请输入项目编号: ");
        Project project = mapper.selectProjectWithMembers(projectId);
        if (project == null) {
            System.out.println("未找到项目编号 " + projectId + " 的项目。");
            return;
        }
        System.out.println("项目: " + project);
        if (project.getMembers().isEmpty()) {
            System.out.println("  该项目暂无成员。");
            return;
        }
        System.out.println("  共有 " + project.getMembers().size() + " 名成员:");
        for (Emp emp : project.getMembers()) {
            System.out.println("    - " + emp);
        }
    }

    private static long readPositiveLong(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                System.out.println("输入不能为空,请重新输入。");
                continue;
            }
            try {
                long value = Long.parseLong(line);
                if (value <= 0) {
                    System.out.println("编号必须是正整数,请重新输入。");
                    continue;
                }
                return value;
            } catch (NumberFormatException ignored) {
                System.out.println("「" + line + "」不是合法数字,请重新输入。");
            }
        }
    }
}
