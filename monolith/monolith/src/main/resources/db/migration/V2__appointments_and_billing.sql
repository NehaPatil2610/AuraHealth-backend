CREATE TABLE appointments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  appointment_time DATETIME(6) NOT NULL,
  status VARCHAR(32) NOT NULL,
  symptoms TEXT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_appointments_doctor_time (doctor_id, appointment_time),
  KEY idx_appointments_patient (patient_id),
  CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE RESTRICT,
  CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE billing (
  id BIGINT NOT NULL AUTO_INCREMENT,
  appointment_id BIGINT NULL,
  amount DOUBLE NOT NULL,
  status VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_billing_appointment (appointment_id),
  CONSTRAINT fk_billing_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
