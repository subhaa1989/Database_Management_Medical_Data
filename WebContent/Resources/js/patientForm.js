(function() {
	window.GATE = (window.GATE || {});
	window.GATE.PatientForm = (window.GATE.PatientForm || {});

	window.GATE.PatientForm.Initialize = function() {
		$('#BackButtonPageHead').attr('href', document.referrer);
		$('#GD-BirthDate').datetimepicker();
		$('.selectpicker').selectpicker();
	}

	window.GATE.PatientForm.validateDecimal = function(value, RE)    {
		var validRE = /^\d*\.?\d*$/;
		if(String(value).match(validRE)){
	    	if(String(value).match(RE)){
	 	       return true;
	 	    }else{
	 	       return false;
	 	    }
	    }else{
	    	return false;
	    }
	}
	
	
	window.GATE.PatientForm.handleData = (function (data, operation) {
		var code = data.status;
		var status = data.responseText;
		if (code == "200") {
			if(operation == "update"){
				window.GATE.Master.Notify(status, "green");
			}else{
				window.GATE.Master.Notify(status, "green");
				window.GATE.PatientForm.ClearForm();
			}
		}else{
			window.GATE.Master.Notify(status, "red");
		}
	}).bind(this);
	
	
	
	window.GATE.PatientForm.Create = function() {
		var height = document.getElementById("GD-Height").value;
		var weight = document.getElementById("GD-Weight").value;
		
		var wtValidation = window.GATE.PatientForm.validateDecimal(weight, /^\d{0,4}(\.\d{0,5})?$/);
		if(wtValidation){
			var htValidation = window.GATE.PatientForm.validateDecimal(height, /^\d{0,1}(\.\d{0,5})?$/);
			if(htValidation){
				//window.GATE.Master.Notify('Saving new patient data. Do not close this page... ', "yellow");
				var patient = window.GATE.PatientForm.BuildPatientXML();
				var xmlHttp = new XMLHttpRequest();
				xmlHttp.open("POST", "patient/metadata/insert", true);
				xmlHttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
				xmlHttp.onload = (function () {
					window.GATE.PatientForm.handleData(xmlHttp, "create");
		        }).bind(this);
				xmlHttp.send("patient-metadata=" + patient + "");
			}else{
				window.GATE.Master.Notify("Invalid height format... Valid format contains at most 1 digit before decimal point and atmost 5 digits after decimal...", "red");
			}
		}else{
			window.GATE.Master.Notify("Invalid weight format... Valid format contains at most 4 digits before decimal point and atmost 5 digits after decimal...", "red");
		}		
	}
	
	window.GATE.PatientForm.ClearForm = function(){
		$('input').val('');
		$('textarea').val('');
	}
	
	window.GATE.PatientForm.Update = function() {
		var height = document.getElementById("GD-Height").value;
		var weight = document.getElementById("GD-Weight").value;
		
		var wtValidation = window.GATE.PatientForm.validateDecimal(weight, /^\d{0,4}(\.\d{0,5})?$/);
		if(wtValidation){
			var htValidation = window.GATE.PatientForm.validateDecimal(height, /^\d{0,1}(\.\d{0,5})?$/);
			if(htValidation){
				//window.GATE.Master.Notify('Updating patient data. Do not close this page... ', "yellow");
				var patientMetadata = window.GATE.PatientForm.BuildPatientXML();
				var patientID = window.GATE.Master.GetURLParam('id',window.location.href);
				var xmlHttp = new XMLHttpRequest();
				var params = "patient-database-id=" + patientID + "&"+ "patient-metadata=" + patientMetadata + "";
				xmlHttp.open("POST", "patient/metadata/update", true);
				xmlHttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
				xmlHttp.onload = (function () {
					window.GATE.PatientForm.handleData(xmlHttp, "update");
		        }).bind(this);
				xmlHttp.send(params);
			}else{
				window.GATE.Master.Notify("Invalid height format... Valid format contains at most 1 digit before decimal point and atmost 5 digits after decimal...", "red");
			}
		}else{
			window.GATE.Master.Notify("Invalid weight format... Valid format contains at most 4 digits before decimal point and atmost 5 digits after decimal...", "red");
		}		
	}

	
	window.GATE.PatientForm.Delete = function() {
		
		var dfd = new $.Deferred();	
		dfd = window.GATE.Helper.DeleteConfirmation(dfd , "");
		
		dfd.then(function(e){
			//window.GATE.Master.Notify('Deleting patient data. Do not close this page... ', "yellow");
			var patientID = window.GATE.Master.GetURLParam('id', window.location.href);
			var xmlHttp = new XMLHttpRequest();
			xmlHttp.open("POST", "patient/metadata/delete?id=" + patientID + "",false);
			xmlHttp.onload = (function () {
				window.GATE.PatientForm.handleData(xmlHttp, "delete");
	        }).bind(this);
			xmlHttp.send(null);
		})
		
		dfd.fail(function() {})
		
	}

	
	window.GATE.PatientForm.FillPatientMetadataOnLoad = function() {
		var patientID = window.GATE.Master.GetURLParam('id',window.location.href);
		if (patientID !== null) {
			var xmlHttp = new XMLHttpRequest();
			xmlHttp.open("GET", "patient/metadata/select-patient?id=" + patientID + "", false);
			xmlHttp.send(null);
			var patientMetadata = xmlHttp.responseText;
			var xml = $.parseXML(patientMetadata);
			if ($(xml).find("code").text() == "500") {
				window.GATE.Master.Notify($(xml).find("status").text(), "red");
			}else{
				$(xml)
				.find("patient_metadata")
				.each(
						function() {
							$('#GD-FirstName').val(
									$(this).find("first_name").text());
							$('#GD-LastName').val(
									$(this).find("last_name").text());
							$('#GD-Gender').val(
									$(this).find("gender").text());
							$('#GD-BirthDate').val(
									$(this).find("birth_date").text());
							$('#GD-Age').val($(this).find("age").text());
							$('#GD-ContactNumber').val(
									$(this).find("contact_number").text());
							$('#GD-Email').val(
									$(this).find("email_id").text());
							$('#GD-Nr').val(
									$(this).find("house_number").text());
							$('#GD-Street').val(
									$(this).find("street").text());
							$('#GD-PostalCode').val(
									$(this).find("postal_code").text());
							$('#GD-City').val($(this).find("city").text());
							$('#GD-Country').val(
									$(this).find("country").text());
							$('#GD-Weight').val(
									$(this).find("weight").text());
							$('#GD-Height').val(
									$(this).find("height").text());
							$('#GD-Notes')
									.val($(this).find("notes").text());
							document
									.getElementsByName("patient-database-id")[0].value = patientID;
						});
			}
			
			$('#updateButton').css("visibility", "visible");
			$('#deleteButton').css("visibility", "visible");
		} else {
			$('#createButton').css("visibility", "visible");
		}
	}

	
	window.GATE.PatientForm.BuildPatientXML = function() {
		var patientMetadata = document.createElement("patientMetadata1");
		var Node = document.createElement("patient_metadata");

		// first name
		var first_name = document.createElement("first_name");
		var fnameContent = document.createTextNode(document
				.getElementById("GD-FirstName").value);
		first_name.appendChild(fnameContent);
		Node.appendChild(first_name);

		// last name
		var last_name = document.createElement("last_name");
		var lnameContent = document.createTextNode(document
				.getElementById("GD-LastName").value);
		last_name.appendChild(lnameContent);
		Node.appendChild(last_name);

		// gender
		var gender = document.createElement("gender");
		var genderContent = document.createTextNode(document
				.getElementById("GD-Gender").value);
		gender.appendChild(genderContent);
		Node.appendChild(gender);

		// birth date
		var birth_date = document.createElement("birth_date");
		var dobContent = document.createTextNode(document
				.getElementById("GD-BirthDate").value);
		birth_date.appendChild(dobContent);
		Node.appendChild(birth_date);

		// contact number
		var contact_no = document.getElementById("GD-ContactNumber").value;
		if (contact_no.substring(0, 1) == "+") {
			contact_no = contact_no.replace("+", "#43#");
		}
		var contact_number = document.createElement("contact_number");
		var contactContent = document.createTextNode(contact_no);
		contact_number.appendChild(contactContent);
		Node.appendChild(contact_number);

		// email id
		var email_id = document.createElement("email_id");
		var emailContent = document.createTextNode(document
				.getElementById("GD-Email").value);
		email_id.appendChild(emailContent);
		Node.appendChild(email_id);

		// street no
		var house_number = document.createElement("house_number");
		var nrContent = document.createTextNode(document
				.getElementById("GD-Nr").value);
		house_number.appendChild(nrContent);
		Node.appendChild(house_number);

		// street
		var street = document.createElement("street");
		var streetContent = document.createTextNode(document
				.getElementById("GD-Street").value);
		street.appendChild(streetContent);
		Node.appendChild(street);

		// postalcode
		var postal_code = document.createElement("postal_code");
		var pcContent = document.createTextNode(document
				.getElementById("GD-PostalCode").value);
		postal_code.appendChild(pcContent);
		Node.appendChild(postal_code);

		// city
		var city = document.createElement("city");
		var pcContent = document.createTextNode(document
				.getElementById("GD-City").value);
		city.appendChild(pcContent);
		Node.appendChild(city);

		// country
		var country = document.createElement("country");
		var countryContent = document.createTextNode(document
				.getElementById("GD-Country").value);
		country.appendChild(countryContent);
		Node.appendChild(country);

		// weight
		var weight = document.createElement("weight");
		var weightContent = document.createTextNode(document
				.getElementById("GD-Weight").value);
		weight.appendChild(weightContent);
		Node.appendChild(weight);

		// height
		var height = document.createElement("height");
		var heightContent = document.createTextNode(document
				.getElementById("GD-Height").value);
		height.appendChild(heightContent);
		Node.appendChild(height);

		// notes
		var notes = document.createElement("notes");
		var notesContent = document.createTextNode(document
				.getElementById("GD-Notes").value);
		notes.appendChild(notesContent);
		Node.appendChild(notes);

		patientMetadata.appendChild(Node);

		return patientMetadata.innerHTML;
	}
})(jQuery);


$(document).ready(function() {
	window.GATE.PatientForm.Initialize();
	window.GATE.PatientForm.FillPatientMetadataOnLoad();
});