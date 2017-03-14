package com.mga.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "patientFScore", propOrder = { "id", "FunctionName",
		"functionValue", "functionDate" })
@XmlRootElement(name = "patient")
public class PatientFScore {

	@XmlElement(name = "id", required = true)
	private Integer id;

	@XmlElement(name = "function_score_name", required = true)
	private String FunctionName;

	@XmlElement(name = "function_score_value", required = true)
	private String functionValue;
	
	@XmlElement(name = "function_score_date", required = true)
	private String functionDate;

	public String getFunctionName() {
		return FunctionName;
	}

	public void setFunctionName(String functionName) {
		FunctionName = functionName;
	}

	public String getFunctionValue() {
		return functionValue;
	}

	public void setFunctionValue(String functionValue) {
		this.functionValue = functionValue;
	}

	public String getFunctionDate() {
		return functionDate;
	}

	public void setFunctionDate(String functionDate) {
		this.functionDate = functionDate;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

}
