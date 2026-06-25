package ch.minova.e4.dispo.dispatch.map.maptrip.internal;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.services.translation.TranslationService;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import ch.minova.dispo.dispatch.map.beans.IAddress;
import ch.minova.dispo.dispatch.map.beans.ICoordinates;
import ch.minova.dispo.dispatch.map.beans.IGeocodableAddress;
import ch.minova.dispo.dispatch.map.beans.IGeocodedWithSource;
import ch.minova.dispo.dispatch.map.core.ICalculatedTripInformation;
import ch.minova.dispo.dispatch.model.IGeocoded;
import ch.minova.dispo.dispatch.model.dto.Trip;
import ch.minova.e4.dispo.dispatch.map.beans.GeocodedImageProvider;
import ch.minova.e4.dispo.dispatch.map.control.MapViewPointController;
import ch.minova.e4.dispo.dispatch.map.ui.IDepotPosition;
import ch.minova.e4.dispo.dispatch.map.ui.IImageFilter;
import ch.minova.e4.dispo.dispatch.map.ui.IMapControl;
import ch.minova.e4.dispo.dispatch.map.ui.IMapSelectionListener;
import ch.minova.e4.dispo.dispatch.map.ui.ITruckPosition;

public class MapTripMapControl implements IMapControl {
	private static final double EARTH_RADIUS = 6378137.0;
	private static final double MAX_LATITUDE = 85.05112878;

	private Composite parent;
	private Browser browser;
	private boolean browserReady;
	private boolean browserFunctionsRegistered;
	private boolean inSearchMode;
	private boolean inGeoCodeMode;
	private IMapSelectionListener mapSelectionListener;
	private IImageFilter imageFilter;
	@Inject
	private UISynchronize uiSynchronize;
	private List<IGeocoded> images = new ArrayList<IGeocoded>();
	private List<ITruckPosition> trucks = new ArrayList<ITruckPosition>();
	private List<IDepotPosition> depots = new ArrayList<IDepotPosition>();

	@Override
	public void createMap(Composite parentComposite, TranslationService translationService) {
		createMap(parentComposite, translationService, true);
	}

	@Override
	public void createMap(Composite parentComposite, TranslationService translationService, boolean extractable) {
		createMap(parentComposite, translationService, extractable, true);
	}

	@Override
	public void createMap(Composite parentComposite, TranslationService translationService, boolean extractable, boolean showToolBar) {
		this.parent = new Composite(parentComposite, SWT.NONE);
		this.parent.setLayout(new FillLayout());

		try {
			this.browser = new Browser(this.parent, SWT.EDGE);
		} catch (SWTError e) {
			this.browser = new Browser(this.parent, SWT.NONE);
		}
		registerBrowserFunctions();
		this.browser.addProgressListener(new ProgressAdapter() {
			@Override
			public void completed(ProgressEvent event) {
				browserReady = true;
				redrawMap(true);
			}
		});

		Bundle bundle = FrameworkUtil.getBundle(getClass());
		URL url = FileLocator.find(bundle, new Path("resources/index.html"), null);
		if (url == null) {
			throw new IllegalStateException("Could not find MapTrip index.html");
		}

		try {
			URL fileUrl = FileLocator.toFileURL(url);
			browser.setUrl(fileUrl.toString());
		} catch (IOException e) {
			throw new IllegalStateException("Could not load MapTrip index.html", e);
		}
	}

	private void registerBrowserFunctions() {
		if (browserFunctionsRegistered || browser == null || browser.isDisposed()) {
			return;
		}
		new MapTripBrowserFunction("javaDeliveryClicked");
		new MapTripBrowserFunction("javaDeliveryClick");
		new MapTripBrowserFunction("javaDeliveryDoubleClicked");
		new MapTripBrowserFunction("javaTruckClicked");
		new MapTripBrowserFunction("javaTruckClick");
		new MapTripBrowserFunction("javaDepotClicked");
		new MapTripBrowserFunction("javaMapClicked");
		browserFunctionsRegistered = true;
	}

