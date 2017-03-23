//
var target_address = 'http://masterrestfull-metadataanalyser.rhcloud.com/app/metadata';
var progress_bas = 7;
var progress_sup = 4;

$(document).ready(function() {
		
	// disable progress bar layer
	toogleProgressArea(0);
	// set progress bar properties
	var progressbar   = $("#progressbar");
	var progressLabel = $(".progress-label");
	progressbar.progressbar({
		value : 0, max : (progress_bas + progress_sup),
		change : function() {
			progressLabel.text(
				"Completion percentage: " + 
				parseStrToFloat((progressbar.progressbar("value") * 100) / 
						(progress_bas + progress_sup), 0) + 
				"%"
				);
		},
		complete : function() {
			progressLabel.text(
				"The analysis has been completed successfully!");
		}
	});
	// set acordion property
	$('#ajaxlayer').accordion({
	    header: "h3",
	    autoHeight: true,
	    heightStyle: "content",
	    autoFill:true 
	});
	// bind click submit button event
	$('#submitbutton').click(submitFormFile);
});

//
function showprogress(message) {
	var progressbar = $("#progressbar");
	var val = progressbar.progressbar("value") || 0;
	progressbar.progressbar("value", val + 1);
	$('#progresspan').append("<p>"+message+"</p>");
}

//
function longpoolit(poolid) {
	// send a asynchronous request to the server
	$.ajax({
		type : "GET",
		url : target_address + "/polling/" + poolid,
		dataType : "json", // Incoming data type text
		success : function(data) {
			switch (data.type) {
			case 0:
				if (data.message) {
					// inform about progress
					showprogress('Processing: ' + data.message);
					// link to the re-polling when a message is consumed.
					longpoolit(poolid);
				}
				break;
			case 1:
				if (data.message) {
					// inform about progress
					showprogress('Completed: (PoolID) ' + data.poolID);
					// print out the final result
					responsePrettyPrint(data.poolID, data);
					$('#ajaxlayer').accordion("refresh");					
					// enable submit form button
					setTimeout(toogleProgressArea(0), 10000);
				}
				break;
			default:
				if (data.message) {
					// inform about the progress
					$('#resultarea').html('Error: (PoolID) ' + data.message);
					// enable submit form button
					setTimeout(toogleProgressArea(0), 3000);					
				}
				break;
			}
		},
		error : function() {
			// Start re-polling if an error occurs.
			longpoolit(poolid);
		}
	});
}

//
function toogleProgressArea(toggle) {
	//
	$('#progresspan').html("");
	if (toggle == 1) {
		//
		$('#progressbar').progressbar('option', 'value', 0);
		// disable submit form button
		objStateToggle("#submitbutton").hide();
		// enable progress layer
		objStateToggle("#progresslayer").show();
		// enable progress layer
		objStateToggle("#progresspan").show();
	} else {
		// enable submit form button
		objStateToggle("#submitbutton").show();
		// disable progress layer
		objStateToggle("#progresslayer").hide();
		// enable progress layer
		objStateToggle("#progresspan").hide();		
	}
}

//
function submitFormFile(event) {
	event.stopPropagation();
	// toggle the progress area
	toogleProgressArea(1);
	// collect file from form input field
	var formData = new FormData();
	var file = $('input[name="metafile"').get(0).files[0];
	formData.append('file', file);
	// collect url location from form input field
	var location = $('input[name="metaurl"').val();
	formData.append('location', location);	
	// set default repository id
	formData.append('repository', 0);
	// send a asynchronous request to the server
	$.ajax({
		url : target_address + "/submitfile",
		type : 'POST',
		cache : false,
		dataType : 'json',
		data : formData,
		contentType : false,
		processData : false,
		success : function(data) {			
			if (data.poolID) {
				// inform about progress
				var message = 'Starting: (PoolID) ' + data.poolID; 
				showprogress(message);
				longpoolit(data.poolID);

			} else {
				// inform about the progress
				$('#resultarea').html('Failure: (PoolID) ');
				// enable submit form button
				setTimeout(toogleProgressArea(0), 3000);
			}
		},
		error : function(jqXHR, status, error) {
			// inform about the progress
			$('#resultarea').html('Error: ' + status);
			// enable submit form button
			setTimeout(toogleProgressArea(0), 3000);
		}
	});
}

// 
function responsePrettyPrint(poolid, data) {
	// show data layer html object
	objStateToggle("#ajaxlayer").show();
	// print the results out
	metabolightsPrint(poolid, $.parseJSON(data.message), $('#ajaxlayer'));
}

//
function metabolightsPrint(poolid, data, container) {
	// add tab header element
	var div = $("<div />"); div.hide();
	// add description header
	container.append($("<h3>MetaboLights Study ID " + data.id + "</h3>"));
	container.append(div);
	// add table header element
	var table = $("<table class='result-table' />");
	div.append(table);
	// add table header
	containerAddStudyHeader(poolid, data, table);
	// add table ontologies
	containerAddOntologies("Ontologies", data.ontologies, table);
	// add table classes description
	containerAddClassesDesc("Classes List", data.metaClasses, table);
	// add table classes analysis
	containerAddClassesAnal("Class Analysis", data.metaClasses, table);
	// HTML complete show results
	div.show();
}

