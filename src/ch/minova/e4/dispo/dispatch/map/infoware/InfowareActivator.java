package ch.minova.e4.dispo.dispatch.map.infoware;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class InfowareActivator implements BundleActivator {
	public static final String PLUGIN_ID = "ch.minova.e4.dispo.dispatch.map.infoware";

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/**
	 * The constructor
	 */
	public InfowareActivator() {}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		context = bundleContext;
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		context = null;
	}
}