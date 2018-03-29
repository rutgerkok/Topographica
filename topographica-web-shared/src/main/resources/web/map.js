// Globals: worldFolderName, 


// Setup map
function setupMap(id) {
	var map = {};

	map.leafletMap = L.map(id, {
	    crs: L.CRS.Simple,
	    zoomControl: false
	}).setView([0, 0], -1);
	L.tileLayer('/images/' + worldFolderName + '/r.{x}.{y}.jpg', {
	    minZoom: -1,
	    maxZoom: -1,
	    attribution: '@PROJECT_NAME@',
	    tms: false,
	    noWrap: true,
	    continuousWorld: true
	}).addTo(map.leafletMap);
	
	// Start fetching players
	map.playerMarkers = [];
	window.addEventListener("focus", function() { startUpdating(map) });
	startUpdating(map);
}

function startUpdating(map) { // Also works for restarting updating
	if (map.updateInterval !== undefined) {
		clearInterval(map.updateInterval);
	}
	map.updateInterval = setInterval(function() {
		 fetchPlayers(map);
	}, 10 * 1000);
	fetchPlayers(map);
}

// Load players
function fetchPlayers(map) {
    if (!document.hasFocus()) {
        return;
    }
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
	        if (this.status == 200) {
	            displayPlayers(map, JSON.parse(this.responseText));
	        } else {
	        	clearDisplayedPlayers(map);
	        }
	    }
    };
    xhttp.open("GET",
               "players.json?world=" + encodeURIComponent(worldFolderName),
               true);
    xhttp.send(); 
}
function clearDisplayedPlayers(map) {
	for (var i = 0; i < map.playerMarkers.length; i++) {
    	var oldMarker = map.playerMarkers[i];
    	map.leafletMap.removeLayer(oldMarker);
    }
    map.playerMarkers = [];
}
function displayPlayers(map, players) {
	clearDisplayedPlayers(map);

	for (var i = 0; i < players.length; i++) {
		var player = players[i];
		playerMarker = L.marker(coordsToLatLong(player.x, player.z));
		playerMarker.addTo(map.leafletMap);
		map.playerMarkers.push(playerMarker);
	}
}
function coordsToLatLong(x, z) {
	return [-z, x]
}

setupMap('map__area');


