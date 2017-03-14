package com.mga.beans;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "functionscoredetails", propOrder = { "PatientFunctionScore" })
@XmlRootElement(name = "functionscoredetails")
public class PatientsFunctionScoreData {

			
		@XmlElement(name = "patient_functionscore", required = true)
		private List<PatientFunctionScoredata> PatientFunctionScore;

		public List<PatientFunctionScoredata> getPatientFunctionScore() {
			return PatientFunctionScore;
		}

		public void setPatientFunctionScore(List<PatientFunctionScoredata> patientFunctionScore) {
			PatientFunctionScore = patientFunctionScore;
		}
		
		
	

}
