// Globals: worldFolderName, worldOrigin, worldMarkers


// Setup map
function setupMap(id) {
    var map = {};

    map.leafletMap = L.map(id, {
        crs: L.CRS.Simple,
        zoomControl: false,
        zoom: -1,
        center: coordsToLatLong(worldOrigin[0], worldOrigin[1])
    });
    L.tileLayer('/images/' + worldFolderName + '/zoom{z}/r.{x}.{y}.png', {
        minZoom: -1,
        maxZoom: -1,
        attribution: '@PROJECT_NAME@',
        tms: false,
        noWrap: true,
        continuousWorld: true
    }).addTo(map.leafletMap);

    addMarkers(map, worldMarkers);

    // Start fetching players
    map.players = [];
    window.addEventListener("focus", function() { startUpdating(map) });
    startUpdating(map);
}

function addMarkers(map, worldMarkers) {
    for (var i = 0; i < worldMarkers.length; i++) {
        var markerJson = worldMarkers[i];
        var method = L[markerJson.method];
        var marker = markerJson.secondParam === undefined?
                        method(markerJson.firstParam) :
                        method(markerJson.firstParam, markerJson.secondParam);
        if (markerJson.tooltip !== undefined) {
            marker.bindPopup(markerJson.tooltip);
        }
        marker.addTo(map.leafletMap);
    }
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
    for (var i = 0; i < map.players.length; i++) {
        var oldMarker = map.players[i].marker;
        map.leafletMap.removeLayer(oldMarker);
    }
    map.players = [];
}
function findPlayerIndex(name, playerList) {
    for (var i = 0; i < playerList.length; i++) {
        if (playerList[i].name === name) {
            return i;
        }
    }
    return -1;
}
function displayPlayers(map, newPlayers) {
    // (Re)move old players
    var oldPlayers = map.players.slice();
    for (var i = 0; i < oldPlayers.length; i++) {
        var oldPlayer = oldPlayers[i];
        var newPlayerIndex = findPlayerIndex(oldPlayer.name, newPlayers);
        if (newPlayerIndex !== -1) {
            // Move on map, remove from the new players list
            var newPlayer = newPlayers[newPlayerIndex];
            oldPlayer.x = newPlayer.x;
            oldPlayer.y = newPlayer.y;
            oldPlayer.marker.setLatLng(coordsToLatLong(newPlayer.x, newPlayer.z));
            newPlayers.splice(newPlayerIndex, 1);
        } else {
            // Remove from map
            map.leafletMap.removeLayer(oldPlayer.marker);
            map.players.splice(i, 1);
        }
    }

    // Add the actually new players
    for (var i = 0; i < newPlayers.length; i++) {
        var player = newPlayers[i];
        playerMarker = L.marker(coordsToLatLong(player.x, player.z), {
            icon: createPlayerIcon(player.name)
        });
        playerMarker.addTo(map.leafletMap);
        playerMarker.bindPopup(player.name);
        player.marker = playerMarker
        map.players.push(player);
    }
}
function createPlayerIcon(playerName) {
    return L.icon({
        iconUrl: 'https://cravatar.eu/helmavatar/' + encodeURIComponent(playerName) + '/16',
        iconSize: [16, 16],
        iconAnchor: [8, 8],
        popupAnchor: [0, -8]
    });
}
function coordsToLatLong(x, z) {
    return [-z, x]
}

setupMap('map__area');


