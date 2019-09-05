package ch.minova.e4.dispo.dispatch.map.infoware.internal;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.core.services.translation.TranslationService;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.osgi.service.prefs.BackingStoreException;

import ch.minova.dispo.dispatch.map.beans.GeocodableAddressWithSource;
import ch.minova.dispo.dispatch.map.beans.GeocodedAddressWithSource;
import ch.minova.dispo.dispatch.map.beans.GeocodedLocator;
import ch.minova.dispo.dispatch.map.beans.IAddress;
import ch.minova.dispo.dispatch.map.beans.ICoordinates;
import ch.minova.dispo.dispatch.map.beans.IGeocodableAddress;
import ch.minova.dispo.dispatch.map.beans.IGeocodedAddress;
import ch.minova.dispo.dispatch.map.beans.IGeocodedWithSource;
import ch.minova.dispo.dispatch.map.beans.IInfoProvider;
import ch.minova.dispo.dispatch.map.beans.ShipmentInfos;
import ch.minova.dispo.dispatch.map.core.ICalculatedTripInformation;
import ch.minova.dispo.dispatch.map.core.IGeoCoderController;
import ch.minova.dispo.dispatch.map.core.IProjectionsController;
import ch.minova.dispo.dispatch.map.core.IRoutingController;
import ch.minova.dispo.dispatch.model.DispoModelCache;
import ch.minova.dispo.dispatch.model.IGeocoded;
import ch.minova.dispo.dispatch.model.consumption.StationTankQuantityCalculator;
import ch.minova.dispo.dispatch.model.dto.Address;
import ch.minova.dispo.dispatch.model.dto.OpenDeliveryBean;
import ch.minova.dispo.dispatch.model.dto.Remarks;
import ch.minova.dispo.dispatch.model.dto.Shipment;
import ch.minova.dispo.dispatch.model.dto.Trip;
import ch.minova.dispo.dispatch.model.dto.TruckPool;
import ch.minova.dispo.dispatch.model.dto.Vehicle;
import ch.minova.e4.dispo.dispatch.events.DispoDispatchEventTopics;
import ch.minova.e4.dispo.dispatch.map.beans.DepotPosition;
import ch.minova.e4.dispo.dispatch.map.beans.DistancesAndAngles;
import ch.minova.e4.dispo.dispatch.map.beans.GeocodedImageLocator;
import ch.minova.e4.dispo.dispatch.map.beans.GeocodedImageProvider;
import ch.minova.e4.dispo.dispatch.map.beans.TextDecorator;
import ch.minova.e4.dispo.dispatch.map.beans.TruckPosition;
import ch.minova.e4.dispo.dispatch.map.control.MapControlMouseAdapter;
import ch.minova.e4.dispo.dispatch.map.control.MapViewPointController;
import ch.minova.e4.dispo.dispatch.map.dialogs.FindAddressDialog;
import ch.minova.e4.dispo.dispatch.map.preferences.PreferenceIDs;
import ch.minova.e4.dispo.dispatch.map.ui.Activator;
import ch.minova.e4.dispo.dispatch.map.ui.IDepotPosition;
import ch.minova.e4.dispo.dispatch.map.ui.IImageFilter;
import ch.minova.e4.dispo.dispatch.map.ui.IMapControl;
import ch.minova.e4.dispo.dispatch.map.ui.IMapSelectionListener;
import ch.minova.e4.dispo.dispatch.map.ui.ITruckPosition;
import ch.minova.e4.dispo.dispatch.map.ui.MapImageConstants;
import ch.minova.e4.ui.controller.BufferedDisplay;
import ch.minova.e4.ui.preferences.Preference;
import ch.minova.ncore.data.ValueFormatType;
import ch.minova.ncore.data.form.ValueFormatter;
import ch.minova.ncore.internationalization.Messages;
import ch.minova.ncore.log.Log;
import ch.minova.ncore.util.MessageFormat;

/**
 * @author wild
 * @since 11.0.0
 */
public class MapControl implements IMapControl {
	private static final String TRUCK_IMAGE = "TRUCK_IMAGE";
	private static final String DEPOT_IMAGE = "DEPOT_IMAGE";
	// Standard immer 0
	private int tripKey = 0;
	private String tripKeyStr = "";

	@Inject
	private UISynchronize sync;

	private Color getColorOfString(String rgb) {
		RGB locatorRGB = ch.minova.e4.ui.preferences.Preference.asRGB(rgb);
		return new Color(Display.getCurrent(), locatorRGB);
	}

	/**
	 * Linie, die zwischen Lieferungen auf der Karte gezogen wurde, sollte Verzerrung aktiviert sein
	 * 
	 * @author bohlender
	 */
	private class DistortionLine {
		/**
		 * Startpunkt
		 */
		private final Point start;

		/**
		 * Die Bilder, die alle auf diesem Startpunkt liegen.
		 */
		private List<GeocodedImageProvider> providers = new LinkedList<>();

		/**
		 * Jede Lieferung wird einem Index zugeordnet, an den sie dann gerendert wird
		 */
		private int[] indexes;

		/**
		 * @param start
		 *            Der Startpunkt der Linie
		 */
		public DistortionLine(Point start) {
			this.start = start;
		}

		/**
		 * @param provider
		 *            Fügt den {@link GeocodedImageProvider} zur Liste der Provider hinzu
		 */
		public void add(GeocodedImageProvider provider) {
			providers.add(provider);
		}

		/**
		 * Zeichnet das Bild des Providers an der dafür vorgesehenen Position
		 * 
		 * @param gc
		 *            Zielort des zeichnens
		 */
		public void draw(GC gc) {
			int lineLength = -1;
			indexes = new int[providers.size()];
			for (int i = 0; i < providers.size(); i++) {
				GeocodedImageProvider prov = providers.get(i);
				if (prov.getSelection().isSelected()) {
					lineLength++;
					indexes[i] = lineLength;
				} else {
					indexes[i] = -1;
				}
				if (lineLength > 0) {
					Point to = DistancesAndAngles.calculateEndPoint(start.x, start.y, lineLength * distortionDistance, distortionAngle);
					gc.drawLine(start.x, start.y, to.x, to.y);
				}
			}
		}
	}

	/**
	 * Tooltip für die Karte
	 * 
	 * @author bohlender
	 */
	private class InfoToolTip extends org.eclipse.jface.window.ToolTip {
		private String text;
		private Label label;

		/**
		 * Erzeugt einen Tooltip zu einem {@link Control}
		 * 
		 * @param control
		 *            Das zugehörige {@link Control}
		 */
		public InfoToolTip(Control control) {
			super(control, RECREATE, true);
		}

		@Override
		protected Composite createToolTipContentArea(Event event, Composite parent) {
			label = new Label(parent, SWT.WRAP);
			FillLayout layout = new FillLayout();
			layout.spacing = 3;
			layout.marginHeight = 3;
			layout.marginWidth = 3;
			parent.setLayout(layout);
			// label.setFont(new Font(label.getDisplay(), fontData));
			if (text != null) {
				label.setText(text);
			}
			label.pack();
			Rectangle bounds = label.getBounds();
			setShift(new Point(0, 0 - bounds.height - (2 * 3)));
			return parent;
		}

		/**
		 * @param text
		 *            Der anzuzeigende Text
		 */
		protected void setText(String text) {
			this.text = text;
		}
	}