//
function containerAddStudyHeader(poolid, data, container) {
	// add download CSV file button
	var row = $("<tr />");
	container.append(row);
	var rowvalue = "<a href='http://json-csv.com/?u=http://masterrestfull-metadataanalyser.rhcloud.com/app/metadata/tojson/" + poolid + 
				   "' target='_blank'><img alt='Export this result to CSV file format' src='http://masterweb-metadataanalyser.rhcloud.com/images/csv_1.png' /></a>" + 
				   "<a href='http://masterrestfull-metadataanalyser.rhcloud.com/app/metadata/tojson/" + poolid + 
				   "' target='_blank'><img alt='Export this result to JSON file format' src='http://masterweb-metadataanalyser.rhcloud.com/images/json_1.png' /></a>";
	row.append($("<td style='text-align:right;' colspan='2'>" + rowvalue + "</td>"));
	// adds header elements
	containerAddRow("Study ID:", data.id, container);
	containerAddRow("Study Specificity:", parseStrToFloat(data.specValue, 3), container);
	containerAddRow("Study Coverage:", parseStrToFloat(data.covValue, 3), container);
	containerAddRow("Unique ID:", data.uniqueID, container);
	containerAddRow("Parse Duration (miliseconds):", data.parseDuration, container);
}

//
function containerAddClassesAnal(rowdesc, data, container) {
	//
	containerAddRow(rowdesc, "", container);

	var mainrow = $("<tr />");
	container.append(mainrow);
	mainrow.append($("<td />"));
	// iterate over all classes results
	var maincolumn = $("<td />");
	mainrow.append(maincolumn);
	$.each(data, function(index, row) {
		var innertable = $("<table class = 'class-table' />");
		maincolumn.append(innertable);
		// list class properties
		containerAddClassHeader("Class Details", row, innertable);
		// list all class annotations found
		containerAddClassAnnotations("Annotation List", row.metaAnnotations, innertable, 3);
		// list all class terms found
		containerAddClassTerms("Terms List", row.metaTerms, innertable, 3);
		//
		maincolumn.append($("<br />"));
	});
}

//
function containerAddClassHeader(rowdesc, data, container) {
	//
	containerAddRow("Name: " + data.name, "", container);
	//
	var mainrow = $("<tr />");
	container.append(mainrow);
	var maincolumn = $("<td colspan='2' />");
	mainrow.append(maincolumn);
	var innertable = $("<table class='spec-table' />");
	maincolumn.append(innertable);
	var innerrow = $("<tr />");
	innertable.append(innerrow);
	//
	innerrow.append("<td>Specificity Value: " + parseStrToFloat(data.specValue, 3) + "</td>");
	innerrow.append("<td>Coverage Value: " + parseStrToFloat(data.covValue, 3) + "</td>");
	innerrow.append("<td>Annotations Count: " + data.metaAnnotations.length + "</td>");
	innerrow.append("<td>Terms Count: " + data.metaTerms.length + "</td>");
	//
	return innerrow;
}

//
function containerAddClassAnnotations(rowdesc, data, container, cspan) {
	// print description value
	rowdesc += " (" + data.length + ")";
	containerAddRow(rowdesc, "", container);
	// iterate over all class annotations
	var mainrow = $("<tr />");
	container.append(mainrow);
	mainrow.append("<td />");
	var maincolumn = $("<td />");
	mainrow.append(maincolumn);
	var innertable = $("<table class='spec-table' />");
	maincolumn.append(innertable);
	$.each(data, function(index, row) {
		var innerrow = $("<tr />");
		innertable.append(innerrow);
		innerrow.append("<td>URI: " + row.uri + "</td>");
		innerrow.append("<td>Specificity Value: " + parseStrToFloat(row.specValue, 3) + "</td>");
	});
}

//
function containerAddClassTerms(rowdesc, data, container, cspan) {
	// print description value
	rowdesc += " (" + data.length + ")";
	containerAddRow(rowdesc, "", container);
	// iterate over all class annotations
	var mainrow = $("<tr />");
	container.append(mainrow);
	mainrow.append("<td />");
	var maincolumn = $("<td />");
	mainrow.append(maincolumn);
	var innertable = $("<table class='spec-table' />");
	maincolumn.append(innertable);
	$.each(data, function(index, row) {
		var innerrow = $("<tr />");
		innertable.append(innerrow);
		innerrow.append("<td>Name: " + row.name + "</td>");
	});
}

//
function containerAddClassesDesc(rowdesc, data, container) {
	// iterate over all classes description
	var rowvalue = "";
	var counter = 1;
	$.each(data, function(index, row) {
		rowvalue += row.name;
		if (counter < data.length) {
			rowvalue += ", ";
		}
		counter++;
	});
	rowdesc += " (" + data.length + "): ";
	containerAddRow(rowdesc, rowvalue, container);
}

//
function containerAddRow(rowdesc, rowvalue, container, valuespan) {
	var row = $("<tr />");
	container.append(row);
	row.append($("<td class='caption'>" + rowdesc + "</td>"));
	row.append($("<td class='value' " + ((valuespan > 0) ? "colspan = '" + valuespan + "'" : "") + ">" + rowvalue + "</td>"));
}

//
function containerAddOntologies(rowdesc, data, container) {
	var mainrow = $("<tr />");
	container.append(mainrow);
	mainrow.append($("<td class='caption'>" + rowdesc + " (" + data.length + "): </td>"));
	//
	var maincolumn = $("<td class='value' />");
	mainrow.append(maincolumn);
	var innertable = $("<table />");
	maincolumn.append(innertable);
	$.each(data, function(index, row) {
		containerAddRow("URI: ", row.uri, innertable);
	});
}

//
function objStateToggle(id) {
	var object = {
		"id" : id,
		"state" : "on",
		"hide" : function() {
			$(this.id).hide();
			this.state = "off";
		},
		"show" : function() {
			$(this.id).show();
			this.state = "on";
		},
		"toggle" : function() {
			if (this.state == "on") {
				this.hide();
			} else {
				this.show();
			}
		}
	};
	return object;
};

//
function parseStrToFloat(value, decimal) {
	var result = parseFloat(value).toFixed(decimal);
	if (result != NaN && result >= 0) {
		return result;
	}
	return "NA";

}
