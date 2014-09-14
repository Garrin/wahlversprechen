
/*
 * Ideen
 * Welche Partei bricht die meisten Wahlversprechen (absolut/prozentual)?
 * Wie viele Wahlversprechen gibt es pro Kategorie? - Schwierig: Welche sind unterschiedlich und welche sind gleich?
 * Zu welchen Tags gibt es wie viele Versprechen?
 */

var wahlversprechen_authors_json_url = "http://www.wahlversprechen2013.de/json/authors";
var wahlversprechen_categories_json_url = "http://www.wahlversprechen2013.de/json/categories";
var wahlversprechen_items_json_url = "http://www.wahlversprechen2013.de/json/items/"; //Add author.name to get the election promises of that author

var authors = [];
var authors_names = [];
var categories = [];
var categories_names = [];
var election_promises = [];
var election_promises_by_category_counter = [];
var election_promises_by_rating_counter = [];
var election_promises_grouped_by_parties_grouped_by_categories_series_values = [];
var election_promises_grouped_by_ratings_grouped_by_categories_series_values = [];
var ratings_names = [];

$(document).ready(function () {

	//authors = getAuthors();
	$.ajax({
		type: "GET",
		url: wahlversprechen_authors_json_url,
		async: false,
		dataType: "json",
		success: function (data) {
			$.each(data, function (index, author) {
				authors_names.push(author.name);
				authors.push(author);
			});
		},
		failure: function (response) {
			console.log(response);
			console.log("Failed to retrieve authors.");
		}
	});

	//categories = getCategories();
	$.ajax({
		type: "GET",
		url: wahlversprechen_categories_json_url,
		async: false,
		dataType: "json",
		success: function (data) {
			$.each(data, function (index, category) {
				categories.push(category);
				categories_names.push(category.name);
			});
		},
		failure: function (response) {
			console.log(response);
			console.log("Failed to retrieve categories.");
		}
	});

	//electionPromises = getElectionPromises();
	for (var i = 0; i < authors.length; i++) {
		$.ajax({
			type: "GET",
			url: wahlversprechen_items_json_url + authors_names[i],
			async: false,
			dataType: "json",
			success: function (data) {
				$.each(data, function (index, electionPromise) {
					election_promises.push(electionPromise);
				});
			},
			failure: function (response) {
				console.log(response);
				console.log("Failed to retrieve election promises.");
			}
		});
	}

	//ratings = getRatings();
	$.each(election_promises, function (index, electionPromise) {
		if (!(typeof electionPromise.ratings[0] === "undefined")) {
			if ($.inArray(electionPromise.ratings[0].rating, ratings_names) === -1) {
				ratings_names.push(electionPromise.ratings[0].rating);
			}
		}
	});

	buildAdditionalArrays();
	prepareChartData();

	//drawStackedBarChart();
	draw_electionPromises_parties_categories_basicBarChart();
	draw_electionPromises_ratings_categories_basicBarChart();
});

function buildAdditionalArrays() {
	//build 2-dimensional array to contain counts of topics of election promises by author
	for (var i = 0; i < categories.length; i++) {
		election_promises_by_category_counter.push([]);

		for (var k = 0; k < authors.length; k++) {
			election_promises_by_category_counter[i].push([]);
			election_promises_by_category_counter[i][k] = 0;
		}
	}

	//build 2-dimensional array to contain counts of ratings of election promises by author
	for (var i = 0; i < categories.length; i++) {
		election_promises_by_rating_counter.push([]);

		for (var k = 0; k < ratings_names.length; k++) {
			election_promises_by_rating_counter[i].push([]);
			election_promises_by_rating_counter[i][k] = 0;
		}
	}
}

