DELIMITER $$

CREATE PROCEDURE upgrade_legacy_aurahealth_schema()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'username'
  ) THEN
    ALTER TABLE users
      ADD COLUMN full_name VARCHAR(160) NULL,
      ADD COLUMN email VARCHAR(320) NULL,
      ADD COLUMN password_hash VARCHAR(100) NULL,
      ADD COLUMN role ENUM('PATIENT', 'DOCTOR') NULL,
      ADD COLUMN created_at DATETIME(6) NULL;

    UPDATE users u
    SET full_name = COALESCE(NULLIF(name, ''), 'Legacy user'),
        email = LOWER(username),
        password_hash = password,
        role = CASE
          WHEN EXISTS (
            SELECT 1 FROM user_roles ur
            WHERE ur.user_id = u.id AND ur.role = 'ROLE_DOCTOR'
          ) THEN 'DOCTOR'
          ELSE 'PATIENT'
        END,
        created_at = UTC_TIMESTAMP(6);

    ALTER TABLE users
      MODIFY full_name VARCHAR(160) NOT NULL,
      MODIFY email VARCHAR(320) NOT NULL,
      MODIFY password_hash VARCHAR(100) NOT NULL,
      MODIFY role ENUM('PATIENT', 'DOCTOR') NOT NULL,
      MODIFY created_at DATETIME(6) NOT NULL,
      ADD UNIQUE KEY uk_users_email (email);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'doctors' AND column_name = 'specialization'
  ) THEN
    ALTER TABLE doctors
      ADD COLUMN specialty VARCHAR(120) NULL,
      ADD COLUMN license_id VARCHAR(80) NULL,
      ADD COLUMN is_approved BOOLEAN NULL;

    UPDATE doctors
    SET specialty = COALESCE(NULLIF(specialization, ''), 'General Practice'),
        license_id = COALESCE(NULLIF(license_number, ''), CONCAT('LEGACY-', id)),
        is_approved = approval_status;

    ALTER TABLE doctors
      MODIFY specialty VARCHAR(120) NOT NULL,
      MODIFY license_id VARCHAR(80) NOT NULL,
      MODIFY is_approved BOOLEAN NOT NULL,
      ADD UNIQUE KEY uk_doctors_license_id (license_id);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'patients'
      AND column_name = 'subscription_tier' AND data_type <> 'enum'
  ) THEN
    UPDATE patients
    SET subscription_tier = CASE UPPER(COALESCE(subscription_tier, ''))
      WHEN 'EARLY_ASSISTANCE' THEN 'EARLY_ASSISTANCE'
      WHEN 'PRIORITY' THEN 'EARLY_ASSISTANCE'
      WHEN 'PERSONAL_ASSISTANCE' THEN 'PERSONAL_ASSISTANCE'
      WHEN 'PERSONAL' THEN 'PERSONAL_ASSISTANCE'
      WHEN 'COMPREHENSIVE_PRIORITY' THEN 'COMPREHENSIVE_PRIORITY'
      WHEN 'WELLNESS' THEN 'COMPREHENSIVE_PRIORITY'
      ELSE 'FREE'
    END;

    ALTER TABLE patients
      MODIFY subscription_tier ENUM('FREE', 'EARLY_ASSISTANCE', 'PERSONAL_ASSISTANCE', 'COMPREHENSIVE_PRIORITY') NOT NULL DEFAULT 'FREE';
  END IF;
END$$

CALL upgrade_legacy_aurahealth_schema()$$
DROP PROCEDURE upgrade_legacy_aurahealth_schema$$

DELIMITER ;
