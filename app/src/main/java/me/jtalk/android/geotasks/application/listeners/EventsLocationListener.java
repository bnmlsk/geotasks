package me.jtalk.android.geotasks.application.listeners;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.Setter;
import me.jtalk.android.geotasks.R;
import me.jtalk.android.geotasks.application.Settings;
import me.jtalk.android.geotasks.application.Notifier;
import me.jtalk.android.geotasks.location.TaskCoordinates;
import me.jtalk.android.geotasks.source.Event;
import me.jtalk.android.geotasks.source.EventsSource;
import me.jtalk.android.geotasks.util.Logger;

@NoArgsConstructor
public class EventsLocationListener implements LocationListener {
	private static final Logger LOG = new Logger(EventsLocationListener.class);

	public static final long MIN_TIME = 2000;
	public static final float MIN_DISTANCE = 1;

	@Setter(onParam = @__(@NonNull))
	private EventsSource eventsSource;

	@Setter(onParam = @__(@NonNull))
	private Notifier notifier;

	/**
	 * Distance between to event location when reminder must be enabled.
	 */
	@Setter
	private float distanceToAlarm = Settings.DEFAULT_DISTANCE_TO_ALARM;

	/**
	 * MenuItem that toggles geo location listening.
	 */
	@Setter(onParam = @__(@NonNull))
	private MenuItem menuItem;

	private boolean isEnabled = false;

	@Override
	public void onLocationChanged(Location location) {
		LOG.debug("Location update ({0})", location);

		Calendar currentTime = Calendar.getInstance();
		TaskCoordinates currentCoordinates = new TaskCoordinates(location);
		List<Event> events = eventsSource.getActive(currentTime);

		for (Event event : events) {
			double distance = event.getCoordinates().distanceTo(currentCoordinates);
			if (distance <= distanceToAlarm) {
				LOG.debug("Notify about event {0} (distance {1}, current coordinates {2})",
						event.getTitle(), distance, currentCoordinates);

				notifier.onEventIsNear(event);
			}
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	public void toggleGeoListening(Context context) throws SecurityException {
		menuItem.setChecked(isEnabled);

		if (isEnabled) {
			onEnabled(context);
		} else {
			onDisabled(context);
		}
	}

	private void onEnabled(Context context) throws SecurityException {
		LocationManager locationManager = getLocationMamager(context);
		locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				EventsLocationListener.MIN_TIME,
				EventsLocationListener.MIN_DISTANCE,
				this);

		menuItem.setIcon(R.drawable.ic_gps_fixed_black_48dp);

		Toast.makeText(context, R.string.toast_geolistening_enabled, Toast.LENGTH_SHORT).show();

		LOG.debug("Geo listening enabled");
	}

	private void onDisabled(Context context) throws SecurityException {
		LocationManager locationManager = getLocationMamager(context);
		locationManager.removeUpdates(this);

		menuItem.setIcon(R.drawable.ic_gps_off_black_48dp);

		Toast.makeText(context, R.string.toast_geolistening_disnabled, Toast.LENGTH_SHORT).show();

		LOG.debug("Geo listening disabled");
	}

	public boolean tryToggle(boolean enabled) {
		if (this.isEnabled == enabled) {
			return false;
		}

		this.isEnabled = enabled;
		return true;
	}

	private LocationManager getLocationMamager(Context context) {
		return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}
}
