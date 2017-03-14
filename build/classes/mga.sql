CREATE SCHEMA IF NOT EXISTS medical_gate_analysis ;

CREATE TABLE IF NOT EXISTS medical_gate_analysis.doctor_metadata (
	doctor_id VARCHAR(10) NOT NULL,
	first_name VARCHAR(50) NOT NULL,
	last_name VARCHAR(50) NOT NULL,
	medical_center TEXT NOT NULL,
	notes TEXT,
	PRIMARY KEY (doctor_id)
)COMMENT = 'Table to store Doctor\'s metadata';

CREATE TABLE IF NOT EXISTS medical_gate_analysis.patient_metadata (
 patient_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
 first_name VARCHAR(50) NOT NULL,
 last_name VARCHAR(50) NOT NULL,
 gender VARCHAR(12) NOT NULL,
 birth_date DATE,
 contact_number VARCHAR(30),
 email_id VARCHAR(50),
 house_number VARCHAR(10),
 street TEXT,
 postal_code VARCHAR(15),
 city VARCHAR(30) NOT NULL,
 country VARCHAR(30) NOT NULL,
 weight DECIMAL(6,2) UNSIGNED,
 height DECIMAL(5,2) UNSIGNED,
 notes TEXT,
 creation_date DATE,
 last_update DATE,
 PRIMARY KEY (patient_id)
) COMMENT = 'Table to store Patient\'s metadata';

CREATE TABLE IF NOT EXISTS medical_gate_analysis.patient_sole_data ( 
 id INT UNSIGNED NOT NULL AUTO_INCREMENT,
 patient_id INT UNSIGNED NOT NULL,
 file VARCHAR(50),
 time DATETIME,
 time_difference INT,
 left_pressure0 FLOAT, 
 left_pressure1 FLOAT, 
 left_pressure2 FLOAT,
 left_pressure3 FLOAT,
 left_pressure4 FLOAT, 
 left_pressure5 FLOAT, 
 left_pressure6 FLOAT, 
 left_pressure7 FLOAT,
 left_pressure8 FLOAT, 
 left_pressure9 FLOAT, 
 left_pressure10 FLOAT, 
 left_pressure11 FLOAT, 
 left_pressure12 FLOAT,
 left_acceleration_x FLOAT, 
 left_acceleration_y FLOAT, 
 left_acceleration_z FLOAT, 
 left_total_force FLOAT, 
 left_cop_x FLOAT, 
 left_cop_y FLOAT, 
 left_temperature FLOAT,
 right_pressure0 FLOAT, 
 right_pressure1 FLOAT, 
 right_pressure2 FLOAT,
 right_pressure3 FLOAT,
 right_pressure4 FLOAT, 
 right_pressure5 FLOAT, 
 right_pressure6 FLOAT, 
 right_pressure7 FLOAT,
 right_pressure8 FLOAT, 
 right_pressure9 FLOAT,
 right_pressure10 FLOAT, 
 right_pressure11 FLOAT, 
 right_pressure12 FLOAT,
 right_acceleration_x FLOAT, 
 right_acceleration_y FLOAT, 
 right_acceleration_z FLOAT, 
 right_total_force FLOAT, 
 right_cop_x FLOAT, 
 right_cop_y FLOAT, 
 right_temperature FLOAT,
 PRIMARY KEY (id),
 FOREIGN KEY (patient_id) REFERENCES patient_metadata(patient_id)
)COMMENT = 'Table to store Patient\'s Sole data';

CREATE TABLE IF NOT EXISTS medical_gate_analysis.patient_function_scores_data (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    patient_id INT UNSIGNED NOT NULL,
    function_score_name VARCHAR(50),
    function_score_value VARCHAR(200),
    function_score_date Date,
   PRIMARY KEY (id),
   FOREIGN KEY (patient_id) REFERENCES patient_metadata(patient_id)
)COMMENT = 'Table to store Patient\'s function-score data'; 

CREATE TABLE IF NOT EXISTS medical_gate_analysis.patient_sole_data_files (
 id INT UNSIGNED NOT NULL AUTO_INCREMENT, 
 patient_id INT UNSIGNED NOT NULL,
 sole_data_txt_file VARCHAR(200),
 sole_data_txt_file_insert_timestamp DATETIME,
 file_timestamp DATETIME,
 notes TEXT,
 min_left_pressure FLOAT,
 min_right_pressure FLOAT,
 max_left_pressure FLOAT,
 max_right_pressure FLOAT,
 mean_left_pressure FLOAT,
 mean_right_pressure FLOAT,
 var_left_pressure FLOAT,
 var_right_pressure FLOAT,
 std_dev_left_pressure FLOAT,
 std_dev_right_pressure FLOAT,
 resting_time_left FLOAT,
 resting_time_right FLOAT,
 min_of_activity_left FLOAT,
 min_of_activity_right FLOAT,
 file_content LONGBLOB,
 PRIMARY KEY (id),
 FOREIGN KEY (patient_id) REFERENCES patient_metadata(patient_id)
)COMMENT = 'Table to store Patient\'s Sole data file\'s details';

CREATE TABLE IF NOT EXISTS medical_gate_analysis.patient_other_files (
 id INT UNSIGNED NOT NULL AUTO_INCREMENT, 
 patient_id INT UNSIGNED NOT NULL,
 file_name VARCHAR(200),
 file_type VARCHAR(50),
 file_insert_timestamp DATETIME,
 file_content LONGBLOB,
 PRIMARY KEY (id),
 FOREIGN KEY (patient_id) REFERENCES patient_metadata(patient_id)
)COMMENT = 'Table to store Patient\'s other data file\'s details';

CREATE TABLE IF NOT EXISTS medical_gate_analysis.patient_misc_data (
id INT UNSIGNED NOT NULL AUTO_INCREMENT, 
patient_id INT UNSIGNED NOT NULL,
pain_level_nrs INT,
pain_level_vas INT,
date Date,
content_level INT,
PRIMARY KEY (id),
FOREIGN KEY (patient_id) REFERENCES patient_metadata(patient_id)
)COMMENT = 'Table to store Patient\'s miscellaneous data details';


