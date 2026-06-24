# ch.minova.e4.dispo.dispatch.map.maptrip

Neue MapTrip-basierte Kartenimplementierung für DISPO.

Dieses Repository basiert auf der bisherigen DISPO-Kartenimplementierung, ersetzt die alte Kartendarstellung jedoch schrittweise durch die MapTrip JavaScript API innerhalb eines SWT Browser Widgets.

## Ziel

Die alte Karte verwendet bisher:

```text
SWT Canvas
MapViewPointController
MapImageLoader
MapServer GetMap Images
SWT GC Rendering
```

Diese Implementierung soll ersetzt werden durch:

```text
SWT Browser
lokale HTML/JavaScript-Ressourcen
MapTrip JavaScript API
Marker, Cluster, Tooltips und Routen direkt in JavaScript
Java <-> JavaScript Kommunikation
```

Das Projekt ist kein reines Demo-Projekt. Es soll eine DISPO-kompatible neue Kartenimplementierung entstehen, die später die alte Karte ersetzen kann.

## Architektur

Die bestehende DISPO UI arbeitet über das Interface:

```java
IMapControl
```

Deshalb soll die neue Karte weiterhin als `IMapControl` bereitgestellt werden.

Geplante Struktur:

```text
MapControlPart
  -> IMapControl
  -> MapTripMapControl / BrowserMapControl
  -> SWT Browser
  -> resources/index.html
  -> resources/js/app.js
  -> MapTrip JavaScript API
```

Die alte DISPO-Integration bleibt dadurch möglichst stabil.

## Verantwortlichkeiten

### Java-Seite

Java bleibt verantwortlich für:

```text
DISPO-Datenmodell
Geocoding
Routing
Optimierung
Koordinatenumrechnung
Filterlogik
Selection-Handling
```

Die bereits vorhandene Backend-Implementierung über die MapTrip Server API soll dafür genutzt werden.

### JavaScript-Seite

JavaScript ist verantwortlich für:

```text
Kartendarstellung
Marker
Cluster
Tooltips / Popups
Routenanzeige
Interaktion mit der Karte
Marker-Click-Events
```

JavaScript soll keine direkten Requests an lokale Test-Endpunkte wie `localhost:8080` senden.

## Ressourcen

Die MapTrip-Karte wird über lokale Ressourcen geladen:

```text
resources/
  index.html
  css/
    style.css
  js/
    app.js
  assets/
    truck.svg
    delivery.png
    depot.png
```

Diese Ressourcen müssen in `build.properties` enthalten sein.

Beispiel:

```properties
bin.includes = META-INF/,\
               .,\
               OSGI-INF/,\
               resources/
```

## Java nach JavaScript

Java ruft JavaScript über `Browser.execute(...)` auf.

Beispiele:

```java
browser.execute("window.dispoMap.setDeliveries(" + deliveriesJson + ");");
browser.execute("window.dispoMap.setTrucks(" + trucksJson + ");");
browser.execute("window.dispoMap.setDepots(" + depotsJson + ");");
browser.execute("window.dispoMap.centerOn(" + lat + "," + lon + ");");
```

Die JavaScript-Seite stellt dafür eine zentrale API bereit:

```javascript
window.dispoMap = {
  setDeliveries(deliveries) {},
  setTrucks(trucks) {},
  setDepots(depots) {},
  clearMarkers() {},
  centerOn(lat, lon) {},
  showLocator(lat, lon) {},
  showRoute(points) {},
  clearRoute() {}
};
```

## JavaScript nach Java

JavaScript meldet Ereignisse über `BrowserFunction` zurück an Java.

Geplante Callbacks:

```text
javaDeliveryClicked(id)
javaDeliveryDoubleClicked(id)
javaTruckClicked(id)
javaDepotClicked(id)
javaMapClicked(lat, lon)
```

Diese Callbacks sollen in Java wieder auf die bestehende DISPO-Selection-Logik abgebildet werden.

## Koordinaten

DISPO arbeitet teilweise mit Mercator-Koordinaten.

Für die Anzeige in der MapTrip JavaScript API sollen vorzugsweise WGS84-Koordinaten an JavaScript übergeben werden.

Regel:

```text
WGS84 coordinateX = longitude
WGS84 coordinateY = latitude
```

Beispiel für Marker-Daten:

```json
{
  "id": "123",
  "type": "delivery",
  "lat": 49.78686,
  "lon": 9.97996,
  "tooltip": "Lieferung 123"
}
```

## Migrationsplan

Empfohlene Reihenfolge:

```text
1. Projekt kopieren und umbenennen.
2. Bundle-/Artifact-/Package-Namen anpassen.
3. Neue Browser-basierte MapControl-Implementierung hinzufügen.
4. resources/index.html über FileLocator laden.
5. Leere MapTrip-Karte anzeigen.
6. Java -> JavaScript Bridge implementieren.
7. Lieferungen als Marker darstellen.
8. Fahrzeuge und Depots darstellen.
9. Tooltips / Popups ergänzen.
10. JavaScript -> Java Callbacks implementieren.
11. Zentrieren und Locator-Funktionen implementieren.
12. Clustering aktivieren.
13. Routen / Trips als Shape oder Polyline darstellen.
14. Alte Canvas-/MapServer-Logik entfernen.
```

## Wichtige Hinweise

Die alte Canvas-basierte Implementierung soll nicht sofort vollständig gelöscht werden, solange sie noch als Referenz benötigt wird.

Am Ende soll die neue Karte jedoch nicht mehr abhängig sein von:

```text
SWT Canvas Rendering
MapServer GetMap Images
MapImageLoader
PaintListener Rendering
SWT GC Drawing
pixelbasierter Marker-Suche
```

Falls alte Methoden aus `IMapControl` noch benötigt werden, können sie während der Migration zunächst als sichere Kompatibilitätsmethoden implementiert werden. Sie sollen jedoch keine ungefangenen Exceptions werfen, wenn sie vom DISPO UI aufgerufen werden.

## Definition of Done

Die neue Kartenimplementierung gilt als evaluierbar, wenn:

```text
1. Die DISPO Map View öffnet sich mit SWT Browser.
2. Die MapTrip JavaScript API Karte wird angezeigt.
3. Die alte MapServer-Bilddarstellung wird nicht mehr verwendet.
4. Lieferungen werden als Marker angezeigt.
5. Fahrzeuge werden als Marker angezeigt.
6. Depots/Tankstellen werden als Marker angezeigt.
7. Tooltips oder Popups funktionieren.
8. Klicks auf Marker werden an Java zurückgemeldet.
9. Die bestehende DISPO-Selection-Logik kann weiterverwendet werden.
10. Zoom und Pan funktionieren über die MapTrip Karte.
11. Clustering funktioniert bei vielen Punkten.
12. Routen oder Trips können auf der Karte dargestellt werden.
```

## Abgrenzung

Nicht Teil des ersten Umsetzungsschritts:

```text
vollständige Entfernung aller alten Interfaces
kompletter Umbau von MapControlPart
Verlagerung von Routing/Geocoding in JavaScript
produktive Aktivierung ohne vorherige Evaluation
```

Zuerst soll eine stabile neue Implementierung entstehen, die sich wie ein normales `IMapControl` in DISPO integrieren lässt.
