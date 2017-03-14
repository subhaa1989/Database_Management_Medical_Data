package com.mga.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "patient_misc_data", propOrder = { "PatientMiscData" })


@XmlRootElement(name = "patient_misc_data")
public class PatientsInsertMiscData {
	
	
	@XmlElement(name = "patient_miscdata", required = true)
	private List<PatientMiscData> PatientMiscData;

	public List<PatientMiscData> getPatientMiscData() {
		return PatientMiscData;
	}

	public void setPatientMiscData(List<PatientMiscData> patientMiscData) {
		PatientMiscData = patientMiscData;
	}
}
