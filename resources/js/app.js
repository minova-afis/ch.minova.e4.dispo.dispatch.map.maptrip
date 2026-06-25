(function () {
    var map;
    var markerOverlays = [];
    var routeOverlays = [];
    var currentDeliveries = [];
    var currentTrucks = [];
    var currentDepots = [];

    var DEFAULT_LAT = 49.78686;
    var DEFAULT_LON = 9.97996;
    var DEFAULT_MAP_TYPE = "tomtom-latest";

    var DELIVERY_ICON_URL = "assets/delivery.png";
    var TRUCK_ICON_URL = "assets/truck.svg";

    window.dispoMap = {
        clearMarkers: clearMarkers,
        setData: setData,
        setDeliveries: setDeliveries,
        setTrucks: setTrucks,
        setDepots: setDepots,
        centerOn: centerOn,
        showLocator: showLocator,
        showRoute: showRoute,
        clearRoute: clearRoute
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", createMap);
    } else {
        createMap();
    }

    function createMap() {
        var mapElement = document.getElementById("map");
        if (!mapElement || typeof IWMap === "undefined") {
            return;
        }

        map = new IWMap(mapElement);
        map.getOptions().setAutoResize(true);
        map.getOptions().setLanguage("de");
        map.getOptions().setAccentColor("rgb(0, 0, 0)");

        var coordinate = new IWCoordinate(DEFAULT_LON, DEFAULT_LAT, IWCoordinate.WGS84);
        var type = map.getOptions().getMapTypeByName(DEFAULT_MAP_TYPE);
        map.setCenter(coordinate, map.getZoom(), type);

        addMapTypeToolbar();
        renderMarkers();
        log("DISPO map initialized");
    }

    function addMapTypeToolbar() {
        if (!map || typeof IWIconManager === "undefined" || typeof IWSVGToolbarControl === "undefined") {
            return;
        }

        IWIconManager.initialize();

        var layer = map.getLayoutManager().getLayer(0);
        var toolbar = new IWSVGToolbarControl(map, { vertical: false });
        toolbar.addItem({ name: "tomtom-latest", icon: IWIconManager.createIcon("toolbar-roadmap"), tooltip: "Show Roadmap" });
        toolbar.addItem({ name: "air", icon: IWIconManager.createIcon("toolbar-air"), tooltip: "Show Aerial Map" });
        toolbar.addItem({ name: "hybrid", icon: IWIconManager.createIcon("toolbar-hybrid"), tooltip: "Show Hybrid Map" });
        layer.addControl(toolbar, IWAlignment.LEFT, IWAlignment.TOP, 0, 5);

        IWEventManager.addListener(toolbar, "onclick", function (event) {
            var type = map.getOptions().getMapTypeByName(event.item);
            map.setMapType(type);
        });
    }

    function clearMarkerOverlays() {
        markerOverlays.forEach(removeOverlay);
        markerOverlays = [];
    }

    function clearMarkers() {
        clearMarkerOverlays();
        currentDeliveries = [];
        currentTrucks = [];
        currentDepots = [];
    }

    function setData(data) {
        data = data || {};
        currentDeliveries = Array.isArray(data.deliveries) ? data.deliveries : [];
        currentTrucks = Array.isArray(data.trucks) ? data.trucks : [];
        currentDepots = Array.isArray(data.depots) ? data.depots : [];
        log("setData", currentDeliveries.length, currentTrucks.length, currentDepots.length);
        renderMarkers();
    }

    function setDeliveries(deliveries) {
        currentDeliveries = Array.isArray(deliveries) ? deliveries : [];
        log("setDeliveries", currentDeliveries.length);
        renderMarkers();
    }

    function setTrucks(trucks) {
        currentTrucks = Array.isArray(trucks) ? trucks : [];
        log("setTrucks", currentTrucks.length);
        renderMarkers();
    }

    function setDepots(depots) {
        currentDepots = Array.isArray(depots) ? depots : [];
        log("setDepots", currentDepots.length);
        renderMarkers();
    }

    function renderMarkers() {
        if (!map) {
            return;
        }

        clearMarkerOverlays();

        currentDeliveries.forEach(function (delivery) {
            addMarker(delivery, "delivery");
        });

        currentTrucks.forEach(function (truck) {
            addMarker(truck, "truck");
        });

        currentDepots.forEach(function (depot) {
            addMarker(depot, "depot");
        });
    }

    function addMarker(item, type) {
        var position = getPosition(item);
        if (!position) {
            return;
        }

        if (typeof IWMarker === "undefined") {
            return;
        }

        var marker = new IWMarker(map, new IWCoordinate(position.lon, position.lat, IWCoordinate.WGS84));
        var icon = getMarkerIcon(type);
        if (icon && typeof marker.setDefaultIcon === "function") {
            marker.setDefaultIcon(icon);
        }

        attachMarkerCallbacks(marker, item, type);
        addOverlay(marker);
        markerOverlays.push(marker);
    }

    function getMarkerIcon(type) {
        if (typeof IWIcon === "undefined" || typeof IWPoint === "undefined" || typeof IWSize === "undefined") {
            return null;
        }

        if (type === "truck") {
            return new IWIcon(TRUCK_ICON_URL, new IWPoint(12, 12), new IWSize(60, 60));
        }

        if (type === "delivery") {
            return new IWIcon(DELIVERY_ICON_URL, new IWPoint(12, 12), new IWSize(25, 25));
        }

        return null;
    }

    function getPosition(item) {
        if (!item) {
            return null;
        }

        var lat = firstNumber(item.lat, item.latitude, item.coordinateY, item.y);
        var lon = firstNumber(item.lon, item.lng, item.longitude, item.coordinateX, item.x);
        if (!isNumber(lat) || !isNumber(lon)) {
            return null;
        }

        return { lat: lat, lon: lon };
    }

    function firstNumber() {
        for (var i = 0; i < arguments.length; i++) {
            var value = arguments[i];
            if (isNumber(value)) {
                return value;
            }
        }
        return null;
    }

    function attachMarkerCallbacks(marker, item, type) {
        if (!marker || !item || typeof IWEventManager === "undefined") {
            return;
        }

        var id = getItemId(item);
        IWEventManager.addListener(marker, "onclick", function () {
            callJavaClick(type, id);
        });
        IWEventManager.addListener(marker, "ondblclick", function () {
            callJavaDoubleClick(type, id);
        });
    }

    function getItemId(item) {
        return firstDefined(item.id, item.key, item.tripKey, item.shipmentId, item.keyLong, item.keyText);
    }

    function firstDefined() {
        for (var i = 0; i < arguments.length; i++) {
            if (arguments[i] !== undefined && arguments[i] !== null) {
                return arguments[i];
            }
        }
        return "";
    }

    function callJavaClick(type, id) {
        var value = String(firstDefined(id, ""));
        if (type === "delivery" && typeof window.javaDeliveryClicked === "function") {
            window.javaDeliveryClicked(value);
        } else if (type === "truck" && typeof window.javaTruckClicked === "function") {
            window.javaTruckClicked(value);
        } else if (type === "depot" && typeof window.javaDepotClicked === "function") {
            window.javaDepotClicked(value);
        }
    }

    function callJavaDoubleClick(type, id) {
        var value = String(firstDefined(id, ""));
        if (type === "delivery" && typeof window.javaDeliveryDoubleClicked === "function") {
            window.javaDeliveryDoubleClicked(value);
        }
    }

    function addOverlay(overlay) {
        if (!overlay || !map) {
            return;
        }

        if (typeof overlay.getMap === "function" && overlay.getMap()) {
            return;
        }

        if (typeof overlay.setMap === "function") {
            overlay.setMap(map);
            return;
        }

        var overlayManager = map.getOverlayManager ? map.getOverlayManager() : null;
        if (overlayManager && typeof overlayManager.addOverlay === "function") {
            overlayManager.addOverlay(overlay);
        }
    }

    function centerOn(lat, lon) {
        if (!map || !isNumber(lat) || !isNumber(lon)) {
            return;
        }

        map.setCenter(new IWCoordinate(lon, lat, IWCoordinate.WGS84));
    }

    function showLocator(lat, lon) {
        centerOn(lat, lon);
        // TODO Add temporary locator marker once marker rendering is wired.
    }

    function showRoute(points) {
        clearRoute();
        if (!Array.isArray(points)) {
            return;
        }

        // Route rendering will be wired once Java passes normalized route points.
    }

    function clearRoute() {
        routeOverlays.forEach(removeOverlay);
        routeOverlays = [];
    }

    function removeOverlay(overlay) {
        if (!overlay) {
            return;
        }

        var overlayManager = map && map.getOverlayManager ? map.getOverlayManager() : null;
        if (typeof overlay.remove === "function") {
            overlay.remove();
        } else if (overlayManager && typeof overlayManager.removeOverlay === "function") {
            overlayManager.removeOverlay(overlay);
        }
    }

    function log() {
        if (window.console && typeof window.console.log === "function") {
            window.console.log.apply(window.console, arguments);
        }
    }
    function isNumber(value) {
        return typeof value === "number" && isFinite(value);
    }
}());