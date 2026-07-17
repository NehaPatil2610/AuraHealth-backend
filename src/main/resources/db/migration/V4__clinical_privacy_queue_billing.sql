-- Doctor search & consultation metadata
ALTER TABLE doctors
  ADD COLUMN city VARCHAR(120) NULL DEFAULT 'Mumbai',
  ADD COLUMN consultation_fee DOUBLE NOT NULL DEFAULT 500;

-- Patient vitals & clinical summary (medical_history already stores summary)
ALTER TABLE patients
  ADD COLUMN current_vitals TEXT NULL;

-- Appointment queue, billing, and priority flags
ALTER TABLE appointments
  ADD COLUMN billing_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
  ADD COLUMN queue_number INT NULL,
  ADD COLUMN is_priority BOOLEAN NOT NULL DEFAULT FALSE;

-- Richer invoice records
ALTER TABLE billing
  ADD COLUMN invoice_number VARCHAR(32) NULL,
  ADD COLUMN description VARCHAR(500) NULL,
  ADD COLUMN patient_id BIGINT NULL,
  ADD CONSTRAINT fk_billing_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE SET NULL;

CREATE TABLE notifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  message TEXT NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notifications_user (user_id),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE feedback (
  id BIGINT NOT NULL AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  rating INT NOT NULL,
  comment TEXT NOT NULL,
  is_spam BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_feedback_doctor (doctor_id),
  KEY idx_feedback_patient (patient_id),
  CONSTRAINT fk_feedback_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
  CONSTRAINT fk_feedback_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
