package com.demo2.mapper;

import com.demo2.entity.Emp;
import com.demo2.support.EmbeddedDatabase;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Properties;

public class FaultDiagnosisTest {
    private static String mapperXml;

    @BeforeClass
    public static void setup() throws Exception {
        EmbeddedDatabase.initialize("fault_diagnosis");
        try (InputStream stream = Resources.getResourceAsStream("mapper/EmpMapper.xml")) {
            mapperXml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void wrongEntityPropertyFailsDuringParameterBinding() {
        String broken = mapperXml.replace("#{ename}", "#{empname}");
        try (SqlSession session = build(broken).openSession()) {
            RuntimeException error = Assert.assertThrows(RuntimeException.class,
                    () -> session.getMapper(EmpMapper.class).insertEmp(employee()));
            Throwable cause = rootCause(error);
            Assert.assertTrue(cause instanceof ReflectionException);
            Assert.assertTrue(cause.getMessage().contains("empname"));
            session.rollback();
        }
        assertCorrectInsert();
    }

    @Test
    public void wrongNamedParameterReportsBindingException() {
        String broken = mapperXml.replace("#{empno}", "#{missingEmpno}");
        try (SqlSession session = build(broken).openSession()) {
            RuntimeException error = Assert.assertThrows(RuntimeException.class,
                    () -> session.getMapper(EmpMapper.class).selectEmpById(1L));
            Throwable cause = rootCause(error);
            Assert.assertTrue(cause instanceof BindingException);
            Assert.assertTrue(cause.getMessage().contains("missingEmpno"));
        }
        try (SqlSession session = build(mapperXml).openSession()) {
            Assert.assertEquals("Lin Han", session.getMapper(EmpMapper.class).selectEmpById(1L).getEname());
        }
    }

    @Test
    public void wrongResultColumnLeavesNameNullWhenAutomaticMappingIsDisabled() {
        String broken = mapperXml
                .replace("id=\"empMap\"", "id=\"empMap\" autoMapping=\"false\"")
                .replace("property=\"ename\" column=\"ename\"", "property=\"ename\" column=\"wrong_ename\"");
        try (SqlSession session = build(broken).openSession()) {
            Emp emp = session.getMapper(EmpMapper.class).selectEmpById(1L);
            Assert.assertNotNull(emp);
            Assert.assertNull(emp.getEname());
            Assert.assertEquals("Engineer", emp.getJob());
        }
        try (SqlSession session = build(mapperXml).openSession()) {
            Assert.assertEquals("Lin Han", session.getMapper(EmpMapper.class).selectEmpById(1L).getEname());
        }
    }

    @Test
    public void missingGeneratedKeysInsertsRowButDoesNotPopulateId() {
        String broken = mapperXml.replace(" useGeneratedKeys=\"true\"", "");
        try (SqlSession session = build(broken).openSession()) {
            Emp emp = employee();
            EmpMapper mapper = session.getMapper(EmpMapper.class);
            Assert.assertEquals(1, mapper.insertEmp(emp));
            Assert.assertNull(emp.getEmpno());
            Assert.assertEquals(1, mapper.selectPreferred(emp.getEname(), null).size());
            session.rollback();
        }
        assertCorrectInsert();
    }

    private static void assertCorrectInsert() {
        try (SqlSession session = build(mapperXml).openSession()) {
            Emp emp = employee();
            Assert.assertEquals(1, session.getMapper(EmpMapper.class).insertEmp(emp));
            Assert.assertNotNull(emp.getEmpno());
            session.rollback();
        }
    }

    private static SqlSessionFactory build(String xml) {
        Properties properties = EmbeddedDatabase.properties("fault_diagnosis");
        UnpooledDataSource dataSource = new UnpooledDataSource(properties.getProperty("jdbc.driver"),
                properties.getProperty("jdbc.url"), "sa", "");
        Configuration configuration = new Configuration(
                new Environment("fault", new JdbcTransactionFactory(), dataSource));
        configuration.setLogImpl(Log4j2Impl.class);
        configuration.setMapUnderscoreToCamelCase(true);
        new XMLMapperBuilder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), configuration,
                "fault-copy.xml", configuration.getSqlFragments()).parse();
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private static Throwable rootCause(Throwable error) {
        while (error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    private static Emp employee() {
        Emp emp = new Emp();
        emp.setEname("Fault Drill Employee");
        emp.setJob("Test Engineer");
        emp.setHiredate(LocalDate.of(2025, 1, 6));
        emp.setSal(new BigDecimal("9000"));
        emp.setDeptno(40);
        return emp;
    }
}
