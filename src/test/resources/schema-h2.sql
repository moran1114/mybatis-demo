DROP TABLE IF EXISTS staff_profile;

CREATE TABLE staff_profile (
    staff_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(50) NOT NULL,
    gender_mark CHAR(1) NOT NULL DEFAULT 'U',
    business_unit VARCHAR(60) NOT NULL,
    position_title VARCHAR(60) NOT NULL,
    monthly_pay DECIMAL(12, 2) NOT NULL CHECK (monthly_pay >= 0),
    entry_date DATE NOT NULL,
    active_state TINYINT NOT NULL DEFAULT 1 CHECK (active_state IN (0, 1))
);

CREATE INDEX idx_staff_profile_name ON staff_profile(full_name);
CREATE INDEX idx_staff_profile_unit_state ON staff_profile(business_unit, active_state);
