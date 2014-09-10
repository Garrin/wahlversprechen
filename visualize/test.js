$(document).ready(function() {
	



	console.log("Hello World!");

	//JSON request example
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

	//Tutorial bar chart
	var data = [4, 8, 15, 16, 23, 42];

	var x = d3.scale.linear().domain([0, d3.max(data)]).range([0, 420]);

	d3.select(".bar_chart")
		.selectAll("div")
			.data(data)
		.enter().append("div")
			.style("width", function(d) { return x(d) + "px"; })
			.text(function(d) { return d; });
});