	/**
	 * Hört auf Änderungen an der Konfiguration
	 * 
	 * @author bohlender
	 */
	private class MapPreferencesListener implements IPreferenceChangeListener, INodeChangeListener {
		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			if (event.getKey().equals(PreferenceIDs.MAP_LOCATOR_COLOR)) {
				locatorRGB = Preference.asRGB((String) event.getNewValue());
			}
			if (event.getKey().equals(PreferenceIDs.MAP_LOCATOR_SIZE)) {
				locatorSize = Integer.parseInt((String) event.getNewValue());
			}
			if (event.getKey().equals(PreferenceIDs.MAP_LOCATOR_DISPLAY_TIME)) {
				locatorDuration = Integer.parseInt((String) event.getNewValue());
			}
			if (event.getKey().equals(PreferenceIDs.MAP_LOCATOR_ICON)) {
				iconLocation = (String) event.getNewValue();
			}
			if (event.getKey().equals(PreferenceIDs.MAP_ZOOM_LATITUDE)) {
				mapZoomLatitude = Integer.parseInt((String) event.getNewValue());
			}
			if (event.getKey().equals(PreferenceIDs.MAPDISTORTION_ACTIVATED)) {
				distortion = Boolean.parseBoolean((String) event.getNewValue());
			}
			if (event.getKey().equals(PreferenceIDs.MAPDISTORTION_ANGLE)) {
				distortionAngle = Integer.parseInt((String) event.getNewValue());
			}
			if (event.getKey().equals(PreferenceIDs.MAPDISTORTION_DISTANCE)) {
				distortionDistance = Integer.parseInt((String) event.getNewValue());
			}
			if (event.getKey().equals(PreferenceIDs.MAP_GEOCODING_IGNORE_QUALITY)) {
				geocodeIgnoreQuality = Boolean.parseBoolean((String) event.getNewValue());
			}
			if (event.getKey().equals(PreferenceIDs.MAP_GEOCODING_IGNORE_QUALITY_VALUE)) {
				geocodeIgnoreQualityValue = Integer.parseInt((String) event.getNewValue());
			}
			if (event.getKey().equals(PreferenceIDs.MAP_RESET)) {
				if (extracted) {
					parent.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							shell.setLocation(0, 0);
						}
					});
				} else {
					extractedShellX = 0;
					extractedShellY = 0;
					extractedShellHeight = 0;
					extractedShellWidth = 0;
					extractedMaximized = false;
				}
			}
		}

		@Override
		public void added(NodeChangeEvent event) {
			// refreshMapAreas();
		}

		@Override
		public void removed(NodeChangeEvent event) {
			// refreshMapAreas();
		}
	}

	/**
	 * {@link TimerTask}, mit dem bei mehrfachen Scrollen mit der Maus lediglich die Zoomstufe erhöht wird, ohne wirklich neuzeichnen zu müssen
	 * 
	 * @author bohlender
	 */
	private class ResizeTimerTask extends TimerTask {
		@Override
		public void run() {
			if (!map.isDisposed()) {
				if (Thread.currentThread() == Display.getDefault().getThread()) {
					Rectangle client = map.getClientArea();
					viewpointChanged = true;
					controller.setClientArea(client.width, client.height);
					int widthMap = client.width;
					int heightMap = client.height;
					boolean resizeMap = false;
					if (widthMap > 1920) {
						widthMap = 1920;
						resizeMap = true;
					}
					if (heightMap > 1080) {
						heightMap = 1080;
						resizeMap = true;
					}
					if (resizeMap) {
						map.setSize(widthMap, heightMap);
					}
					redrawMap(true);
				} else {
					Display.getDefault().syncExec(this);
				}
			}
		}
	}

	/**
	 * Erzeugt einen Locator auf der Karte
	 * 
	 * @author bohlender
	 */
	private class FireAndForgetLocator extends TimerTask {
		private Timer timer;
		private GeocodedLocator locator;

		public FireAndForgetLocator(GeocodedLocator locator) {
			this.locator = locator;
			timer = new Timer();
			timer.schedule(this, 1000l * locatorDuration);
		}

		@Override
		public void run() {
			if (!map.isDisposed()) {
				mapImages.remove(locator);
				map.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						if (!map.isDisposed()) {
							redrawMap(false);
						}
					}
				});
				timer.cancel();
				if (locator instanceof GeocodedImageLocator) {
					((GeocodedImageLocator) locator).dispose();
				}
			} else {
				timer.cancel();
			}
		}
	}

	/**
	 * Listener, der auf Größenänderung der gesamten Maske hört und die Karte entsprechend neu läd.
	 * 
	 * @author bohlender
	 */
	private class MapResizeListener implements ControlListener, DisposeListener {
		private Timer timer = new Timer();

		private TimerTask task = null;

		private Shell shell;

		public MapResizeListener(Shell shell) {
			this.shell = shell;
			this.shell.addDisposeListener(this);
		}

		@Override
		public void controlResized(ControlEvent e) {
			// Timer API, um nach 500ms, wenn kein Resize-Event mehr kommt, das
			// Bild neu zu laden
			if (task != null) {
				task.cancel();
			}
			if (timer == null) {
				timer = new Timer();
			}
			try {
				timer.purge();
				task = new ResizeTimerTask();
				timer.schedule(task, 500l);
			} catch (Exception err) {
				// IllegalStateException -> Timer schon gecancelt
				Log.err(this, err.getMessage());
				timer = null;
			}
		}

		@Override
		public void controlMoved(ControlEvent e) {
			// DO NOTHING - Macht keinen Unterschied
		}

		@Override
		public void widgetDisposed(DisposeEvent e) {
			shell.removeControlListener(this);
			if (timer != null) {
				timer.cancel();
				timer.purge();
				timer = null;
			}
		}
	}

	/**
	 * Listener, der auf Position, Größe und Maximiert-Status einer extrahierten Karte hört
	 * 
	 * @author bohlender
	 */
	private class MapExtractedLocationListener implements ControlListener {
		@Override
		public void controlMoved(ControlEvent e) {
			extractedShellX = ((Shell) e.widget).getLocation().x;
			extractedShellY = ((Shell) e.widget).getLocation().y;
			extractedShellWidth = ((Shell) e.widget).getSize().x;
			extractedShellHeight = ((Shell) e.widget).getSize().y;
			extractedMaximized = ((Shell) e.widget).getMaximized();
		}

		@Override
		public void controlResized(ControlEvent e) {
			extractedShellX = ((Shell) e.widget).getLocation().x;
			extractedShellY = ((Shell) e.widget).getLocation().y;
			extractedShellWidth = ((Shell) e.widget).getSize().x;
			extractedShellHeight = ((Shell) e.widget).getSize().y;
			extractedMaximized = ((Shell) e.widget).getMaximized();
		}
	}

	/**
	 * Platzhalter Object mit vorverarbeiteten Informationen für die Karte
	 * 
	 * @author bohlender
	 * @see MapControl#preprocessImages(List)
	 */
	private class MapImages {
		private final List<IGeocoded> images;
		private final Map<Point, DistortionLine> distortionLines;
		private final Map<IGeocoded, Point> imagePoints;

		public MapImages(List<IGeocoded> images, Map<Point, DistortionLine> distortionLines, Map<IGeocoded, Point> imagePoints) {
			super();
			this.images = images;
			this.distortionLines = distortionLines;
			this.imagePoints = imagePoints;
		}
	}

	private class ImageOnMap {
		Image image;
		int x;
		int y;

		private ImageOnMap(Image i, int x, int y) {
			this.image = i;
			this.x = x;
			this.y = y;
		}
	}

	/**
	 * {@link PaintListener}, der alle Zeichenoperationen der Karte ausführt.
	 * 
	 * @author bohlender
	 */
	private class MapPaintListener implements PaintListener {
		private boolean breakOut = false;
		private boolean painting = false;
		private Image mapImage;
		private MapViewPointController mVC;

		// Initial mal mit leeren Listen und Maps füllen
		public MapImages images = new MapImages(new LinkedList<IGeocoded>(), new HashMap<Point, MapControl.DistortionLine>(), new HashMap<IGeocoded, Point>());

		@Override
		public void paintControl(PaintEvent e) {
//			if (painting || mVC == null || mVC.getImage() == null) {
			if (painting) {
				return;
			}
			painting = true;

//			Image newImage = new Image(h.gc.getDevice(), map.getBounds());
//			GC igc = new GC(newImage);
//			igc.setBackground(h.gc.getBackground());
//			igc.setForeground(h.gc.getForeground());
//			igc.drawImage(mVC.getImage(), 0, 0);

			try {
				TruckPosition.reset();
				if (rectangle != null) {
					map.setBackgroundImage(backgroundImage);
					e.gc.setForeground(colorBlack);
//					igc.setForeground(colorBlack);
					if (useRectangle) {
						e.gc.drawRectangle(rectangle);
//						igc.drawRectangle(rectangle);
					} else {
						e.gc.drawFocus(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
//						igc.drawFocus(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
					}
					return;
				}
				if (!images.distortionLines.isEmpty()) {
					for (DistortionLine line : images.distortionLines.values()) {
						line.draw(e.gc);
//						line.draw(igc);
					}
				}
				if (!images.images.isEmpty()) {
					// Beugt gelegentlichen ConcurrentModificationExceptions vor...
					IGeocoded[] pics = images.images.toArray(new IGeocoded[images.images.size()]);
					List<ImageOnMap> mapImages = new ArrayList<>();
					for (IGeocoded geo : pics) {
						if (breakOut) {
							takeShot(e.gc);
							return;
						}
						if (controller.isInViewPort(geo.getMercatorX(), geo.getMercatorY())) {
							if (geo instanceof GeocodedImageProvider) {
								GeocodedImageProvider geocodedImage = (GeocodedImageProvider) geo;
								Boolean allocated = (Boolean) geocodedImage.getInfos().get(ShipmentInfos.ALLOCATED.name());
								if (!showAllocatedShipments && allocated) {
									continue;
								}
								if (imageFilter == null || imageFilter.show(geocodedImage)) {
									if (geocodedImage.getSelection().isSelected() || allocated) {
										// Nach dem Refactoring sollten alle
										// imagePoints schon bereits gerechent sein!
										Point point = images.imagePoints.get(geo);
										if (point == null) {
											continue;
										}
										if (images.distortionLines.containsKey(point)) {
											// Wir müssen den Punkt verschieben
											DistortionLine distortionLine = images.distortionLines.get(point);
											int index = distortionLine.indexes[distortionLine.providers.indexOf(geocodedImage)];
											if (index > 0) {
												point = DistancesAndAngles.calculateEndPoint(point.x, point.y, index * distortionDistance, distortionAngle);
											}
										}
										if (geocodedImage.getDecorator() != null) {
											geocodedImage.getDecorator().draw(e.gc, point.x, point.y);
//											geocodedImage.getDecorator().draw(igc, point.x, point.y);
										}
										Image image = geocodedImage.getImage();
										Rectangle rec = image.getBounds();
										mapImages.add(new ImageOnMap(image, (point.x - (rec.width / 2)), (point.y - (rec.height / 2))));
//										e.gc.drawImage(image, (point.x - (rec.width / 2)), (point.y - (rec.height / 2)));
									}
								}
							} else if (geo instanceof GeocodedImageLocator) {
								GeocodedImageLocator loc = (GeocodedImageLocator) geo;
								Image image = loc.getImage();
								Rectangle bounds = image.getBounds();
								Point locator = controller.getPixelfromMercator(geo.getMercatorX(), geo.getMercatorY());
								images.imagePoints.put(geo, locator);
								mapImages.add(new ImageOnMap(image, locator.x - (bounds.width / 2), locator.y - (bounds.height / 2)));
//								e.gc.drawImage(image, locator.x - (bounds.width / 2), locator.y - (bounds.height / 2));
							} else if (geo instanceof ITruckPosition) {
								if (trucksSelected == true) {
									ITruckPosition truck = (ITruckPosition) geo;
									// Koordinaten für Fahrzeuge IMMER neu rechnen,
									// Caching macht hier Probleme!
									Point locator = controller.getPixelfromMercator(truck.getMercatorX(), truck.getMercatorY());
									// Und cachen sie für den Tooltip
									images.imagePoints.put(truck, locator);
									Image image = registry.get(TRUCK_IMAGE);
									Rectangle bounds = image.getBounds();
									// Image erst später Zeichnen, erstmal die Linien
									// und Texte!
									int centerx = locator.x - (bounds.width / 2);
									int centery = locator.y - (bounds.height / 2);
									truck.getDecorator().draw(e.gc, locator.x, locator.y);
//									truck.getDecorator().draw(igc, locator.x, locator.y);
									// Zum Schluss das Bild
									mapImages.add(new ImageOnMap(image, centerx, centery));
//									e.gc.drawImage(image, centerx, centery);
								}
							} else if (geo instanceof IDepotPosition) {
								if (tdepotsSelected == true) {
									IDepotPosition depot = (IDepotPosition) geo;
									// Koordinaten für Fahrzeuge IMMER neu rechnen,
									// Caching macht hier Probleme!
									Point locator = controller.getPixelfromMercator(depot.getMercatorX(), depot.getMercatorY());
									// Und cachen sie für den Tooltip
									images.imagePoints.put(depot, locator);
									Image image = registry.get(DEPOT_IMAGE);
									Rectangle bounds = image.getBounds();
									// Image erst später Zeichnen, erstmal die Linien
									// und Texte!
									int centerx = locator.x - (bounds.width / 2);
									int centery = locator.y - (bounds.height / 2);
									depot.getDecorator().draw(e.gc, locator.x, locator.y);
//									depot.getDecorator().draw(igc, locator.x, locator.y);
									// Zum Schluss das Bild
									mapImages.add(new ImageOnMap(image, centerx, centery));
//									e.gc.drawImage(image, centerx, centery);
								}
							} else if (geo instanceof GeocodedLocator) {
								Color locColor = new Color(e.display, locatorRGB);
//								igc.setForeground(locColor);
								e.gc.setForeground(locColor);
								Point locator = controller.getPixelfromMercator(geo.getMercatorX(), geo.getMercatorY());
								images.imagePoints.put(geo, locator);
								e.gc.drawOval(locator.x - (locatorSize / 2), locator.y - (locatorSize / 2), locatorSize, locatorSize);
//								igc.drawOval(locator.x - (locatorSize / 2), locator.y - (locatorSize / 2), locatorSize, locatorSize);
								locColor.dispose();
							}
						} else if (geo instanceof GeocodedImageProvider) {
							// nein, das könnten wir auch noch mal malen
							// ((GeocodedImageProvider) geo).dispose();
						}
					}
					// Wir malen alles auf einmal
					for (ImageOnMap imageOnMap : mapImages) {
//						igc.drawImage(imageOnMap.image, imageOnMap.x, imageOnMap.y);
						e.gc.drawImage(imageOnMap.image, imageOnMap.x, imageOnMap.y);
					}
					// Einmal durchgelaufen setzen wir das Flag wieder um
					breakOut = false;
//					h.gc.copyArea(newImage, 0, 0);
//					e.gc.dispose();
//					igc.dispose();
				}

				if (rectangle == null) {
					takeShot(e.gc);
				}
			} finally {
				painting = false;
			}
		}

		private void takeShot(GC gc) {
			if (backgroundImage != null) {
				backgroundImage.dispose();
			}
			if (map.getBounds().width == 0 && map.getBounds().height == 0) {
				map.setBounds(map.getBounds().x, map.getBounds().y, 1, 1);
			}
			backgroundImage = new Image(gc.getDevice(), map.getBounds());
			// Dieses Problem hat nur Neon und MAC
			// map.removePaintListener(paintListener);
			gc.copyArea(backgroundImage, 0, 0);
			// map.addPaintListener(paintListener);
		}

		public Image getMapImage() {
			return mapImage;
		}

		public void setMapImage(Image mapImage) {
			this.mapImage = mapImage;
		}

		public MapViewPointController getmVC() {
			return mVC;
		}

		public void setmVC(MapViewPointController mVC) {
			this.mVC = mVC;
		}
	}

	private MapViewPointController controller;
	private Composite parent;
	private Canvas map;
	private MapControlMouseAdapter adapter;
	private Image backgroundImage = null;

	private int locatorSize = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).getInt(PreferenceIDs.MAP_LOCATOR_SIZE, 25);
	private int locatorDuration = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).getInt(PreferenceIDs.MAP_LOCATOR_DISPLAY_TIME, 15);

	private List<IGeocoded> mapImages = new CopyOnWriteArrayList<>();
	private RGB locatorRGB;
	private String iconLocation;

	private boolean extracted = false;
	private boolean initialExtracted = false;
	private Composite parentComposite;

	private IEclipsePreferences areaprefs;

	@Inject
	@Optional
	private IProjectionsController projections;

	@Inject
	private IEclipseContext context;

	@Inject
	private IEventBroker broker;

	@Inject
	@Optional
	private IGeoCoderController geoCoder; // FIXME und wenn keiner da ist?

	@Inject
	@Optional
	private IRoutingController routing; // FIXME und wenn keiner da ist?

	private TranslationService translationService;

	private Shell infoShell;

	private Table infoTable;

	private IMapSelectionListener listener;

	private InfoToolTip infoToolTip;
	private InfoToolTip truckToolTip;

	private IImageFilter imageFilter = null;

	private FindAddressDialog addressDialog = null;

	private boolean inSearchMode = false;
	private boolean inGeoCodeMode = false;

	private MapPreferencesListener prefChange;

	private Shell shell;

	private boolean parentClosed = false;

	private int extractedShellX = 0;
	private int extractedShellY = 0;
	private int extractedShellWidth = 0;
	private int extractedShellHeight = 0;
	private boolean extractedMaximized = false;

	private MapExtractedLocationListener extractionListener;
	private ImageRegistry registry = new ImageRegistry();

	private boolean trucksSelected;
	private boolean tdepotsSelected;
	private boolean enabledepot;

	private Rectangle rectangle;

	private boolean useRectangle = Platform.getWS().equals(Platform.WS_WIN32);
	private final Color colorBlack = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
	private boolean viewpointChanged;

	private int mapZoomLatitude = 5000;

	private boolean distortion = false;
	private int distortionAngle = 0;
	private int distortionDistance = 0;

	// private Map<Point, DistortionLine> distortionLines = new HashMap<Point,
	// MapControl.DistortionLine>();
	private boolean geocodeIgnoreQuality = false;
	private Integer geocodeIgnoreQualityValue = 90;
	private boolean showAllocatedShipments;
	private Integer maxShipmentsDispatchedInArea = 25;
	private Integer drawMapDelay = 2000;

	//
	private boolean showAllocatedforObject;
	private Integer showtripkey;
	private Integer showtruckKey;
	private Integer showtruckPoolKey;
	private LocalDate scheduledDate;

	private DropTargetListener dropTargetListener;
	private Transfer[] transferTypes;
	private DropTarget mapDropTarget;
	// private FontData fontData;

	private final MapPaintListener paintListener = new MapPaintListener();
	private boolean drawTrip;

	protected Boolean draw = false;

	private String locatorRGBfirstPositionBackground;
	private String locatorRGBfirstPositionForeground;
	private String locatorRGBAllPositionBackground;
	private String locatorRGBAllPositionForeground;

	public MapControl() {
		init();
	}

	@Override
	public void init() {
		// TODO können jetzt injected werden
		IEclipsePreferences mapPrefsNode = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		prefChange = new MapPreferencesListener();
		mapPrefsNode.addPreferenceChangeListener(prefChange);
		locatorRGBAllPositionForeground = mapPrefsNode.get(PreferenceIDs.MAP_LOCATOR_COLOR_FOREGROUND_SHIPMENT_OF_TRIP, "255, 255, 255");
		locatorRGBAllPositionBackground = mapPrefsNode.get(PreferenceIDs.MAP_LOCATOR_COLOR_BACKGROUND_SHIPMENT_OF_TRIP, "0, 0, 128");
		locatorRGBfirstPositionForeground = mapPrefsNode.get(PreferenceIDs.MAP_LOCATOR_COLOR_FOREGROUND_FIRST_SHIPMENT_OF_TRIP, "255, 255, 255");
		locatorRGBfirstPositionBackground = mapPrefsNode.get(PreferenceIDs.MAP_LOCATOR_COLOR_BACKGROUND_FIRST_SHIPMENT_OF_TRIP, "0, 0, 128");
		locatorSize = mapPrefsNode.getInt(PreferenceIDs.MAP_LOCATOR_SIZE, 20);
		String rgbString = mapPrefsNode.get(PreferenceIDs.MAP_LOCATOR_COLOR, "255, 0, 0");
		locatorRGB = Preference.asRGB(rgbString);
		iconLocation = mapPrefsNode.get(PreferenceIDs.MAP_LOCATOR_ICON, null);
		areaprefs = ConfigurationScope.INSTANCE.getNode(PreferenceIDs.MAPAREA_PREFERENCES_CONTEXT);
		areaprefs.addNodeChangeListener(prefChange);
		extractedShellHeight = mapPrefsNode.getInt(PreferenceIDs.MAP_HEIGHT, 0);
		extractedShellWidth = mapPrefsNode.getInt(PreferenceIDs.MAP_WIDTH, 0);
		extractedShellX = mapPrefsNode.getInt(PreferenceIDs.MAP_LOCATION_X, 0);
		extractedShellY = mapPrefsNode.getInt(PreferenceIDs.MAP_LOCATION_Y, 0);
		extractedMaximized = mapPrefsNode.getBoolean(PreferenceIDs.MAP_EXTRACTED_MAXIMIZED, false);
		initialExtracted = mapPrefsNode.getBoolean(PreferenceIDs.MAP_EXTRACTED, false);
		mapZoomLatitude = mapPrefsNode.getInt(PreferenceIDs.MAP_ZOOM_LATITUDE, 5000);
		distortion = mapPrefsNode.getBoolean(PreferenceIDs.MAPDISTORTION_ACTIVATED, false);
		distortionAngle = mapPrefsNode.getInt(PreferenceIDs.MAPDISTORTION_ANGLE, 0);
		distortionDistance = mapPrefsNode.getInt(PreferenceIDs.MAPDISTORTION_DISTANCE, 25);
		geocodeIgnoreQuality = mapPrefsNode.getBoolean(PreferenceIDs.MAP_GEOCODING_IGNORE_QUALITY, geocodeIgnoreQuality);
		geocodeIgnoreQualityValue = mapPrefsNode.getInt(PreferenceIDs.MAP_GEOCODING_IGNORE_QUALITY_VALUE, geocodeIgnoreQualityValue);
		maxShipmentsDispatchedInArea = mapPrefsNode.getInt(PreferenceIDs.MAP_MAX_SHIPMENTS_DISPATCHED_IN_AREA, maxShipmentsDispatchedInArea);
		drawMapDelay = mapPrefsNode.getInt(PreferenceIDs.DRAW_MAP_DELAY, 2000);

		if (registry.get(TRUCK_IMAGE) == null) {
			registry.put(TRUCK_IMAGE, Activator.getImageRegistry().getDescriptor(MapImageConstants.MAPIMAGES_TRUCK));
		}
		if (registry.get(DEPOT_IMAGE) == null) {
			registry.put(DEPOT_IMAGE, Activator.getImageRegistry().getDescriptor(MapImageConstants.MAPIMAGES_DEPOT));
		}
	}

	@Override
	public void createMap(Composite parentComposite, TranslationService service) {
		createMap(parentComposite, service, true);
	}

	@Override
	public void createMap(Composite parentComposite, TranslationService service, boolean extractable, boolean showToolbar) {
		this.parentComposite = parentComposite;
		this.translationService = service;
		this.parent = new Composite(parentComposite, SWT.NONE);
//		this.parent = new Composite(parentComposite, SWT.NO_BACKGROUND);
		GridLayout gridLayout = new GridLayout(1, false);
		this.parent.setLayout(gridLayout);

		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		ToolBar toolbar = new ToolBar(parent, SWT.NONE);
		toolbar.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		createToolBar(service, toolbar);
		if (showToolbar) {
			parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}

//		map = new Canvas(parent, SWT.NO_BACKGROUND);
		map = new Canvas(parent, SWT.NONE);
		map.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		map.addControlListener(new MapResizeListener(parentComposite.getShell()));
		map.addPaintListener(paintListener);
		controller = new MapViewPointController(this);
		// context.getParent().set(MapViewPointController.class, controller);
		adapter = new MapControlMouseAdapter(this, controller);
//		paintListener.setmVC(controller);
		// jetzt sind wir fertig - inject ganz oben, um die Helper zu informieren
		// das wird jetzt an eine andere Stelle gesetzt als es ursprünglich lag, es ist
		// aber dasselbe Objekt
		// this.context.getParent().getParent().set(IMapControl.class, null);
		// this.context.getParent().getParent().set(IMapControl.class, this);
	}

	private void createToolBar(final TranslationService service, ToolBar toolbar) {
		ToolItem itemZoomIn = new ToolItem(toolbar, SWT.NONE);
		itemZoomIn.setImage(Activator.getImageRegistry().get(MapImageConstants.MAPIMAGES_ZOOM_IN));
		itemZoomIn.setToolTipText(service.translate("@Dispo.Dispatch.MapSectionEnlarge", null));
		itemZoomIn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// DO SOMETHING
				controller.zoomIn();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// DO NOTHING
			}
		});
		ToolItem itemZoomOut = new ToolItem(toolbar, SWT.NONE);
		itemZoomOut.setImage(Activator.getImageRegistry().get(MapImageConstants.MAPIMAGES_ZOOM_OUT));
		itemZoomOut.setToolTipText(service.translate("@Dispo.Dispatch.MapSectionScaleDown", null));
		itemZoomOut.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// DO SOMETHING
				controller.zoomOut();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// DO NOTHING
			}
		});
		ToolItem itemZoomFull = new ToolItem(toolbar, SWT.NONE);
		itemZoomFull.setImage(Activator.getImageRegistry().get(MapImageConstants.MAPIMAGES_ZOOM_FULL_EXTENT));
		itemZoomFull.setToolTipText(service.translate("@Dispo.Dispatch.ShowWholeMap", null));
		itemZoomFull.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// DO SOMETHING
				controller.zoomFullExtent();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// DO NOTHING
			}
		});
	}

	@Override
	public void createMap(Composite parentComposite, TranslationService service, boolean extractable) {
		this.parentComposite = parentComposite;
		this.translationService = service;
		this.parent = new Composite(parentComposite, SWT.NONE);
//		this.parent = new Composite(parentComposite, SWT.NO_BACKGROUND);
		GridLayout gridLayout = new GridLayout(1, false);
		this.parent.setLayout(gridLayout);

		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

//		map = new Canvas(parent, SWT.NO_BACKGROUND);
		map = new Canvas(parent, SWT.NONE);

		map.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		map.addControlListener(new MapResizeListener(parentComposite.getShell()));
		map.addPaintListener(paintListener);
		controller = new MapViewPointController(this);
//		paintListener.setmVC(controller);
		context.getParent().set(MapViewPointController.class, controller);
		adapter = new MapControlMouseAdapter(this, controller);
		if (extractable && initialExtracted) {
			parent.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					extractMap();
				}
			});
		}

		// jetzt sind wir fertig - inject ganz oben, um die Helper zu informieren
		// das wird jetzt an eine andere Stelle gesetzt als es ursprünglich lag, es ist
		// aber dasselbe Objekt
		this.context.getParent().getParent().set(IMapControl.class, null);
		this.context.getParent().getParent().set(IMapControl.class, this);
	}

	protected void extractMap() {
		if (!extracted) {
			boolean pack = false;
			Point parentSize = parent.getSize();
			shell = new Shell(parent.getDisplay(), SWT.BORDER | SWT.CLOSE | SWT.MIN | SWT.MAX | SWT.RESIZE);
			shell.setLayout(new FillLayout());
			shell.setText(translationService.translate("@Dispo.Dispatch.Group.Map", null));
			shell.setImage(Activator.getImageRegistry().get(MapImageConstants.MAPIMAGES_MAP));
			Composite container = new Composite(shell, SWT.NONE);
			GridLayout layout = new GridLayout();
			container.setLayout(layout);
			extracted = reparentMap(container);
			GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);

			if (extractedShellX == 0 && extractedShellY == 0 && extractedShellHeight == 0 && extractedShellWidth == 0) {
				// Default initialisieren
				layoutData.widthHint = parentSize.x;
				layoutData.heightHint = parentSize.y;
				pack = true;
			}

			parent.setLayoutData(layoutData);

			container.addControlListener(new MapResizeListener(shell));
			// extract.setToolTipText(service.translate("Dispo.Dispatch.Tooltip.CloseMapWindow",
			// null));

			parentComposite.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					parentClosed = true;
					if (!shell.isDisposed()) {
						if (mapDropTarget != null) {
							// Wir müssen den Listener hier lösen, da sonst ein
							// DND-Error geworfen wird, sollte das Widget das
							// nächste mal für DnD angemeldet werden
							mapDropTarget.removeDropListener(dropTargetListener);
							// mapDropTarget.dispose();
							// mapDropTarget = null;
						}
						shell.close();
					}
				}
			});

			shell.addShellListener(new ShellAdapter() {
				@Override
				public void shellClosed(ShellEvent e) {
					if (!parentClosed) {
						resetMap();
					}
				}
			});

			// mapDropTarget = null;
			if (transferTypes != null && dropTargetListener != null) {
				if (mapDropTarget == null) {
					mapDropTarget = new DropTarget(parent, DND.DROP_MOVE);
					mapDropTarget.setTransfer(transferTypes);
					mapDropTarget.addDropListener(dropTargetListener);
				}
			}

			if (pack) {
				shell.pack();
				extractionListener = new MapExtractedLocationListener();
				shell.addControlListener(extractionListener);
			}
			shell.open();
			if (!pack) {
				shell.setSize(extractedShellWidth, extractedShellHeight);
				shell.setLocation(extractedShellX, extractedShellY);
				shell.layout();
				shell.setMaximized(extractedMaximized);
				extractionListener = new MapExtractedLocationListener();
				shell.addControlListener(extractionListener);
			}

			while (!shell.isDisposed()) {
				if (!shell.getDisplay().readAndDispatch()) {
					shell.getDisplay().sleep();
				}
			}

			if (!shell.isDisposed()) {
				shell.dispose();
			}
		} else {
			resetMap();
		}
	}

	@Inject
	public void showActiveTruck(@Optional @Named(DispoDispatchEventTopics.DISPATCH_CONTEXT_ACTIVE_TRUCK) Vehicle vehicle) {
		if (vehicle != null) {
			// showtripkey = null;
			// showtruckPoolKey = null;
			showtruckKey = vehicle.getKey();
		} else {
			showtripkey = null;
			showtruckPoolKey = null;
			showtruckKey = null;
		}
		redrawIfAllocatedforObject();
	}

	@Inject
	public void showActiveTruckPool(@Optional @Named(DispoDispatchEventTopics.DISPATCH_CONTEXT_ACTIVE_TRUCKPOOL) Vehicle vehicle) {
		if (vehicle != null) {
			if (vehicle.isTruckPool()) {
				// showtruckKey = null;
				// showtripkey = null;
				showtruckPoolKey = vehicle.getKey();
			}
		} else {
			showtruckPoolKey = null;
		}
		redrawIfAllocatedforObject();
	}

	@Inject
	private void showCurrentScheduledDate(@Optional @Named(DispoDispatchEventTopics.DISPATCH_CONTEXT_SCHEDULED_DATE) LocalDate scheduledDate) {
		this.scheduledDate = scheduledDate;
		redrawIfAllocatedforObject();
	}

	@Inject
	private void showActiveTrip(@Optional @Named(DispoDispatchEventTopics.DISPATCH_CONTEXT_ACTIVE_TRIP) Trip trip) {
		if (trip != null) {
			// showtruckPoolKey = null;
			// showtruckKey = null;
			showtripkey = trip.getKey();
		} else {
			showtripkey = null;
			showtruckPoolKey = null;
			showtruckKey = null;
		}
		redrawIfAllocatedforObject();
		if (this.drawTrip) {
			if (trip != null) {
				setTripKey(trip.getKey());
				setTripKeyStr(trip.getKey().toString() + "_" + trip.getCalculatedNumber());
			} else {
				setTripKey(0);
				setTripKeyStr(null);
			}

			// Zeichnen wenn geändert.
			context.getParent().set(DispoDispatchEventTopics.DISPATCH_CONTEXT_ACTIVE_TRIP_SHAPE, tripKeyStr);
			redraw(tripKeyStr);
		}
	}

	private void redrawIfAllocatedforObject() {
		if (showAllocatedforObject) {
			redrawMap(false, true);
		}
	}

	private void resetMap() {
		shell.removeControlListener(extractionListener);
		extracted = !reparentMap(parentComposite);
		// extract.setToolTipText(service.translate("@Dispo.Dispatch.Tooltip.OpenMapWindow", null));
		shell.dispose();
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		controller.setMapDistanceByX(controller.getMapDistanceX());
		parentComposite.layout(true, true);
		redrawMap(true);
	}

	@Override
	public void redrawMapImages(final boolean changed) {
		this.viewpointChanged = changed;
		// Wenn wir das Rechteck ziehen, wollen wir eigentlich zoomen und müssen
		// erstmal nix mit den Bildern machen,
		// bis wir einen erneuten Redraw bekommen!
		if (rectangle == null) {
			// Wir machen synchron eine Kopie der Liste, die wir anschließend vorbearbeiten
			List<IGeocoded> images = new CopyOnWriteArrayList<>(mapImages);

			paintListener.breakOut = true;
			paintListener.images = preprocessImages(images);
		}
	}

	/**
	 * Wird immer im UI-Thread aufgerufen!
	 */
	@Override
	public void redrawMap(final boolean changed) {
		redrawMap(changed, false);
	}

	/**
	 * Wird immer im UI-Thread aufgerufen!
	 */
	protected void redrawMap(final boolean changed, final String trip) {
		tripKeyStr = trip;

		// ab hier kann man wahrscheinlich die redrawMap(changed) von oben verwenden
		redrawMap(changed, true);
	}

	private Boolean changedmap = false;

	/**
	 * @author wild
	 * @since 11.1.0
	 */
	@Override
	public void redrawMap(final boolean changed, final boolean async) {
		synchronized (changedmap) {
			// Sobald wir es auf true setzen bleibt es ach so
			changedmap |= changed;
		}
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (!map.isDisposed()) {
					redrawMapImages(changedmap);
					map.getShell().setCursor(map.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
					if (changedmap) {
						// Wenn es eine Konkrete Shapedatei gibt, dann wird diese angezeigt.
						if (tripKeyStr != null && !tripKeyStr.isEmpty()) {
							controller.drawMapTrip(changedmap, tripKeyStr);
						} else if (tripKey != 0) {
							controller.drawMapTrip(changedmap, String.valueOf(tripKey));
						} else {
							controller.drawMap(changedmap);
						}
					}
					map.getShell().setCursor(null);
					paintListener.breakOut = false;
					Image backgroundImage = map.getBackgroundImage();
					if (backgroundImage == null || backgroundImage.isDisposed()) {
						controller.drawMap(true);
					}
					map.redraw();

					// wieder zurücksetzen
					synchronized (changedmap) {
						changedmap = false;
					}
					// erst wenn tatsächlich fertig gemalt wurde!
					synchronized (draw) {
						draw = false;
					}
				}
			}
		};

		if (!async) {
			redrawMapImages(changed);
			map.getDisplay().asyncExec(runnable);
		} else {
			synchronized (this.draw) {
				if (!this.draw) {
					this.draw = true;
					if (!map.isDisposed()) {
						BufferedDisplay.getDefault().asyncExec("drawMap", runnable, drawMapDelay);
					}
					// else es wird schon gezeichnet!
				}
			}
		}
	}

	private MapImages preprocessImages(List<IGeocoded> mapImages) {
		Map<IGeocoded, Point> imagePoints = null;
		if (viewpointChanged || paintListener.images == null) {
			// Diese Punkte IMMER löschen, wenn sich der sichtbare Auschnitt ändert
			imagePoints = new HashMap<>();
		} else {
			imagePoints = paintListener.images.imagePoints;
		}
		Map<Point, DistortionLine> distortionLines = new HashMap<>();
		// Kartenpunkte rechnen
		for (IGeocoded geo : mapImages) {
			Point p = imagePoints.get(geo);
			if (p == null) {
				// Kartenpunkt berechnen
				// TODO Nullpointer abfangen
				try {
					if (controller.isInViewPort(geo.getMercatorX(), geo.getMercatorY())) {
						p = controller.getPixelfromMercator(geo.getMercatorX(), geo.getMercatorY());
						imagePoints.put(geo, p);
					} else {
						// liegt der Punkt ausserhalb des sichtbaren Bereichs, so
						// müssen wir ihn auch nicht zeichnen
						continue;
					}

				} catch (NullPointerException ex) {
					imagePoints.remove(geo);
					continue;
				}
			}
			// An dieser Stelle werden die Positionen der Depot ein und ausgeblendet.
			if (geo instanceof DepotPosition) {
				if (!tdepotsSelected) {
					if (p != null) {
						imagePoints.remove(geo);
					}
					continue;
				}
			}
			// An dieser Stelle werden die Positionen der Trucks ein und ausgeblendet.
			if (geo instanceof TruckPosition) {
				if (!trucksSelected) {
					if (p != null) {
						imagePoints.remove(geo);
					}
					continue;
				}
			}
			if (geo instanceof GeocodedImageProvider) {
				handleGeocodedImageProvider((GeocodedImageProvider) geo);
				GeocodedImageProvider prov = (GeocodedImageProvider) geo;
				if ((Boolean) prov.getInfos().get(ShipmentInfos.ALLOCATED.name()) && showAllocatedforObject) {
					if (checkShowGeo(prov)) {
						continue;
					} else {
						if (p != null) {
							imagePoints.remove(geo);
						}
						continue;
					}
				}
				if ((Boolean) prov.getInfos().get(ShipmentInfos.ALLOCATED.name()) && !showAllocatedShipments) {
					// Das Shipment ist allocated und wir wollen es nicht
					// anzeigen, also schmeissen wirs raus
					if (p != null) {
						imagePoints.remove(geo);
					}
					continue;
				}
				if (distortion) {
					DistortionLine line = null;
					if (distortionLines.containsKey(p)) {
						line = distortionLines.get(p);
						line.add(prov);
					} else {
						line = new DistortionLine(p);
						line.add(prov);
						distortionLines.put(p, line);
					}
				}
			}
		}
		return new MapImages(mapImages, distortionLines, imagePoints);
	}

	private void handleGeocodedImageProvider(GeocodedImageProvider geo) {
		Object object = geo.getInfos().get("");
		if (object != null && object instanceof Shipment) {
			Shipment shipment = (Shipment) object;
			// #32593 markiert die 1. Lieferposition in den Komplimentärfarben.
			if (shipment.getTrip() != null) {
				if (geo.getDecorator() != null) {
					if (shipment.isfirstShipmentOfTrip()) {
						((TextDecorator) geo.getDecorator()).setBackground(getColorOfString(locatorRGBfirstPositionBackground));
						((TextDecorator) geo.getDecorator()).setForeground(getColorOfString(locatorRGBfirstPositionForeground));
					} else if (shipment.getTrip() != null) {
						if (geo.getDecorator() != null) {
							((TextDecorator) geo.getDecorator()).setBackground(getColorOfString(locatorRGBAllPositionBackground));
							((TextDecorator) geo.getDecorator()).setForeground(getColorOfString(locatorRGBAllPositionForeground));
						} else {

						}
					}
				}
			}
		}
	}

	/**
	 * Diese Methode überprüft, ob das Bild auf die Karte gemalt wird order nicht
	 * 
	 * @param geo
	 * @return
	 */
	public boolean checkShowGeo(GeocodedImageProvider geo) {
		Object object = geo.getInfos().get("SOURCE");
		if (object instanceof Shipment) {
			Trip trip = ((Shipment) object).getTrip();
			if (trip != null && trip.getScheduledDate().equals(scheduledDate)) {
				if (this.showtripkey != null) {
					if (this.showtripkey.equals(((Shipment) object).getTripKey())) {
						return true;
					} else {
						return false;
					}
				} else if (this.showtruckKey != null && this.showtruckKey.equals(((Shipment) object).getTruck().getVehicleKey())) {
					return true;
				} else if (this.showtruckPoolKey != null) {
					TruckPool truckPool = DispoModelCache.getInstance().getTruckPool(showtruckPoolKey);
					if (truckPool.getVehicleKeys().contains(((Shipment) object).getTruck().getVehicleKey())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean reparentMap(Composite parent) {
		if (this.parent.isReparentable()) {
			boolean setParent = this.parent.setParent(parent);
			map.setFocus();
			return setParent;
		}
		return false;
	}

	@Override
	public Composite getParent() {
		return parent;
	}

	@Override
	public void dispose() {
		// MouseAdapter disposen
		if (adapter != null) {
			adapter.dispose();
		}
		// // Dann den Rest
		if (parent != null && !parent.isDisposed()) {
			parent.dispose();
		}
		IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		prefs.removePreferenceChangeListener(prefChange);
		prefs.putInt(PreferenceIDs.MAP_LOCATION_X, extractedShellX);
		prefs.putInt(PreferenceIDs.MAP_LOCATION_Y, extractedShellY);
		prefs.putInt(PreferenceIDs.MAP_WIDTH, extractedShellWidth);
		prefs.putInt(PreferenceIDs.MAP_HEIGHT, extractedShellHeight);
		prefs.putBoolean(PreferenceIDs.MAP_EXTRACTED, extracted);
		prefs.putBoolean(PreferenceIDs.MAP_EXTRACTED_MAXIMIZED, extractedMaximized);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			Log.err(this, e);
		}
		if (controller != null) {
			controller.dispose();
			controller = null;
		}

		// jetzt sind wir fertig - inject ganz oben, um die Helper zu informieren
		// das wird jetzt an eine andere Stelle gesetzt als es ursprünglich lag, es ist
		// aber dasselbe Objekt
		this.context.getParent().getParent().set(IMapControl.class, null);
		this.context.getParent().getParent().set(IMapControl.class, this);
	}

	@Override
	public Canvas getMap() {
		return map;
	}

	@Override
	public void showMercator(int mercatorX, int mercatorY) {
		if (controller.isInViewPort(mercatorX, mercatorY)) {
			parentComposite.setFocus();
			if (iconLocation != null) {
				showLocator(new GeocodedImageLocator(mercatorX, mercatorY, iconLocation), false);
			} else {
				showLocator(new GeocodedLocator(mercatorX, mercatorY), false);
			}
		}
	}

	protected boolean centerOnMercator(int mercatorX, int mercatorY) {
		if (controller.getMapCenterX() == mercatorX && controller.getMapCenterY() == mercatorY && controller.getMapDistanceX() == mapZoomLatitude) {
			// Optimierung
			return false;
		}
		controller.setMapCenter(mercatorX, mercatorY);
		controller.setMapDistanceByX(mapZoomLatitude);
		return true;
	}

	@Override
	public boolean centerOnMercator(int mercatorX, int mercatorY, boolean showCenter) {
		boolean changed = centerOnMercator(mercatorX, mercatorY);

		if (showCenter) {
			if (iconLocation != null) {
				showLocator(new GeocodedImageLocator(mercatorX, mercatorY, iconLocation), changed);
			} else {
				showLocator(new GeocodedLocator(mercatorX, mercatorY), changed);
			}
		}

		return changed;
	}

	private void showLocator(GeocodedLocator locator, boolean changed) {
		mapImages.add(locator);
		new FireAndForgetLocator(locator);
		redrawMap(changed);
	}

	protected void showLocatorMercator(int mercx, int mercy) {
		if (controller.isInViewPort(mercx, mercy)) {
			if (iconLocation != null) {
				showLocator(new GeocodedImageLocator(mercx, mercy, iconLocation), false);
			} else {
				showLocator(new GeocodedLocator(mercx, mercy), false);
			}
		}
	}

	@Override
	public Point getTopLeft() {
		return new Point(controller.getMercatorLeft(), controller.getMercatorTop());
	}

	@Override
	public Point getBottomRight() {
		return new Point(controller.getMercatorRight(), controller.getMercatorBottom());
	}

	@Override
	public ICoordinates toWGS84(IGeocoded geo) {
		return toWGS84(geo.getMercatorX(), geo.getMercatorY());
	}

	@Override
	public ICoordinates toWGS84(int mercatorX, int mercatorY) {
		if (this.projections == null) {
			throw new RuntimeException("ProjectionsController not available");
		} else {
			return projections.toWGS84(mercatorX, mercatorY);
		}
	}

	@Override
	public void addImage(GeocodedImageProvider imageProvider) {
		if (imageProvider != null) {
			mapImages.add(imageProvider);
		}
	}

	@Override
	public void addImage(IDepotPosition value) {
		mapImages.add(value);
	}

	@Override
	public void addImage(ITruckPosition value) {
		mapImages.add(value);
	}

	@Override
	public void setImages(List<IGeocoded> images) {
		if (images != null) {
			mapImages = images;
		}
	}

	@Override
	public void clearImages() {
		mapImages.clear();
	}

	@Override
	public void setTrucks(List<ITruckPosition> trucks) {
		trucksSelected = true;
		mapImages.addAll(trucks);
		redrawMap(false, true);
	}

	@Override
	public void setDepots(List<IDepotPosition> depots) {
		if (!enabledepot) {
			mapImages.addAll(depots);
			enabledepot = true;
		}
	}

	@Override
	public IGeocoded toMercator(double wgs84x, double wgs84y) {
		if (this.projections == null) {
			throw new RuntimeException("ProjectionsController not available");
		} else {
			return projections.toMercator(wgs84x, wgs84y);
		}
	}

	@Override
	public void redraw() {
		redrawMap(false);
	}

	@Override
	public void redraw(String Trip) {
		redrawMap(true, Trip);
	}

	private List<IInfoProvider> getShipmentsFromPixel(int pixelx, int pixely) {
		List<IInfoProvider> shipments = new LinkedList<>();
		int minx, maxx, miny, maxy;
		int hoverPixels = Activator.getHoverPixels();
		minx = pixelx - hoverPixels;
		maxx = pixelx + hoverPixels;
		miny = pixely - hoverPixels;
		maxy = pixely + hoverPixels;
		for (IGeocoded geo : paintListener.images.imagePoints.keySet().toArray(new IGeocoded[0])) {
			if (geo instanceof IInfoProvider) {
				Point p = paintListener.images.imagePoints.get(geo);
				Boolean allocated = (Boolean) ((IInfoProvider) geo).getInfos().get(ShipmentInfos.ALLOCATED.name());
				if (geo instanceof GeocodedImageProvider && ((GeocodedImageProvider) geo).getSelection().isSelected() || allocated) {
					// Es kann sein, das der ausgewählte Punkt nicht mehr
					// vorhanden ist, da er disponiert wurde...
					// Wir müssen noch prüfen, ob da allocated Shipments dabei
					// sind
					if (allocated && !showAllocatedShipments) {
						continue;
					}
					if ((p.x > minx && p.x < maxx) && (p.y > miny && p.y < maxy) && mapImages.contains(geo)) {
						shipments.add((IInfoProvider) geo);
					} else if (!mapImages.contains(geo)) {
						// ... dann löschen wir ihn aus der Liste der Punkte, so
						// schlägt er nicht mehr auf, bis er neu gerechnet wurde
						// (Stationdispatch)!
						paintListener.images.imagePoints.remove(geo);
					}
				}
			}
		}
		return shipments;
	}

	@Override
	public void showInfos(final int pixelx, final int pixely) {
		if (trucksSelected) {
			List<ITruckPosition> positions = getTrucksFromPixel(pixelx, pixely);
			if (positions.size() > 0) {
				String tooltipText = "";
				boolean first = true;
				for (ITruckPosition pos : positions) {
					if (!first) {
						tooltipText += "\n";
					} else {
						first = false;
					}
					tooltipText += pos.getTruckText();
					// tooltipText += " - " +
					// DateTimeFormatter.formatDateTimeString(pos.getPositionDate());
					LocalDateTime positionDate = pos.getPositionDate();
					String positionDateStr = ValueFormatter.toString(ValueFormatType.DATE_TIME, positionDate, null);
					tooltipText += " - " + positionDateStr;
				}
				if (map.isDisposed()) {
					map.redraw();
					Log.warn(this, MessageFormat.format("map was disposed (Widget is Disposed) in MapControl.showInfos({0},{0})", pixelx, pixely));
				}
				if (truckToolTip == null) {
					truckToolTip = new InfoToolTip(map);
				}
				truckToolTip.setText(tooltipText);
				truckToolTip.show(new Point(pixelx + 10, pixely));
			}
		}
		if (infoShell == null) {
			infoShell = new Shell(map.getShell(), SWT.NO_TRIM | SWT.ON_TOP);
			infoShell.setLayout(new FillLayout());
			// Font font = new Font(map.getDisplay(), fontData);
			// infoShell.setFont(font);
			infoShell.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					// Die Infoshell wird disposed, wenn die Karte wieder
					// integriert wird.
					if (infoShell != null) {
						infoShell.removeDisposeListener(this);
					}
					infoShell = null;
					infoTable = null;
				}
			});
			infoTable = new Table(infoShell, SWT.V_SCROLL | SWT.FULL_SELECTION);
			// infoTable.setFont(font);

			for (ShipmentInfos info : ShipmentInfos.getShownInfos()) {
				createColumn(infoTable, info);
			}

			infoTable.setHeaderVisible(true);
			infoShell.setVisible(false);

			infoToolTip = new InfoToolTip(infoShell);

			infoTable.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (listener != null && e.item.getData() instanceof Integer) {
						listener.shipmentSelected(((Integer) e.item.getData()).intValue());
					}
					if (e.item.getData(ShipmentInfos.TOOLTIP_TEXT.name()) != null) {
						infoToolTip.setText((String) e.item.getData(ShipmentInfos.TOOLTIP_TEXT.name()));
						infoToolTip.show(new Point(0, 0));
					}
				}
			});
			infoTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					TableItem item = infoTable.getSelection()[0];
					if (listener != null && item.getData() instanceof Integer) {
						listener.shipmentDoubleClicked(((Integer) item.getData()).intValue());
					}
					if (item.getData(ShipmentInfos.TOOLTIP_TEXT.name()) != null) {
						infoToolTip.setText((String) item.getData(ShipmentInfos.TOOLTIP_TEXT.name()));
						infoToolTip.show(new Point(0, 0));
					}
				}
			});
			infoTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent e) {
					try {
						TableItem item = infoTable.getSelection()[0];
						if (listener != null && item.getData() instanceof Integer) {
							// TODO hier muss noch die Tour rausgefiltert werdern
							Integer shipmentKey = (Integer) item.getData();
							Shipment shipment = DispoModelCache.getInstance().getShipment(shipmentKey);
							if (shipment != null && shipment.getTrip() != null) {
								broker.post(DispoDispatchEventTopics.DISPATCH_SHOW_TRIP, shipment.getTrip());
								broker.post(DispoDispatchEventTopics.DISPATCH_CONTEXT_ACTIVE_TRIP, shipment.getTrip());
								context.getParent().set(DispoDispatchEventTopics.DISPATCH_CONTEXT_ACTIVE_TRUCK, shipment.getTrip().getTruck().getVehicle());
								context.getParent().set(DispoDispatchEventTopics.DISPATCH_CONTEXT_ACTIVE_TRIP, null);
								context.getParent().set(DispoDispatchEventTopics.DISPATCH_CONTEXT_ACTIVE_TRIP, shipment.getTrip());
								context.getParent().set(DispoDispatchEventTopics.DISPATCH_CONTEXT_SELECT_TRIP, null);
								context.getParent().set(DispoDispatchEventTopics.DISPATCH_CONTEXT_SELECT_TRIP, shipment.getTrip());
							}
						}
					} catch (ArrayIndexOutOfBoundsException ex) {
						Log.debug(this, "MouseDown: infottable disposed");
						hideShipmentInfos();
					}

				}
			});
			infoTable.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
						TableItem item = infoTable.getSelection()[0];
						if (listener != null && item.getData() instanceof Integer) {
							listener.shipmentDoubleClicked(((Integer) item.getData()).intValue());
						}
						if (item.getData(ShipmentInfos.TOOLTIP_TEXT.name()) != null) {
							infoToolTip.setText((String) item.getData(ShipmentInfos.TOOLTIP_TEXT.name()));
							infoToolTip.show(new Point(0, 0));
						}
						// hideShipmentInfos();
						// showInfos(pixelx, pixely);
					}
				}
			});
		}
		List<IInfoProvider> infoProviders = getShipmentsFromPixel(pixelx, pixely);

		if (infoProviders.size() > 0) {
			infoTable.removeAll();

			int totalQuantity = 0;
			for (IInfoProvider prov : infoProviders) {

				TableItem item = new TableItem(infoTable, SWT.NONE);
				Boolean allocated = (Boolean) prov.getInfos().get(ShipmentInfos.ALLOCATED.name());
				if (allocated) {
					item.setImage(Activator.getImageRegistry().get(MapImageConstants.MAPIMAGES_TRUCK_SMALL));
				}
				Object o = prov.getInfos().get("");
				if (o instanceof Shipment) {
					Shipment shipment = ((Shipment) o);
					prov.getInfos().put(ShipmentInfos.ADDRESS.name(), shipment.getAddressInfo());
					prov.getInfos().put(ShipmentInfos.TOOLTIP_TEXT.name(), getToolTipInfo(shipment));
				}
				int i = 0;
				for (ShipmentInfos infos : ShipmentInfos.getShownInfos()) {
					if (infos.getType() == Integer.class) {
						Integer intValue = (Integer) prov.getInfos().get(infos.name());
						if (intValue != null) {
							item.setText(i, NumberFormat.getIntegerInstance().format(intValue));
							if (infos == ShipmentInfos.QUANTITY) {
								totalQuantity += intValue.intValue();
							}
						} else {
							item.setText(i, "");
						}
					} else {
						Object object = prov.getInfos().get(infos.name());
						if (object != null) {
							item.setText(i, object.toString());
						} else {
							item.setText(i, "");
						}
					}
					i++;
				}
				item.setData(prov.getInfos().get(ShipmentInfos.ID.name()));
				item.setData(ShipmentInfos.TOOLTIP_TEXT.name(), prov.getInfos().get(ShipmentInfos.TOOLTIP_TEXT.name()));
			}

			infoTable.getColumn(0).setText(translationService.translate(ShipmentInfos.QUANTITY.getTranslationID(), null) + ": "
					+ NumberFormat.getIntegerInstance().format(totalQuantity));

			for (TableColumn tc : infoTable.getColumns()) {
				tc.pack();
			}

			infoTable.pack();

			infoShell.pack();

			Point display = map.toDisplay(pixelx, pixely);
			// Dieser Punkt ist der Punkt, über alle Monitore

			// Der Monitor, auf dem die Shell liegt
			Monitor monitor = map.getShell().getMonitor();
			// In den Bound steht auf dem Hauptmonitor 0,0 in x,y, auf dem
			// 2. Display die Breite und die Differenz zur Höhe. Beide Werte
			// können negativ
			// sein!
			Rectangle monitorBounds = monitor.getBounds();
			// Wir kopieren uns mal den Punkt, um dann den richtigen Punkt
			// für die Positionierung zu ermitteln
			Point monitorPoint = new Point(display.x, display.y);
			// Der Punkt liegt außerhalb des Hauptschirms
			if (display.x < 0 || display.x > monitorBounds.x) {
				// Zweiter monitor links
				monitorPoint.x = display.x - monitorBounds.x;
			}
			if (display.y < 0 || display.y > monitorBounds.y) {
				monitorPoint.y = display.y - monitorBounds.y;
			}

			// Ergibt das selbe wie monitor.getBounds(), allerdings nur,
			// wenn es keine Leisten (Windowsleiste, Telefonanlagen-Leiste
			// SuK) gibt:
			Rectangle displaySize = monitor.getClientArea();

			// Entscheiden, ob nach rechts oder links zu rendern.
			if ((monitorPoint.x + infoShell.getBounds().width) > monitor.getBounds().width) {
				display.x = display.x - infoShell.getBounds().width - 5;
			} else {
				display.x = display.x + 5;
			}
			// Die Startposition der Shell setzen
			infoShell.setLocation(display);
			// Größe ermitteln
			Point shellSize = infoShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			// Schauen, ob das auch noch auf den Monitor passt
			int diffY = displaySize.height - monitorPoint.y;
			if (shellSize.y > (diffY)) {
				// Wenn nicht, verkleinern wir die Größe, damit ein
				// Rollbalken entsteht.
				infoShell.setSize(shellSize.x, diffY);
				infoShell.layout();
				infoTable.layout();
			}

			infoShell.setVisible(true);

			infoShell.open();

			// Wir wollen die Info-Shell auch schliessen, wenn das Fenster
			// den Fokus verliert
			infoShell.addShellListener(new ShellAdapter() {
				@Override
				public void shellDeactivated(ShellEvent e) {
					hideShipmentInfos();
				}

				@Override
				public void shellClosed(ShellEvent e) {
					infoShell.setVisible(false);
					// infoToolTip.hide();
					e.doit = false;
				}
			});

			infoTable.select(0);

			Event event = new Event();
			event.item = infoTable.getItem(0);
			infoTable.notifyListeners(SWT.Selection, event);

			while (infoShell != null && infoShell.isVisible() && !infoShell.isDisposed()) {
				if (!infoShell.getDisplay().readAndDispatch()) {
					infoShell.getDisplay().sleep();
				}
			}
			infoToolTip.hide();
			// HACK: Wir müssen die aktuelle Infoshell asynchron disposen
			final Shell tmpShell = infoShell;
			infoShell = null;
			if (tmpShell != null) {
				tmpShell.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						tmpShell.dispose();
					}
				});
			}
		}
	}

	private static String getNonEmptyStringOrNull(Object o) {
		return o == null ? null : (o.toString().trim().isEmpty() ? null : o.toString().trim());
	}

	private static String getProductText(OpenDeliveryBean shipment, boolean showProductDescription) {
		String prod = null;

		if (showProductDescription) {
			String prodDesc = shipment.getItemDescription();
			if (prodDesc != null && prodDesc.trim().length() > 0) {
				prod = prodDesc;
			} else {
				// Wohl leer, nehmen wir den Matchcode
				prod = shipment.getItem().getKeyText();
			}
		} else {
			prod = shipment.getItem().getKeyText();
		}

		return prod;
	}

	/**
	 * Diese Methode liefert die ToolTipInfos zu dem ausgewählten Objekt auf der Karte: Es werden folgende Informationen angezeigt: Address.Address
	 * Address.Address2 Address.Street, Address.Streetnumber Address.PostalCode Address.City Consignee.keyText Shipment.Quantity,Item.keyText,
	 * Shipment.ValidFrom - Shipment.ValidUntil,Shipment.TimeFrom - Shipment.TimeUntil,
	 * 
	 * @param shipment
	 * @return
	 */
	private Object getToolTipInfo(OpenDeliveryBean shipment) {
		StringBuilder builder = new StringBuilder();
		Address address = shipment.getConsignee().getContact().getAddress();

		builder.append(address.getAddress());
		builder.append("\r\n");
		String address2 = getNonEmptyStringOrNull(address.getAddress2());
		if (address2 != null) {
			builder.append(address2).append("\r\n");
		}
		String street = address.getStreet();
		if (street != null) {
			builder.append(street);
			if (address.getStreetnumber() != null) {
				builder.append(" ");
				builder.append(address.getStreetnumber());
			}
			builder.append("\r\n");
		}
		builder.append(address.getPostalCode());
		builder.append(" ");
		builder.append(address.getCity());
		builder.append("\r\n");
		builder.append(translationService.translate("tCustomer", null));
		builder.append(": ");
		builder.append(shipment.getConsignee().getKeyText());
		builder.append("\r\n");

		Integer quantity;
		if (shipment.isShipment()) {
			quantity = ((Shipment) shipment).getQuantity();
		} else {
			quantity = StationTankQuantityCalculator.getPossibleDeliveryQuantity(shipment, scheduledDate);
		}
		builder.append(quantity);

		builder.append(", ");
		builder.append(getProductText(shipment, Activator.isShowProductDescription()));
		builder.append(", ");
		if (shipment.getValidFrom() != null) {
			builder.append(ValueFormatter.toString(ValueFormatType.SHORT_DATE, shipment.getValidFrom().toLocalDate(), null));
			builder.append(" - ");
		}
		if (shipment.getValidUntil() != null) {
			builder.append(ValueFormatter.toString(ValueFormatType.SHORT_DATE, shipment.getValidUntil().toLocalDate(), null));
		}

		if (shipment.isShipment()) {
			if (((Shipment) shipment).getTimeFrom() != null) {
				builder.append(", ");
				builder.append(ValueFormatter.toString(ValueFormatType.SHORT_TIME, ((Shipment) shipment).getTimeFrom(), null));
				builder.append(" - ");
			}
			if (((Shipment) shipment).getTimeUntil() != null) {
				if (((Shipment) shipment).getTimeFrom() == null) {
					builder.append(", ");
				}
				builder.append(ValueFormatter.toString(ValueFormatType.SHORT_TIME, ((Shipment) shipment).getTimeUntil(), null));
			}
		}

		// DispoRemarks werden im 11er Stand aus den Deliveries geholt!
		if (shipment.getDelivery() != null && shipment.getDelivery().getRemarks() != null) {
			Remarks remarks = shipment.getDelivery().getRemarks();
			if (remarks.getRemarks() != null && !remarks.getRemarks().isEmpty()) {
				// DispoRemarks
				builder.append("\r\n");
				builder.append(remarks.getRemarks());
			}
			if (remarks.getInternalRemarks() != null && !remarks.getInternalRemarks().isEmpty()) {
				builder.append("\r\n");
				builder.append(remarks.getInternalRemarks());
			}
		}

		if (shipment.isShipment()) {
			if (((Shipment) shipment).getTruck() != null && ((Shipment) shipment).getTrip() != null) {
				builder.append("\r\n");
				builder.append(translationService.translate("tTruck", null));
				builder.append(": ");
				builder.append(((Shipment) shipment).getTruck().getVehicle().getKeyText());
				builder.append("\r\n");
				builder.append(translationService.translate("tTrip", null));
				builder.append(": ");
				builder.append(((Shipment) shipment).getTrip().getKeyText());
				if (shipment.getScheduledDate() != null) {
					builder.append(" / ");
					builder.append(ValueFormatter.toString(ValueFormatType.SHORT_DATE, shipment.getScheduledDate(), null));
				}
			}
			if (((Shipment) shipment).getPricePerUnit() != null) {
				builder.append("\r\n");
				builder.append(translationService.translate("tOrder.Price", null));
				builder.append(": ");
				builder.append(NumberFormat.getCurrencyInstance().format(((Shipment) shipment).getPricePerUnit()));
			}
		}

		// nur für SuK
		// if (shipment.getPackageQuantity() != null) {
		// builder.append("\r\n");
		// builder.append(Messages.getString("tItem.PackageQuantity"));
		// builder.append(": ");
		// NumberFormat nf = NumberFormat.getInstance();
		// nf.setMinimumFractionDigits(3);
		// nf.setMaximumFractionDigits(3);
		// builder.append(nf.format(shipment.getPackageQuantity()));
		// }

		return builder.toString();
	}

	private List<ITruckPosition> getTrucksFromPixel(int pixelx, int pixely) {
		List<ITruckPosition> trucks = new LinkedList<>();
		int minx, maxx, miny, maxy;
		int hoverPixels = Activator.getHoverPixels();
		minx = pixelx - hoverPixels;
		maxx = pixelx + hoverPixels;
		miny = pixely - hoverPixels;
		maxy = pixely + hoverPixels;
		// Aus den aktuellen Bildern die Trucks raussuchen -> diese haben
		// aktuelle Daten!
		for (IGeocoded geo : mapImages) {
			// Zugehörige Image-Points finden -> diese können veraltete Daten
			// enthalten
			if (geo instanceof ITruckPosition && paintListener.images.imagePoints.containsKey(geo)) {
				// Die Punkte müssen aktuell sein!
				Point p = paintListener.images.imagePoints.get(geo);
				if ((p.x > minx && p.x < maxx) && (p.y > miny && p.y < maxy)) {
					// Den aktuellen Truck wollen wir anzeigen!
					trucks.add((ITruckPosition) geo);
				}
			}
		}
		return trucks;
	}

	private void createColumn(Table table, ShipmentInfos info) {
		TableColumn tc = new TableColumn(table, SWT.NONE);
		tc.setText(translationService.translate(info.getTranslationID(), null));
	}

	@Override
	public void hideShipmentInfos() {
		if (infoShell != null && !infoShell.isDisposed()) {
			infoShell.setVisible(false);
		}
		if (truckToolTip != null) {
			truckToolTip.hide();
		}
	}

	@Override
	public <E extends IGeocodableAddress> List<IGeocodedWithSource<E>> checkAddresses(final List<E> addresses) {
		if (addressDialog != null) {
			// Da hatten wir wohl 2 auf, macht nix, wir machen den offenen zu!
			addressDialog.close();
			addressDialog = null;
		}
		final List<IGeocodedWithSource<E>> foundAddresses = new LinkedList<>();
		for (final E a : addresses) {
			try {
				final List<IGeocodedAddress> geo = geoCoder.geocodeAddress(a);
				// Wir müssen das Fenster öffnen, wenn:
				// 1. die Adresse nicht gefunden wurde
				// 2. die Qualität nicht egal und und:
				// 2.1 es ein eindeutiger Hit ist, aber die Qualität zu schlecht
				// 2.2 es mehrere Treffer gibt
				if (geo.size() == 0
						|| (!geocodeIgnoreQuality && ((geo.size() == 1 && geo.get(0).getHitProbability() < geocodeIgnoreQualityValue) || geo.size() > 1))) {
					// Keiner oder mutliple Treffer
					// Form anzeigen
					inSearchMode = true;
					Runnable r = new Runnable() {
						@Override
						public void run() {
							// API sagt, sollten wir so machen...
							if (!map.isDisposed()) {
								addressDialog = new FindAddressDialog(map.getShell(), MapControl.this, translationService, geoCoder,
										FindAddressDialog.DIALOG_TAKEOVER);
								addressDialog.create();
								addressDialog.setAddress(a);
								addressDialog.setResult(geo);
								int result = addressDialog.open();
								if (result == IDialogConstants.OK_ID) {
									GeocodableAddressWithSource<E> add = new GeocodableAddressWithSource<>(a);
									IGeocodedAddress add2 = addressDialog.getSelected();
									if (add2 != null) {
										add.setMercatorX(add2.getMercatorX());
										add.setMercatorY(add2.getMercatorY());
										// Auch in diesem Fall wollen wir die
										// Koordinaten haben. Lieber 20m daneben
										// als mitten auf der Erdkugel
										add.setCoordinateX(add2.getCoordinateX());
										add.setCoordinateY(add2.getCoordinateY());
										foundAddresses.add(add);
									}
								}
								addressDialog.close();
								addressDialog = null;
								inSearchMode = false;
							}
						}
					};

					if (Thread.currentThread() == map.getDisplay().getThread()) {
						// Wir sind im UI-Thread
						r.run();
					} else {
						// IM UI Thread ausführen
						map.getDisplay().syncExec(r);
					}
				} else {
					// Hier können nur noch Listen mit 1 oder mehreren Treffern ankommen
					IGeocodedAddress foundGeo = null;
					if (geo.size() > 1) {
						// Haben wir mehr als einen, suchen wir uns den mit der besten Qualität raus
						for (IGeocodedAddress add : geo) {
							if (foundGeo == null) {
								foundGeo = add;
							} else {
								if (foundGeo.getHitProbability() < add.getHitProbability()) {
									foundGeo = add;
								}
							}
						}
					} else {
						foundGeo = geo.get(0);
					}
					GeocodableAddressWithSource<E> add = new GeocodableAddressWithSource<>(a);
					add.setMercatorX(foundGeo.getMercatorX());
					add.setMercatorY(foundGeo.getMercatorY());
					add.setCoordinateX(foundGeo.getCoordinateX());
					add.setCoordinateY(foundGeo.getCoordinateY());
					foundAddresses.add(add);
				}
			} catch (Exception e) {
				Log.err(this, e);
			}
		}
		return foundAddresses;
	}

	@Override
	public void setMapSelectionListener(IMapSelectionListener listener) {
		this.listener = listener;
	}

	@Override
	public void getNearestShipment(int x, int y) {
		IInfoProvider found = null;
		int latest = 30; // das definiert automatisch den maximalen Wert TODO konfigurierbar
		for (IGeocoded geo : paintListener.images.imagePoints.keySet()) {
			if (geo instanceof IInfoProvider) {
				boolean allocated = (Boolean) ((IInfoProvider) geo).getInfos().get(ShipmentInfos.ALLOCATED.name());
				if (allocated) {
					// Der ist schon disponiert und kann nicht nochmal
					// disponiert werden
					continue;
				}
				if (geo instanceof GeocodedImageProvider && !((GeocodedImageProvider) geo).getSelection().isSelected()) {
					// Der ist eigentlich nicht sichtbar...
					continue;
				}

				// Es werden die Koordinaten des Punktes von der Liste der
				// Shipments auf der Karte zurückgegeben.
				Point p = paintListener.images.imagePoints.get(geo);
				// Berechnung der Distanz zu den Koordianten des aktuellen geo-Objektes
				int current = getAbsoluteDistance(p, x, y);

				// Wenn aktueller Wert <= dem alten ist wird dieser umgesetzt
				if (current <= latest) {
					latest = current;
					found = (IInfoProvider) geo;
				}
			}
		}

		if (listener != null && found != null) {
			IInfoProvider prov = found;
			listener.shipmentDoubleClicked((Integer) prov.getInfos().get(ShipmentInfos.ID.name()));
		}
	}

	@Override
	public void getShipments(int x, int y, int x2, int y2) {
		IInfoProvider found = null;
		ArrayList<Integer> shipments = new ArrayList<>();
		for (IGeocoded geo : paintListener.images.imagePoints.keySet()) {
			if (found == null && geo instanceof IInfoProvider) {
				found = (IInfoProvider) geo;
			}
			if (geo instanceof IInfoProvider) {
				boolean allocated = (Boolean) ((IInfoProvider) geo).getInfos().get(ShipmentInfos.ALLOCATED.name());
				if (allocated) {
					// Der ist schon disponiert und kann nicht nochmal
					// disponiert werden
					continue;
				}
				if (geo instanceof GeocodedImageProvider && !((GeocodedImageProvider) geo).getSelection().isSelected()) {
					// Der ist eigentlich nicht sichtbar...
					continue;
				}
				Point p = paintListener.images.imagePoints.get(geo);
				if (p.x < x && p.x >= x2 && p.y < y && p.y >= y2) {
					// An dieser Stelle wird das aktuelle geo Object verwendet.
					found = (IInfoProvider) geo;
					// Hinzufügen eines Shipments zu der Liste.
					shipments.add((Integer) found.getInfos().get(ShipmentInfos.ID.name()));
				}
			}
		}
		if (shipments.size() > maxShipmentsDispatchedInArea) {
			Log.warnUser(this, Messages.getFString("msg.DispoDispatchShipmentFromArea", maxShipmentsDispatchedInArea), true);
			return;
		}
		if (listener != null && !shipments.isEmpty()) {
			listener.shipmentsSelected(shipments);
		}
	}

	private int getAbsoluteDistance(Point p, int x, int y) {
		// wir berechnen die Länge der Diagonale
		int retx = p.x - x;
		int rety = p.y - y;
		double diagDouble = Math.sqrt((retx * retx) + (rety * rety));
		int diagInt = (int) Math.round(diagDouble);
		return diagInt;
	}

	@Override
	public void setImageFilter(IImageFilter filter) {
		this.imageFilter = filter;
	}

	@Override
	public boolean isInSearchMode() {
		return inSearchMode;
	}

	@Override
	public void setSearchMode(boolean inSearchMode) {
		this.inSearchMode = inSearchMode;
	}

	@Override
	public MapViewPointController getController() {
		return controller;
	}

	@Override
	public IGeocodedWithSource<IAddress> openGeocoderForm(Shell parent, IAddress address) {
		try {
			List<IGeocodedAddress> geo = this.geoCoder.geocodeAddress(address);

			inSearchMode = true;
			addressDialog = new FindAddressDialog(parent, this, this.translationService, this.geoCoder, FindAddressDialog.DIALOG_TAKEOVER);
			addressDialog.create();
			addressDialog.setAddress(address);
			addressDialog.setResult(geo);
			int retCode = addressDialog.open();
			inSearchMode = false;
			if (retCode == Dialog.OK) {
				GeocodedAddressWithSource<IAddress> add = new GeocodedAddressWithSource<>(address);
				IGeocodedAddress add2 = addressDialog.getSelected();
				if (add2 != null) {
					add.setMercatorX(add2.getMercatorX());
					add.setMercatorY(add2.getMercatorY());
					// Auch in diesem Fall wollen wir die Koordinaten haben.
					// Lieber 20m daneben als mitten auf der Erdkugel
					add.setCoordinateX(add2.getCoordinateX());
					add.setCoordinateY(add2.getCoordinateY());
					return add;
				}
			}
		} catch (Exception e) {
			Log.err(this, e);
			inSearchMode = false;
		}
		addressDialog = null;
		return null;
	}

	@Override
	public void closeGeocoderForm() {
		if (addressDialog != null) {
			addressDialog.close();
		}
	}

	@Override
	public IGeocodedWithSource<IAddress> geocodeAddress(IAddress address) {
		try {
			return openGeocoderForm(map.getShell(), address);
		} catch (Exception e) {
			Log.err(this, e);
		}
		return null;
	}

	@Override
	public ICalculatedTripInformation getCalculatedTripInformation(List<IGeocoded> stopLocations, String shapeName) {
		return routing.getCalculatedTripInformation(stopLocations, shapeName);
	}

	@Override
	public void setDrawSelectionRectangle(Rectangle rec) {
		this.rectangle = rec;
	}

	@Override
	public void setShowAllocatedForObject(boolean show) {
		this.showAllocatedforObject = show;
		this.showAllocatedShipments = show;
		redrawMap(false, true);
	}

	@Override
	public void setShowAllocated(boolean show) {
		this.showAllocatedShipments = show;
		redrawMap(false, true);
	}

	@Override
	public void setDropTargetListener(Transfer[] types, DropTargetListener listener) {
		this.transferTypes = types;
		this.dropTargetListener = listener;
	}

	@Override
	public boolean isExtracted() {
		return extracted;
	}

	@Override
	public List<IGeocoded> getOptimizedTrip(List<IGeocoded> tripList, boolean fixedStart, boolean fixedEnd, Trip trip) {
		return routing.getOptimizedRoute(tripList, fixedStart, fixedEnd, trip);
	}

	@Override
	public void setTripKey(int keyLong) {
		this.tripKey = keyLong;

	}

	@Override
	public void resetTripKey() {
		this.tripKey = 0;
		this.tripKeyStr = "";
	}

	@Override
	public void setTripKeyStr(String keyLong) {
		this.tripKeyStr = keyLong;

	}

	@Override
	public void resetTripKeyStr() {
		this.tripKeyStr = "0";
	}

	@Override
	public void showDepot(boolean b) {
		tdepotsSelected = b;
		redrawMap(false, true);
	}

	@Override
	public void showVehicle(boolean b) {
		trucksSelected = b;
		redrawMap(false, true);
	}

	@Override
	public void removeImage(IGeocoded truckPos) {
		for (IGeocoded iGeocoded : mapImages) {
			if (iGeocoded.equals(truckPos)) {
				mapImages.remove(iGeocoded);
				return;
			}
		}
	}

	@Override
	public void removeImage(GeocodedImageProvider prov) {
		if (prov != null) {
			for (IGeocoded iGeocoded : mapImages) {
				if (iGeocoded instanceof GeocodedImageProvider) {
					if (((Integer) (((GeocodedImageProvider) iGeocoded).getInfos().get(ShipmentInfos.ID.name())))
							.equals(prov.getInfos().get(ShipmentInfos.ID.name()))) {
						mapImages.remove(iGeocoded);
						return;
					}
				} else if (iGeocoded.equals(prov)) {
					mapImages.remove(iGeocoded);
					return;
				}
			}
		}
	}

	@Override
	public void extractDispatchMap() {
		extractMap();
	}

	@Override
	public void setDrawTrip(boolean b) {
		this.drawTrip = b;
	}

	@Override
	public List<IGeocoded> getOptimizedTrip(List<IGeocoded> tripList, boolean fixedStart, boolean fixedEnd, String trip) {
		return null;
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
	public UISynchronize getUISynchronize() {
		return sync;
	}
}