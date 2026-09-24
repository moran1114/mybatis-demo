package dev.course.persistence.util;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class MyBatisUtil {

    private static final String CONFIG_RESOURCE = "mybatis-config.xml";
    private static final String DATABASE_RESOURCE = "database.properties";

    private MyBatisUtil() {
    }

    public static SqlSession openSession() {
        return Holder.FACTORY.openSession(false);
    }

    public static SqlSession openMyBatisSession() {
        return MyBatisHolder.FACTORY.openSession(false);
    }

    // 基础实验使用原生构建器，只执行 Mapper XML 中的 SQL。
    public static SqlSessionFactory buildMyBatis(Properties databaseProperties) {
        try (Reader reader = Resources.getResourceAsReader(CONFIG_RESOURCE)) {
            return new SqlSessionFactoryBuilder().build(reader, "local", databaseProperties);
        } catch (IOException exception) {
            throw new IllegalStateException("无法构建 MyBatis SqlSessionFactory", exception);
        }
    }

    // 增强实验替换为 MP 构建器，注入 BaseMapper CRUD 并注册分页插件。
    public static SqlSessionFactory build(Properties databaseProperties, DbType databaseType) {
        try (Reader reader = Resources.getResourceAsReader(CONFIG_RESOURCE)) {
            // MP 3.5.3.1 的默认泛型解析依赖 Spring；纯 Java 工程使用此解析器。
            GenericTypeUtils.setGenericTypeResolver(MyBatisUtil::resolveTypeArguments);
            SqlSessionFactory factory = new MybatisSqlSessionFactoryBuilder()
                    .build(reader, "local", databaseProperties);

            PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(databaseType);
            pagination.setOverflow(false);
            pagination.setMaxLimit(100L);

            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(pagination);
            factory.getConfiguration().addInterceptor(interceptor);
            return factory;
        } catch (IOException exception) {
            throw new IllegalStateException("无法构建 MyBatis SqlSessionFactory", exception);
        }
    }

    public static Properties localDatabaseProperties() {
        Properties properties = new Properties();
        try (InputStream stream = Resources.getResourceAsStream(DATABASE_RESOURCE);
             Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取数据库配置", exception);
        }

        override(properties, "LAB_DB_DRIVER", "jdbc.driver");
        override(properties, "LAB_DB_URL", "jdbc.url");
        override(properties, "LAB_DB_USER", "jdbc.username");
        override(properties, "LAB_DB_PASSWORD", "jdbc.password");
        return properties;
    }

    private static void override(Properties properties, String environmentName, String propertyName) {
        String value = System.getenv(environmentName);
        if (value != null) {
            properties.setProperty(propertyName, value);
        }
    }

    private static Class<?>[] resolveTypeArguments(Class<?> sourceType, Class<?> targetType) {
        return resolveCandidate(sourceType, targetType, new HashMap<>());
    }

    private static Class<?>[] resolveCandidate(Type candidate, Class<?> targetType,
                                                Map<TypeVariable<?>, Type> inheritedTypes) {
        Class<?> rawClass;
        Map<TypeVariable<?>, Type> resolvedTypes = new HashMap<>(inheritedTypes);

        if (candidate instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) candidate;
            Type rawType = parameterized.getRawType();
            if (!(rawType instanceof Class<?>)) {
                return null;
            }
            rawClass = (Class<?>) rawType;
            TypeVariable<?>[] variables = rawClass.getTypeParameters();
            Type[] arguments = parameterized.getActualTypeArguments();
            for (int index = 0; index < variables.length; index++) {
                resolvedTypes.put(variables[index], resolveReference(arguments[index], inheritedTypes));
            }
        } else if (candidate instanceof Class<?>) {
            rawClass = (Class<?>) candidate;
        } else {
            return null;
        }

        if (rawClass == targetType) {
            TypeVariable<?>[] variables = rawClass.getTypeParameters();
            Class<?>[] result = new Class<?>[variables.length];
            for (int index = 0; index < variables.length; index++) {
                result[index] = toClass(resolveReference(variables[index], resolvedTypes));
            }
            return result;
        }

        for (Type interfaceType : rawClass.getGenericInterfaces()) {
            Class<?>[] result = resolveCandidate(interfaceType, targetType, resolvedTypes);
            if (result != null) {
                return result;
            }
        }

        Type parent = rawClass.getGenericSuperclass();
        return parent == null ? null : resolveCandidate(parent, targetType, resolvedTypes);
    }

    private static Type resolveReference(Type type, Map<TypeVariable<?>, Type> resolvedTypes) {
        Type current = type;
        while (current instanceof TypeVariable<?> && resolvedTypes.containsKey(current)) {
            current = resolvedTypes.get(current);
        }
        return current;
    }

    private static Class<?> toClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            }
        }
        return Object.class;
    }

    private static final class Holder {
        private static final SqlSessionFactory FACTORY =
                build(localDatabaseProperties(), DbType.MYSQL);
    }

    private static final class MyBatisHolder {
        private static final SqlSessionFactory FACTORY = buildMyBatis(localDatabaseProperties());
    }
}
