package com.demo2.mapper;

import com.demo2.entity.Emp;
import com.demo2.support.EmbeddedDatabase;
import com.demo2.util.MyBatisUtil;
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

    @Parameterized.Parameters(name = "basic query {0}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
            {1, "Lin Han,Tang Ke,Xu Shu,Zhou Qi,Chen An,Shen Zhi,Zhao Ning,Wu Tong"},
            {2, "Lin Han,Tang Ke,Xu Shu,Zhou Qi,Chen An,Shen Zhi,Zhao Ning,Wu Tong"},
            {3, "Lin Han,Chen An,Shen Zhi,Zhao Ning"},
            {4, "Lin Han,Tang Ke,Xu Shu,Zhou Qi,Shen Zhi,Zhao Ning"},
            {5, "Lin Han,Xu Shu,Zhao Ning,Wu Tong"},
            {6, "Tang Ke,Zhou Qi,Shen Zhi,Zhao Ning"},
            {7, "Tang Ke,Zhou Qi,Zhao Ning"},
            {8, "Lin Han,Tang Ke,Xu Shu,Chen An,Zhao Ning"},
            {9, "Lin Han,Xu Shu,Wu Tong"},
            {10, "Shen Zhi,Zhao Ning,Chen An,Lin Han,Xu Shu,Tang Ke,Wu Tong,Zhou Qi"}
        });
    }

    @BeforeClass
    public static void setup() throws Exception {
        EmbeddedDatabase.initialize("basic_queries");
        factory = MyBatisUtil.buildMyBatis(EmbeddedDatabase.properties("basic_queries"));
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
                default: throw new AssertionError("Unknown query number");
            }
            assertNames(employees.stream().map(Emp::getEname).collect(Collectors.toList()));
        }
    }

    private void assertNames(List<String> actual) {
        List<String> expected = new ArrayList<>(Arrays.asList(expectedNames.split(",")));
        if (number != 10) {
            Collections.sort(expected);
            Collections.sort(actual);
        }
        Assert.assertEquals(expected, actual);
    }
}
