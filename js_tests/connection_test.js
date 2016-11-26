var http = new XMLHttpRequest();
var url = "/resurrection-game";
http.open("POST", url, true);

//Send the proper header information along with the request
http.setRequestHeader("Content-type", "request header");
console.log("sending connection request");
http.onreadystatechange = function() {//Call a function when the state changes.
    if(http.readyState == 4 && http.status == 200) {
        console.log("response: " + http.responseText);
    }
}

var requestBody = {
	type : "Connection",
	id : "1",
	velX : "0",
	velY : "0",
	x : "0",
	y : "0"
}


http.send(JSON.stringify(requestBody));