	private boolean canExecuteJavaScript() {
		return browserReady && browser != null && !browser.isDisposed();
	}

	private void executeJavaScript(final String script) {
		if (!canExecuteJavaScript()) {
			return;
		}

		Runnable command = new Runnable() {
			@Override
			public void run() {
				if (canExecuteJavaScript()) {
					browser.execute(script);
				}
			}
		};

		if (uiSynchronize != null) {
			uiSynchronize.asyncExec(command);
		} else if (browser.getDisplay() != null && !browser.getDisplay().isDisposed()) {
			browser.getDisplay().asyncExec(command);
		}
	}

	private class MapTripBrowserFunction extends BrowserFunction {
		private final String name;

		MapTripBrowserFunction(String name) {
			super(browser, name);
			this.name = name;
		}

		@Override
		public Object function(Object[] arguments) {
			System.out.println("MapTrip callback: " + name + " " + Arrays.toString(arguments));
			return null;
		}
	}

	@Override
	public void init() {
		// The Browser loads resources/index.html asynchronously in createMap(...).
	}

	@Override
	public boolean reparentMap(Composite parent) {
		if (this.parent == null || this.parent.isDisposed()) {
			return false;
		}
		return this.parent.setParent(parent);
	}

	@Override
	public Canvas getMap() {
		return null;
	}

	@Override
	public Composite getParent() {
		return parent;
	}

	@Override
	public void dispose() {
		if (browser != null && !browser.isDisposed()) {
			browser.dispose();
		}
		if (parent != null && !parent.isDisposed()) {
			parent.dispose();
		}
	}

	@Override
	public boolean centerOnMercator(int mercatorX, int mercatorY, boolean showCenter) {
		if (!canExecuteJavaScript()) {
			return false;
		}

		ICoordinates wgs84 = toWGS84(mercatorX, mercatorY);
		if (wgs84 == null) {
			return false;
		}

		double lon = wgs84.getCoordinateX();
		double lat = wgs84.getCoordinateY();

		if (!isFinite(lat) || !isFinite(lon)) {
			return false;
		}

		String functionName = showCenter ? "showLocator" : "centerOn";
		executeJavaScript("window.dispoMap && window.dispoMap." + functionName + " && window.dispoMap." + functionName + "(" + lat + "," + lon + ");");
		return true;
	}

	@Override
	public Point getTopLeft() {
		return new Point(0, 0);
	}

	@Override
	public Point getBottomRight() {
		return new Point(0, 0);
	}

	// Temporary local WebMercator fallback for the frontend prototype.
	// Replace this with IProjectionsController once the MapTrip backend provider is
	// available in the target DISPO runtime.
	@Override
	public ICoordinates toWGS84(IGeocoded geo) {
		return geo == null ? null : toWGS84(geo.getMercatorX(), geo.getMercatorY());
	}

	@Override
	public ICoordinates toWGS84(int mercatorX, int mercatorY) {
		final double longitude = Math.toDegrees(mercatorX / EARTH_RADIUS);
		final double latitude = Math.toDegrees(2.0 * Math.atan(Math.exp(mercatorY / EARTH_RADIUS)) - Math.PI / 2.0);

		return new ICoordinates() {
			@Override
			public double getCoordinateX() {
				return longitude;
			}

			@Override
			public double getCoordinateY() {
				return latitude;
			}
		};
	}

	@Override
	public IGeocoded toMercator(double wgs84x, double wgs84y) {
		final int mercatorX = (int) Math.round(EARTH_RADIUS * Math.toRadians(wgs84x));
		final int mercatorY = (int) Math.round(EARTH_RADIUS * Math.log(Math.tan(Math.PI / 4.0 + Math.toRadians(clampLatitude(wgs84y)) / 2.0)));

		return new IGeocoded() {
			@Override
			public int getMercatorX() {
				return mercatorX;
			}

			@Override
			public int getMercatorY() {
				return mercatorY;
			}
		};
	}

