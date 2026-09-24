package dev.course.persistence.query;

/** SQL 标识符白名单，不能把任意用户字符串传给 ${}。 */
public enum EmpSort {
    EMPNO("empno"), SAL("sal"), HIREDATE("hiredate");

    private final String column;

    EmpSort(String column) {
        this.column = column;
    }

    public String getColumn() {
        return column;
    }
}
