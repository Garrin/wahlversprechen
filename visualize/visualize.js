$(document).ready(function() {

	getAuthors();

	//drawHorizontalBarChart();

	// Sortierte Versprechen nach Parteien und Kategorien
	
	/*
	
	Available JSON API Requests:

	/json/tags
	/json/categories
	/json/authors
	/json/items/{author}
	/json/item/{id}
	/json/item/{id}/relatedurls?[limit=x&offset=y]

	*/
});


var gotCategories = false;
var gotAuthors = false;
var gotPromises = false;

var categories = [];
var authors = [];
var promises = [];

var columns = [];
var itemsPerCategory = [];

function drawC3BarChart() {

	for(var i = 0; i < authors.length; i++) {
		console.log(
			_.countBy( promises, function(promise) { return promise.category; })
		);

		itemsPerCategory.push(
			_.countBy(promises, function(promise) { return promise.category; } )
		);
	}

	/*
	_.countBy([1, 2, 3, 4, 5], function(num) {
		return num % 2 == 0 ? 'even': 'odd';
	});

	=> {odd: 3, even: 2}
	*/


	//console.log(promises);

	var chart = c3.generate({
		data: {
			columns: [
				['Koalitionsvertrag', 30, 200, 100, 400, 150, 250, 1],
				['SPD', 130, 100, 140, 200, 150, 50, 1],
				['CDU', 130, 100, 140, 200, 150, 50, 1],
				['CDU', 130, 100, 140, 200, 150, 50, 1]
			],
			type: 'bar'
		},
		bar: {
			width: {
				ratio: 0.75 // this makes bar width 50% of length between ticks enter absolute number for px value
			}
		},

		axis: {
			x: {
				type: 'category',
				categories: categories
			}
		}

	});
}


//Returns an array containing all authors with their promises
function getPromisesByAuthor() {
	
	//Get all the authors
	var authorNames = [];

	$.getJSON("http://www.wahlversprechen2013.de/json/authors", function(data) {
		$.each(data, function(key, val) {
			//console.log("adding author name:" + val["name"]);
			authorNames.push(val["name"]);
		});

		getPromises(authorNames);
	});
}

function getPromises(authorNames) {
	//console.log("authorNames:" + authorNames);
	for(var i = 0; i < authorNames.length; i++) {
		//console.log(authorNames[i]);
		$.getJSON(encodeURI("http://www.wahlversprechen2013.de/json/items/"+authorNames[i]), function(data) {
			$.each(data, function(key, val) {
				//console.log(val);
				promises.push(val);
			});

			drawC3BarChart(); //does it multiple times
		});
	}
}

/*
function getCategories() {
	$.getJSON("http://www.wahlversprechen2013.de/json/categories", function(data) {
		$.each(data, function(key, val) {
			categories.push(val.name);
		});

		getAuthors();
	});
}
*/

function getAuthors() {
	$.getJSON("http://www.wahlversprechen2013.de/json/authors", function(data) {
		$.each(data, function(key, val) {
			authors.push(val);
		});

		getPromisesByAuthor();
	});
}







function drawHorizontalBarChart() {
	console.log("Hello World!");

	//JSON request example
	/*
	$.getJSON("http://www.wahlversprechen2013.de/json/tags", function(data) {
		var items = [];
		
		$.each(data, function(key, val) {
			items.push("<li id='" + key + "'>ID:" + val["id"] + "Name: " + val["name"] + "</li>");
		});
		
		$("<ul/>", {
			"class": "my-new-list",
			html: items.join( "" )
		}).appendTo( "body" );
	});
	*/

	//TUTORIAL START: bar chart
	//var data = [4, 8, 15, 16, 23, 42];
/*
	var x = d3.scale.linear()
		.domain([0, d3.max(data)])
		.range([0, 420]);

	d3.select(".bar_chart")
		.selectAll("div")
			.data(data)
		.enter().append("div")
			.style("width", function(d) { return x(d) + "px"; })
			.text(function(d) { return d; });
			*/
	//TUTORIAL END


	//bar chart for tags with their starting letter
	var startingLetters = [	"A","B","C","D","E",
							"F","G","H","I","J",
							"K","L","M","N","O",
							"P","Q","R","S","T",
							"U","V","W","X","Y",
							"Z"
	];

	var numberOfTags = [0,0,0,0,0,
						0,0,0,0,0,
						0,0,0,0,0,
						0,0,0,0,0,
						0,0,0,0,0,
						0];
	
	$.getJSON("http://www.wahlversprechen2013.de/json/tags", function(data) {

		// for all tags
		$.each(data, function(key, val) {
			for(var i = 0; i < startingLetters.length; i++) {
				if(val["name"].charAt(0).toUpperCase() === startingLetters[i].toUpperCase()) {
					numberOfTags[i]++;
				}
			}
		});

		//filter array elements, 0 elements are not included anymore
		/*
		numberOfTags = numberOfTags.filter(function(elem, index) {
			if(elem > 0) {
				return true;	
			} else {
				console.log("startingLetters BEFORE:" + startingLetters);
				console.log("Removing an element...");
				startingLetters.splice(index, 1);
				return false;
			}
		});
		*/

		//console.log("startingLetters:"+startingLetters);
		//console.log("FILTERED ARRAY:" + numberOfTags);

		//enter values
		d3.select(".bar_chart")
			.selectAll("div")
				.data(numberOfTags)
			.enter().append("div")
				.style("width", cssWidthForChart)
				.text(
					function(value, index) {return startingLetters[index]+":"+value;}
				);
	});

	
	//draw the chart
	var minBarWidth = 20;
	var maxBarWidth = 1000;
	
	//text function
	function cssWidthForChart(d) {
		return calculateWidth(d) + "px";
	}

	//value function
	function calculateWidth(d) {
		var calculate = d3.scale.linear()
			.domain([0, d3.max(numberOfTags)])
			.range([minBarWidth, maxBarWidth]);

		return calculate(d);
	}
}