	private static double clampLatitude(double latitude) {
		if (latitude > MAX_LATITUDE) {
			return MAX_LATITUDE;
		}
		if (latitude < -MAX_LATITUDE) {
			return -MAX_LATITUDE;
		}
		return latitude;
	}

	@Override
	public void addImage(GeocodedImageProvider imageProvider) {
		if (imageProvider != null) {
			images.add(imageProvider);
			redrawMap(true);
		}
	}

	@Override
	public void clearImages() {
		images.clear();
		redrawMap(true);
	}

	@Override
	public void setImages(List<IGeocoded> images) {
		this.images = images == null ? new ArrayList<IGeocoded>() : new ArrayList<IGeocoded>(images);
		redrawMap(true);
	}

	@Override
	public void setTrucks(List<ITruckPosition> truck) {
		this.trucks = truck == null ? new ArrayList<ITruckPosition>() : new ArrayList<ITruckPosition>(truck);
		redrawMap(true);
	}

	@Override
	public void redraw() {
		redrawMap(false);
	}

	@Override
	public void redraw(String trip) {
		redrawMap(false);
	}

	@Override
	public void redrawMapImages(boolean changed) {
		redrawMap(changed);
	}

	@Override
	public void redrawMap(boolean changed) {
		redrawMap(changed, false);
	}

	@Override
	public void redrawMap(boolean changed, boolean async) {
		if (!canExecuteJavaScript()) {
			return;
		}

		executeJavaScript("window.dispoMap && window.dispoMap.setData && window.dispoMap.setData({"
				+ "\"deliveries\":" + toDeliveryJson(images)
				+ ",\"trucks\":" + toTruckJson(trucks)
				+ ",\"depots\":" + toDepotJson(depots)
				+ "});");
	}

	private String toMarkerJson(IGeocoded item, String type, String id, String tooltip) {
		if (item == null) {
			return null;
		}

		ICoordinates wgs84 = toWGS84(item);
		if (wgs84 == null) {
			return null;
		}

		double lon = wgs84.getCoordinateX();
		double lat = wgs84.getCoordinateY();

		if (!isFinite(lat) || !isFinite(lon)) {
			return null;
		}

		StringBuilder json = new StringBuilder("{");
		json.append("\"id\":").append(quoteJson(id));
		json.append(",\"type\":").append(quoteJson(type));
		json.append(",\"lat\":").append(lat);
		json.append(",\"lon\":").append(lon);
		json.append(",\"tooltip\":").append(quoteJson(tooltip));
		json.append('}');
		return json.toString();
	}

	private String toDeliveryJson(List<IGeocoded> deliveries) {
		StringBuilder json = new StringBuilder("[");
		boolean first = true;

		if (deliveries != null) {
			for (IGeocoded delivery : deliveries) {
				String markerJson = toDeliveryMarkerJson(delivery);
				if (markerJson == null) {
					continue;
				}

				if (!first) {
					json.append(',');
				}
				json.append(markerJson);
				first = false;
			}
		}

		json.append(']');
		return json.toString();
	}

	private String toDeliveryMarkerJson(IGeocoded delivery) {
		return toMarkerJson(delivery, "delivery", buildDeliveryId(delivery), buildDeliveryTooltip(delivery));
	}

	private String buildDeliveryId(IGeocoded delivery) {
		return delivery == null ? "" : String.valueOf(delivery.getMercatorX()) + ":" + String.valueOf(delivery.getMercatorY());
	}

	private String buildDeliveryTooltip(IGeocoded delivery) {
		return "Delivery";
	}

	private String toTruckJson(List<ITruckPosition> trucks) {
		StringBuilder json = new StringBuilder("[");
		boolean first = true;

		if (trucks != null) {
			for (ITruckPosition truck : trucks) {
				String markerJson = toTruckMarkerJson(truck);
				if (markerJson == null) {
					continue;
				}

				if (!first) {
					json.append(',');
				}
				json.append(markerJson);
				first = false;
			}
		}

		json.append(']');
		return json.toString();
	}