function prepareChartData() {
	/*=======================================================================================================================
	This loop fills the arrays required for the chart displaying the election promises grouped by author grouped by category.
	=======================================================================================================================*/
	$.each(election_promises, function (index, election_promise) {
		election_promises_by_category_counter[categories_names.indexOf(election_promise.category)][authors_names.indexOf(election_promise.author)]++;
	});

	for(var i = 0; i < authors.length; i++) {
		
		election_promises_grouped_by_parties_grouped_by_categories_data = [];
		
		for (var n = 0; n < categories.length; n++) {
			election_promises_grouped_by_parties_grouped_by_categories_data.push(election_promises_by_category_counter[n][i]);
		}
		
		election_promises_grouped_by_parties_grouped_by_categories_series_values.push({
			name: authors_names[authors_names.indexOf(authors[i].name)],
			data: election_promises_grouped_by_parties_grouped_by_categories_data //order of values is the same as order of categories in the categories array
		});
	}
	
	/*=======================================================================================================================
	This loop fills the arrays required for the chart displaying the election promises grouped by rating grouped by category.
	=======================================================================================================================*/
	$.each(election_promises, function (index, election_promise) {
		if(!(typeof election_promise.ratings[0] === "undefined")) {
			election_promises_by_rating_counter[categories_names.indexOf(election_promise.category)][ratings_names.indexOf(election_promise.ratings[0].rating)]++;
		}
	});
	
	for(var i = 0; i < ratings_names.length; i++) {
		
		election_promises_grouped_by_ratings_grouped_by_categories_data = [];
		
		for (var n = 0; n < categories.length; n++) {
			election_promises_grouped_by_ratings_grouped_by_categories_data.push(election_promises_by_rating_counter[n][i]);
		}
		
		election_promises_grouped_by_ratings_grouped_by_categories_series_values.push({
			name: ratings_names[i],
			data: election_promises_grouped_by_ratings_grouped_by_categories_data //order of values is the same as order of categories in the categories array
		});
	}
}

//===================
//=====FUNCTIONS=====
//===================

function getAuthors() {

}

function getCategories() {

}

function getElectionPromises() {

}

function getRatings() {
	
}

function drawStackedBarChart() {
	$('#container1').highcharts({
		chart: {
			type: 'bar',
			height: 700,
			width: 700
		},
		title: {
			text: 'Election Promises (Parties, Categories)'
		},
		xAxis: {
			categories: categories_names
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
		series: election_promises_grouped_by_parties_grouped_by_categories_series_values
	});
}

function draw_electionPromises_parties_categories_basicBarChart() {
	$('#container1').highcharts({
		chart: {
			type: 'bar',
			height: 1000,
			width: 800
		},
		title: {
			text: 'Election Promises made by Parties grouped by Categories'
		},
		subtitle: {
			text: 'Source: Wahlversprechen2013.de'
		},
		xAxis: {
			categories: categories_names,
			title: {
				text: null
			}
		},
		yAxis: {
			min: 0,
			title: {
				text: 'Number of Election Promises',
				align: 'high'
			},
			labels: {
				overflow: 'justify'
			}
		},
		tooltip: {
			valueSuffix: ''
		},
		plotOptions: {
			bar: {
				dataLabels: {
					enabled: true
				},
				pointWidth: 10,
				minPointLength: 2
			}
		},
		legend: {
			layout: 'vertical',
			align: 'right',
			verticalAlign: 'top',
			x: -40,
			y: 100,
			floating: true,
			borderWidth: 1,
			backgroundColor: ((Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'),
			shadow: true
		},
		credits: {
			enabled: false
		},
		series: election_promises_grouped_by_parties_grouped_by_categories_series_values
	});
}

function draw_electionPromises_ratings_categories_basicBarChart() {
	$('#container2').highcharts({
		chart: {
			type: 'bar',
			height: 2000,
			width: 800
		},
		title: {
			text: 'Election Promises (Ratings, Categories)'
		},
		subtitle: {
			text: 'Source: Wahlversprechen2013.de'
		},
		xAxis: {
			categories: categories_names,
			title: {
				text: null
			}
		},
		yAxis: {
			min: 0,
			title: {
				text: 'Number of Election Promises',
				align: 'high'
			},
			labels: {
				overflow: 'justify'
			}
		},
		tooltip: {
			valueSuffix: ''
		},
		plotOptions: {
			bar: {
				dataLabels: {
					enabled: true
				},
				pointWidth: 10,
				minPointLength: 2
			}
		},
		legend: {
			layout: 'vertical',
			align: 'right',
			verticalAlign: 'top',
			x: -40,
			y: 100,
			floating: true,
			borderWidth: 1,
			backgroundColor: ((Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'),
			shadow: true
		},
		credits: {
			enabled: false
		},
		series: election_promises_grouped_by_ratings_grouped_by_categories_series_values
	});
}