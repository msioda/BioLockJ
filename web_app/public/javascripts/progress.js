if(typeof(EventSource) !== "undefined") {
	console.log('EventSource Works');
		// Yes! Server-sent events support!
    // Some code.....
	var source = new EventSource("/streamprogress",{ withCredentials: true });
	source.addEventListener("message", function(e) {
	    console.log(e.data);
	}, false);

	source.addEventListener("open", function(e) {
	    console.log("streamprogress connection was opened.");
	}, false);

	source.addEventListener("error", function(e) {
	    console.log("Error - connection was lost.");
	}, false);
	source.onmessage = function(event) {
		console.log('onmessage fired');
		console.log(event);
	//     //document.getElementById("result").innerHTML += event.data + "<br>";
	};

} else {
	console.log('nope');
    // Sorry! No server-sent events support..
}