	private String toTruckMarkerJson(ITruckPosition truck) {
		if (truck == null) {
			return null;
		}

		return toMarkerJson(truck, "truck", buildTruckId(truck), buildTruckTooltip(truck));
	}

	private String buildTruckId(ITruckPosition truck) {
		String truckText = truck.getTruckText();
		if (truckText != null && !truckText.isEmpty()) {
			return truckText;
		}
		return String.valueOf(truck.getMercatorX()) + ":" + String.valueOf(truck.getMercatorY());
	}

	private String buildTruckTooltip(ITruckPosition truck) {
		StringBuilder tooltip = new StringBuilder();

		if (truck.getTruckText() != null && !truck.getTruckText().isEmpty()) {
			tooltip.append(truck.getTruckText());
		}

		if (truck.getTrailerText() != null && !truck.getTrailerText().isEmpty()) {
			if (tooltip.length() > 0) {
				tooltip.append(" / ");
			}
			tooltip.append(truck.getTrailerText());
		}

		if (truck.getPositionDate() != null) {
			if (tooltip.length() > 0) {
				tooltip.append("\n");
			}
			tooltip.append(truck.getPositionDate().toString());
		}

		return tooltip.length() == 0 ? "Truck" : tooltip.toString();
	}

	private String toDepotJson(List<IDepotPosition> depots) {
		StringBuilder json = new StringBuilder("[");
		boolean first = true;

		if (depots != null) {
			for (IDepotPosition depot : depots) {
				String markerJson = toDepotMarkerJson(depot);
				if (markerJson == null) {
					continue;
				}

				if (!first) {
					json.append(',');
				}
				json.append(markerJson);
				first = false;
			}
		}

		json.append(']');
		return json.toString();
	}

	private String toDepotMarkerJson(IDepotPosition depot) {
		if (depot == null) {
			return null;
		}

		return toMarkerJson(depot, "depot", buildDepotId(depot), buildDepotTooltip(depot));
	}

	private String buildDepotId(IDepotPosition depot) {
		String depotText = depot.getDepotText();
		if (depotText != null && !depotText.isEmpty()) {
			return depotText;
		}
		return String.valueOf(depot.getMercatorX()) + ":" + String.valueOf(depot.getMercatorY());
	}

	private String buildDepotTooltip(IDepotPosition depot) {
		StringBuilder tooltip = new StringBuilder();

		if (depot.getDepotText() != null && !depot.getDepotText().isEmpty()) {
			tooltip.append(depot.getDepotText());
		}

		if (depot.getPositionDate() != null) {
			if (tooltip.length() > 0) {
				tooltip.append("\n");
			}
			tooltip.append(depot.getPositionDate().toString());
		}

		return tooltip.length() == 0 ? "Depot" : tooltip.toString();
	}

