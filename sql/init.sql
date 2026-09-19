CREATE DATABASE IF NOT EXISTS persistence_lab
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS persistence_lab.staff_profile;

CREATE TABLE persistence_lab.staff_profile (
    staff_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '档案主键',
    full_name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender_mark CHAR(1) NOT NULL DEFAULT 'U' COMMENT '性别代码：M/F/U',
    business_unit VARCHAR(60) NOT NULL COMMENT '业务单元',
    position_title VARCHAR(60) NOT NULL COMMENT '岗位名称',
    monthly_pay DECIMAL(12, 2) NOT NULL COMMENT '月薪',
    entry_date DATE NOT NULL COMMENT '入职日期',
    active_state TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1在职，0离职',
    CONSTRAINT ck_staff_profile_gender CHECK (gender_mark IN ('M', 'F', 'U')),
    CONSTRAINT ck_staff_profile_pay CHECK (monthly_pay >= 0),
    CONSTRAINT ck_staff_profile_state CHECK (active_state IN (0, 1)),
    INDEX idx_staff_profile_name (full_name),
    INDEX idx_staff_profile_unit_state (business_unit, active_state)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '人员档案';

INSERT INTO persistence_lab.staff_profile
    (full_name, gender_mark, business_unit, position_title,
     monthly_pay, entry_date, active_state)
VALUES
    ('林澈', 'M', '云平台组', 'Java研发', 14500.00, '2023-10-09', 1),
    ('唐可', 'F', '体验设计组', '交互设计', 11800.00, '2024-01-15', 1),
    ('许舟', 'M', '数据质量组', '数据治理', 13600.00, '2022-08-08', 1),
    ('周禾', 'F', '客户运营组', '客户顾问', 9400.00, '2024-05-20', 1),
    ('陈安', 'M', '财务支持组', '成本分析', 10700.00, '2021-03-11', 1),
    ('沈知', 'F', '云平台组', '站点可靠性', 15600.00, '2020-11-23', 0);
