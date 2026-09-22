/**
 * Carta Nova GPS Tracker Extension for Leaflet
 * Handles high-priority GPS layer management and zoom anchoring.
 */
function initMapGpsTracker(mapInstance, mapBounds) {
    // 1. Create a dedicated top pane for this map instance
    mapInstance.createPane('gpsMarkerPane');
    mapInstance.getPane('gpsMarkerPane').style.zIndex = 650; 
    mapInstance.getPane('gpsMarkerPane').style.pointerEvents = 'none'; 

    var gpsZooming = false;
    var pendingGpsLocation = null;
    var CartaNovaGpsMarker = null;

    function applyGpsLocation(latitude, longitude) {
        // Define the custom HTML element for the marker
        if (!CartaNovaGpsMarker) {
            // Define the custom HTML element for the marker
            var pulsingIcon = L.divIcon({
                className: '', // Clear Leaflet's default layout classes
                html: '<div class="gps-pulse-icon"></div>',
                iconSize: [14, 14],      // Exact width and height matching CSS core
                iconAnchor: [7, 7]       // Keeps the center anchor perfectly aligned with coordinates
            });

            // L.marker handles standard icons, but we map it to our custom zIndex pane
            CartaNovaGpsMarker = L.marker([0, 0], {
                pane: 'gpsMarkerPane',
                icon: pulsingIcon
            });
        }

        // Validate boundary against the current map's bounds configuration
        var inside = latitude >= mapBounds[0][0] && latitude <= mapBounds[1][0] &&
                     longitude >= mapBounds[0][1] && longitude <= mapBounds[1][1];

        if (inside) {
            CartaNovaGpsMarker.setLatLng([latitude, longitude]);
            if (!mapInstance.hasLayer(CartaNovaGpsMarker)) {
                CartaNovaGpsMarker.addTo(mapInstance);
            }
        } else if (mapInstance.hasLayer(CartaNovaGpsMarker)) {
            mapInstance.removeLayer(CartaNovaGpsMarker);
        }
    }

    // Attach map listeners directly to this instance
    mapInstance.on('zoomstart', function () {
        gpsZooming = true;
    });

    mapInstance.on('zoomend', function () {
        gpsZooming = false;
        if (pendingGpsLocation) {
            var location = pendingGpsLocation;
            pendingGpsLocation = null;
            applyGpsLocation(location.latitude, location.longitude);
        }
    });

    // Return a clean public execution handle to global scope
    return {
        updatePosition: function(latitude, longitude) {
            if (gpsZooming) {
                pendingGpsLocation = { latitude: latitude, longitude: longitude };
                return;
            }
            applyGpsLocation(latitude, longitude);
        }
    };
}