	private static String quoteJson(String value) {
		if (value == null) {
			return "\"\"";
		}

		StringBuilder result = new StringBuilder("\"");
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"':
					result.append("\\\"");
					break;
				case '\\':
					result.append("\\\\");
					break;
				case '\b':
					result.append("\\b");
					break;
				case '\f':
					result.append("\\f");
					break;
				case '\n':
					result.append("\\n");
					break;
				case '\r':
					result.append("\\r");
					break;
				case '\t':
					result.append("\\t");
					break;
				default:
					if (c < 0x20) {
						result.append(String.format("\\u%04x", Integer.valueOf(c)));
					} else {
						result.append(c);
					}
			}
		}
		result.append('"');
		return result.toString();
	}

	private static boolean isFinite(double value) {
		return !Double.isNaN(value) && !Double.isInfinite(value);
	}

	@Override
	public <E extends IGeocodableAddress> List<IGeocodedWithSource<E>> checkAddresses(List<E> addresses) {
		return Collections.emptyList();
	}

	@Override
	public <E extends IAddress> IGeocodedWithSource<E> geocodeAddress(E address) {
		return null;
	}

	@Override
	public void setMapSelectionListener(IMapSelectionListener listener) {
		this.mapSelectionListener = listener;
	}

	@Override
	public void setImageFilter(IImageFilter filter) {
		this.imageFilter = filter;
	}

	@Override
	public ICalculatedTripInformation getCalculatedTripInformation(List<IGeocoded> stopLocations, String shapeName) {
		return null;
	}

	@Override
	public List<IGeocoded> getOptimizedTrip(List<IGeocoded> tripList, boolean fixedStart, boolean fixedEnd, String trip) {
		return tripList == null ? Collections.<IGeocoded>emptyList() : tripList;
	}

	@Override
	public void setShowAllocated(boolean show) {}

	@Override
	public void setShowAllocatedForObject(boolean show) {}

	@Override
	public void setDropTargetListener(Transfer[] transferTypes, DropTargetListener listener) {}

	@Override
	public boolean isExtracted() {
		return false;
	}

	@Override
	public void setTripKey(int keyLong) {}

	@Override
	public void resetTripKey() {}

	@Override
	public void resetTripKeyStr() {}

	@Override
	public void setTripKeyStr(String keyLong) {}

	@Override
	public void setDepots(List<IDepotPosition> depotPositions) {
		this.depots = depotPositions == null ? new ArrayList<IDepotPosition>() : new ArrayList<IDepotPosition>(depotPositions);
		redrawMap(true);
	}

	@Override
	public void addImage(IDepotPosition value) {
		if (value != null) {
			depots.add(value);
			redrawMap(true);
		}
	}

	@Override
	public void showDepot(boolean b) {}

	@Override
	public void showVehicle(boolean b) {}

	@Override
	public void showMercator(int mercatorX, int mercatorY) {
		centerOnMercator(mercatorX, mercatorY, true);
	}

	@Override
	public MapViewPointController getController() {
		return null;
	}

	@Override
	public IGeocodedWithSource<IAddress> openGeocoderForm(Shell parent, IAddress address) {
		return null;
	}

	@Override
	public void closeGeocoderForm() {}

	@Override
	public boolean isInSearchMode() {
		return inSearchMode;
	}

	@Override
	public boolean isInGeoCodeMode() {
		return inGeoCodeMode;
	}

	@Override
	public void setInGeoCodeMode(boolean inGeoCodeMode) {
		this.inGeoCodeMode = inGeoCodeMode;
	}

	@Override
	public void setSearchMode(boolean inSearchMode) {
		this.inSearchMode = inSearchMode;
	}

	@Override
	public void getNearestShipment(int x, int y) {}

	@Override
	public void getShipments(int x, int y, int x2, int y2) {}

	@Override
	public void setDrawSelectionRectangle(Rectangle rec) {}

	@Override
	public void hideShipmentInfos() {}

	@Override
	public void showInfos(int x, int y) {}

	@Override
	public void addImage(ITruckPosition value) {
		if (value != null) {
			trucks.add(value);
			redrawMap(true);
		}
	}

	@Override
	public void removeImage(GeocodedImageProvider prov) {
		images.remove(prov);
		redrawMap(true);
	}

	@Override
	public void removeImage(IGeocoded position) {
		images.remove(position);
		trucks.remove(position);
		depots.remove(position);
		redrawMap(true);
	}

	@Override
	public void extractDispatchMap() {}

	@Override
	public void setDrawTrip(boolean b) {}

	@Override
	public List<IGeocoded> getOptimizedTrip(List<IGeocoded> tripList, boolean fixedStart, boolean fixedEnd, Trip trip) {
		return tripList == null ? Collections.<IGeocoded>emptyList() : tripList;
	}

	@Override
	public UISynchronize getUISynchronize() {
		return uiSynchronize;
	}
}