var wahlversprechen_authors_json_url = "http://www.wahlversprechen2013.de/json/authors";
var wahlversprechen_categories_json_url = "http://www.wahlversprechen2013.de/json/categories";
var wahlversprechen_items_json_url = "http://www.wahlversprechen2013.de/json/items/"; //Add author.name to get the election promises of that author

var authors = [];
var categories = [];
var election_promises = [];
var election_promises_by_category_counter = [];
var seriesValues = [];

$(document).ready(function () {

	$.ajax({
		type: "GET",
		url: wahlversprechen_authors_json_url,
		async: false,
		dataType: "json",
		success: function (data) {
			
			$.each(data, function(index, author) {
				authors.push(author.name);
			});
		}
	});
	
	$.ajax({
		type: "GET",
		url: wahlversprechen_categories_json_url,
		async: false,
		dataType: "json",
		success: function (data) {
			
			$.each(data, function(index, category) {
				categories.push(category.name);
			});
		}
	});

	//build 3d array to contain counts of topics of election promises by author
	for(var i = 0; i < categories.length; i++) {
		for(var k = 0; k < authors.length; k++) {
			election_promises_by_category_counter.push([]);
			election_promises_by_category_counter[i].push([]);
			election_promises_by_category_counter[i][k] = 0;
		}
	}
	
	console.log(election_promises_by_category_counter);
	
	
	for(var i = 0; i < authors.length; i++) {
		$.ajax({
			type: "GET",
			url: wahlversprechen_items_json_url+authors[i],
			async: false,
			dataType: "json",
			success: function (data) {
				$.each(data, function(index, election_promise) {
					election_promises.push(election_promise);
					election_promises_by_category_counter[categories.indexOf(election_promise.category)][authors.indexOf(election_promise.author)]++;
				});
				
				
				//Prepare data for chart
				var name = authors[i];
				var data = [];
				for(var n = 0; n < categories.length; n++) {
					data.push(election_promises_by_category_counter[n][i]);
				}
				seriesValues.push({
					name: name,
					data: data
				});
			}
		});
	}

	$('#container').highcharts({
		chart: {
			type: 'bar'
		},
		title: {
			text: 'Promises made by Parties grouped by Categories'
		},
		xAxis: {
			categories: categories
		},
		yAxis: {
			min: 0,
			title: {
				text: 'Number of Election Promises'
			}
		},
		legend: {
			reversed: true
		},
		plotOptions: {
			series: {
				stacking: 'normal'
			}
		},
		series: seriesValues
	});
});