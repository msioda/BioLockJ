if(typeof(EventSource) !== "undefined") {
	console.log('EventSource Works');
		// Yes! Server-sent events support!
    // Some code.....
	var StreamLog = new EventSource("/streamLog",{ withCredentials: true });
	StreamLog.addEventListener("message", function(e) {
	    console.log(e.data);
	}, false);

	StreamLog.addEventListener("open", function(e) {
	    console.log("StreamLog connection was opened.");
	}, false);

	StreamLog.addEventListener("error", function(e) {
	    console.log("Error - connection was lost.");
	}, false);
	StreamLog.onmessage = function(event) {
		document.getElementById("log").innerHTML += event.data + "<br>";
	};

	//for getting progress from blj
	var streamProgress = new EventSource("/streamProgress",{ withCredentials: true });
	streamProgress.addEventListener("message", function(e) {
	    console.log(e.data);
	}, false);

	streamProgress.addEventListener("open", function(e) {
	    console.log("streamprogress connection was opened.");
	}, false);

	streamProgress.addEventListener("error", function(e) {
	    console.log("Error - connection was lost.");
	}, false);
	streamProgress.onmessage = function(event) {
		console.log('onmessage fired');
		console.log(event);
	//     //document.getElementById("result").innerHTML += event.data + "<br>";
	};
	var streamProgress = new EventSource("/streamProgress",{ withCredentials: true });
	streamProgress.addEventListener("message", function(e) {
	    console.log(e.data);
	}, false);

	streamProgress.addEventListener("open", function(e) {
	    console.log("streamprogress connection was opened.");
	}, false);

	streamProgress.addEventListener("error", function(e) {
	    console.log("Error - connection was lost.");
	}, false);
	streamProgress.onmessage = function(event) {
		console.log('onmessage fired');
		console.log(event);
	//     //document.getElementById("result").innerHTML += event.data + "<br>";
	};

} else {
	console.log('nope');
    // Sorry! No server-sent events support..
}
