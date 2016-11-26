<html>
    <body>
        <h3>type your script here: </h3>
		<input type="button" id="eval_id" value="eval">
		<h3>.</h3>
		<textarea name="script_form" id="script_form" rows="50" cols="100">
var http = new XMLHttpRequest();
var url = "/mafia-game";
http.open("POST", url, true);

//Send the proper header information along with the request
http.setRequestHeader("Content-type", "request header");
console.log("sending request");
http.onreadystatechange = function() {//Call a function when the state changes.
    if(http.readyState == 4 && http.status == 200) {
        console.log("response: " + http.responseText);
    }
}
http.send("");
		</textarea>
		<script>
		function eval_script() {
		  eval(document.getElementById("script_form").value);
		}
		document.getElementById("eval_id").onclick = function() {
			eval_script();
		};
		</script>
    </body>
</html>