package dev.course.persistence.mapper;

import dev.course.persistence.entity.Emp;
import dev.course.persistence.support.EmbeddedDatabase;
import dev.course.persistence.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class EmpBasicQueryTest {
    private static SqlSessionFactory factory;
    private static List<String> statements;
    private final int number;
    private final String expectedNames;

    public EmpBasicQueryTest(int number, String expectedNames) {
        this.number = number;
        this.expectedNames = expectedNames;
    }

    @Parameterized.Parameters(name = "基础查询 {0}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
            {1, "林澈,唐可,许舟,周禾,陈安,沈知,赵宁,吴桐"},
            {2, "林澈,唐可,许舟,周禾,陈安,沈知,赵宁,吴桐"},
            {3, "林澈,陈安,沈知,赵宁"},
            {4, "林澈,唐可,许舟,周禾,沈知,赵宁"},
            {5, "林澈,许舟,赵宁,吴桐"},
            {6, "唐可,周禾,沈知,赵宁"},
            {7, "唐可,周禾,赵宁"},
            {8, "林澈,唐可,许舟,陈安,赵宁"},
            {9, "林澈,许舟,吴桐"},
            {10, "沈知,赵宁,陈安,林澈,许舟,唐可,吴桐,周禾"}
        });
    }

    @BeforeClass
    public static void setup() throws Exception {
        EmbeddedDatabase.initialize("basic_queries");
        factory = MyBatisUtil.buildMyBatis(EmbeddedDatabase.properties("basic_queries"));
        // 直接执行交付的 SQL 脚本，避免只验证 Mapper、遗漏脚本本身。
        String script = Files.readString(Paths.get("sql/basic-queries.sql"), StandardCharsets.UTF_8);
        statements = Arrays.stream(script.replaceAll("(?m)^--.*$", "").split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isEmpty() && !sql.toUpperCase(java.util.Locale.ROOT).startsWith("USE "))
                .collect(Collectors.toList());
        Assert.assertEquals(10, statements.size());
    }

    @Test
    public void scriptAndMapperReturnExpectedEmployees() throws Exception {
        try (SqlSession session = factory.openSession();
             Statement statement = session.getConnection().createStatement()) {
            List<String> scriptNames = new ArrayList<>();
            try (ResultSet result = statement.executeQuery(statements.get(number - 1))) {
                Assert.assertEquals(number == 2 ? 3 : 8, result.getMetaData().getColumnCount());
                while (result.next()) {
                    scriptNames.add(result.getString("ename"));
                }
            }
            assertNames(scriptNames);
            EmpMapper mapper = session.getMapper(EmpMapper.class);
            List<Emp> employees;
            switch (number) {
                case 1: employees = mapper.selectAllEmps(); break;
                case 2: employees = mapper.selectNameSalDeptno(); break;
                case 3: employees = mapper.selectSalAbove10000(); break;
                case 4: employees = mapper.selectDeptno20Or30(); break;
                case 5: employees = mapper.selectHiredAfter2001(); break;
                case 6: employees = mapper.selectWithComm(); break;
                case 7: employees = mapper.selectSalespeople(); break;
                case 8: employees = mapper.selectSalBetween8000And20000(); break;
                case 9: employees = mapper.selectWithoutCommAndSalBelow15000(); break;
                case 10: employees = mapper.selectOrderBySalDesc(); break;
                default: throw new AssertionError("未知查询编号");
            }
            assertNames(employees.stream().map(Emp::getEname).collect(Collectors.toList()));
            for (Emp emp : employees) {
                Assert.assertNotNull(emp.getSal());
                Assert.assertNotNull(emp.getDeptno());
                if (number == 2) {
                    Assert.assertNull(emp.getEmpno());
                    Assert.assertNull(emp.getJob());
                    Assert.assertNull(emp.getHiredate());
                } else {
                    Assert.assertNotNull(emp.getEmpno());
                    Assert.assertNotNull(emp.getJob());
                    Assert.assertNotNull(emp.getHiredate());
                }
            }
        }
    }

    private void assertNames(List<String> actual) {
        List<String> expected = new ArrayList<>(Arrays.asList(expectedNames.split(",")));
        // 只有第 10 条显式要求顺序；其余查询不假设数据库默认行顺序。
        if (number != 10) {
            Collections.sort(expected);
            Collections.sort(actual);
        }
        Assert.assertEquals(expected, actual);
    }
}
