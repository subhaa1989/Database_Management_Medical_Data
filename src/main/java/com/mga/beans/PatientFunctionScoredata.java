package com.mga.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "patient_functionscore", propOrder = { "patientId", "id", "functionDate", "functionName", "functionValue" })
@XmlRootElement(name = "patient_functionscore")
public class PatientFunctionScoredata {
	
	@XmlElement(name = "patient_id", required = true)
	private Integer patientId;
	
	@XmlElement(name = "id", required = true)
	private Integer id;
	
	@XmlElement(name = "function_date", required = true)
	private String functionDate;

	@XmlElement(name = "function_key", required = true)
	private String functionName;

	@XmlElement(name = "function_value", required = true)
	private String functionValue;

	public Integer getPatientId() {
		return patientId;
	}

	public void setPatientId(Integer patientId) {
		this.patientId = patientId;
	}

	public String getFunctionDate() {
		return functionDate;
	}

	public void setFunctionDate(String functionDate) {
		this.functionDate = functionDate;
	}

	public String getFunctionName() {
		return functionName;
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	public String getFunctionValue() {
		return functionValue;
	}

	public void setFunctionValue(String functionValue) {
		this.functionValue = functionValue;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

}
