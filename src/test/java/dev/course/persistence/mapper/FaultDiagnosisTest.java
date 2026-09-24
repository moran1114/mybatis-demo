package dev.course.persistence.mapper;

import dev.course.persistence.entity.Emp;
import dev.course.persistence.support.EmbeddedDatabase;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Properties;

/** 仅修改内存中的 XML 副本，正常 Mapper 和本机 MySQL 不受故障演练影响。 */
public class FaultDiagnosisTest {
    private static final Logger LOG = LogManager.getLogger(FaultDiagnosisTest.class);
    private static String mapperXml;

    @BeforeClass
    public static void setup() throws Exception {
        EmbeddedDatabase.initialize("fault_diagnosis");
        try (InputStream stream = Resources.getResourceAsStream(
                "dev/course/persistence/mapper/EmpMapper.xml")) {
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
            LOG.info("故障1：错误实体属性 -> {}: {}", cause.getClass().getSimpleName(), cause.getMessage());
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
            LOG.info("补充演练：错误 @Param 名称 -> {}: {}", cause.getClass().getSimpleName(), cause.getMessage());
        }
        try (SqlSession session = build(mapperXml).openSession()) {
            Assert.assertEquals("林澈", session.getMapper(EmpMapper.class).selectEmpById(1L).getEname());
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
            Assert.assertEquals("工程师", emp.getJob());
            LOG.info("故障2：SQL 查到 empno={}，错误 resultMap 的 ename={}；其他字段正常。", emp.getEmpno(), emp.getEname());
        }
        try (SqlSession session = build(mapperXml).openSession()) {
            Assert.assertEquals("林澈", session.getMapper(EmpMapper.class).selectEmpById(1L).getEname());
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
            LOG.info("故障3：INSERT 更新行数=1，但 Java 对象 empno={}。", emp.getEmpno());
            session.rollback();
        }
        assertCorrectInsert();
    }

    private static void assertCorrectInsert() {
        try (SqlSession session = build(mapperXml).openSession()) {
            Emp emp = employee();
            Assert.assertEquals(1, session.getMapper(EmpMapper.class).insertEmp(emp));
            Assert.assertNotNull(emp.getEmpno());
            LOG.info("恢复正确映射：INSERT 成功，主键回填 empno={}。", emp.getEmpno());
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
        emp.setEname("故障演练员工");
        emp.setJob("测试工程师");
        emp.setHiredate(LocalDate.of(2025, 1, 6));
        emp.setSal(new BigDecimal("9000"));
        emp.setDeptno(40);
        return emp;
    }
}
