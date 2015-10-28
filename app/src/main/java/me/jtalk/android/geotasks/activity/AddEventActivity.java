package me.jtalk.android.geotasks.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Calendar;

import me.jtalk.android.geotasks.R;
import me.jtalk.android.geotasks.source.EventsSource;
import me.jtalk.android.geotasks.util.GeoPointFormat;
import me.jtalk.android.geotasks.util.PermissionDependentTask;
import me.jtalk.android.geotasks.util.TasksChain;


public class AddEventActivity extends BaseActivity {
	private static final String TAG = AddEventActivity.class.getName();

	private TasksChain<PermissionDependentTask> addEventChain;
	private TasksChain<PermissionDependentTask> openLocationPickActivityChain;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_event);

		addEventChain = new TasksChain<PermissionDependentTask>()
				.add(makeTask(this::addEvent, Manifest.permission.WRITE_CALENDAR));

		openLocationPickActivityChain = new TasksChain<PermissionDependentTask>()
				.add(makeTask(() -> {
							Intent intent = new Intent(this, LocationPickActivity.class);
							TextView locationText = (TextView) findViewById(R.id.add_event_location_coordinates_text);
							String locationString = locationText.getText().toString();
							if (!locationString.isEmpty()) {
								IGeoPoint geoPoint = GeoPointFormat.parse(locationString);
								intent.putExtra(LocationPickActivity.INTENT_EXTRA_EDIT, true);
								intent.putExtra(LocationPickActivity.INTENT_EXTRA_LATITUDE, geoPoint.getLatitude());
								intent.putExtra(LocationPickActivity.INTENT_EXTRA_LONGITUDE, geoPoint.getLongitude());
							}

							startActivityForResult(intent, LocationPickActivity.INTENT_LOCATION_PICK);
						},
						Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE,
						Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_add_event, menu);
		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}

		switch (requestCode) {
			case LocationPickActivity.INTENT_LOCATION_PICK:
				onLocationResult(data);
				break;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] values) {
		processPermissionRequestResult(addEventChain, requestCode, permissions, values);
	}

	@Override
	protected void onNeededPermissionDenied() {
		Toast.makeText(this, R.string.toast_event_creation_no_permission, Toast.LENGTH_LONG).show();
	}

	/**
	 * This method is called on menu_add_event.menu_action_add_event_save click.
	 *
	 * @param menuItem
	 */
	public void onAddCalendarClick(MenuItem menuItem) {
		processChain(addEventChain);
	}

	/**
	 * This method is called on activity_add_event.add_event_location_pick_button.
	 *
	 * @param view
	 */
	public void showLocationActivity(View view) {
		openLocationPickActivityChain.reset();
		processChain(openLocationPickActivityChain);
	}

	/**
	 * This method is called on activity_add_event.add_event_time_text click.
	 *
	 * @param view
	 */
	public void showTimePickerDialog(View view) {
		TextView textView = (TextView) view;
		Calendar calendar = Calendar.getInstance();

		try {
			String timeText = textView.getText().toString();
			if (!timeText.isEmpty()) {
				calendar.setTime(DateFormat.getTimeInstance().parse(timeText));
			}
		} catch (ParseException exception) {
			Log.w(TAG, "Time value from time text field cannot be parsed", exception);
		}

		new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
			Calendar picked = Calendar.getInstance();
			picked.set(Calendar.HOUR_OF_DAY, hourOfDay);
			picked.set(Calendar.MINUTE, minute);
			textView.setText(DateFormat.getTimeInstance().format(picked.getTime()));
		}, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
	}

	/**
	 * This method is called on activity_add_event.add_event_date_text click.
	 *
	 * @param view
	 */
	public void showDatePickerDialog(View view) {
		TextView textView = (TextView) view;
		Calendar calendar = Calendar.getInstance();

		try {
			String dateText = textView.getText().toString();
			if (!dateText.isEmpty()) {
				calendar.setTime(DateFormat.getDateInstance().parse(dateText));
			}
		} catch (ParseException exception) {
			Log.w(TAG, "Date value from date text field cannot be parsed", exception);
		}

		new DatePickerDialog(this, (dateView, year, monthOfYear, dayOfMonth) -> {
			Calendar picked = Calendar.getInstance();
			picked.set(year, monthOfYear, dayOfMonth);
			textView.setText(DateFormat.getDateInstance().format(picked.getTime()));
		}, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
	}

	private void onLocationResult(Intent data) {
		TextView locationText = (TextView) findViewById(R.id.add_event_location_coordinates_text);
		double longitude = data.getDoubleExtra(LocationPickActivity.INTENT_EXTRA_LONGITUDE, 0.0d);
		double latitude = data.getDoubleExtra(LocationPickActivity.INTENT_EXTRA_LATITUDE, 0.0d);

		IGeoPoint geoPoint = new GeoPoint(latitude, longitude);
		locationText.setText(GeoPointFormat.formatSimple(geoPoint));
	}

	private void addEvent() {
		TextView titleText = (TextView) findViewById(R.id.add_event_name_text);
		TextView descriptionText = (TextView) findViewById(R.id.add_event_description_text);
		TextView locationText = (TextView) findViewById(R.id.add_event_location_coordinates_text);

		String eventTitle = titleText.getText().toString();
		String eventDescription = descriptionText.getText().toString();
		String location = locationText.getText().toString();
		long startTime = this.getStartTime();
		long endTime = EventsSource.DEFAULT_END_TIME;

		getEventsSource().addEvent(eventTitle, eventDescription, location, startTime, endTime);

		finish();
	}

	private long getStartTime() {
		Calendar dateCalendar = parseFromTextView(R.id.add_event_date_text, DateFormat.getDateInstance());
		Calendar timeCalendar = parseFromTextView(R.id.add_event_time_text, DateFormat.getDateTimeInstance());

		if (dateCalendar == null || timeCalendar == null) {
			return EventsSource.DEFAULT_START_TIME;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.set(dateCalendar.get(Calendar.YEAR),
				dateCalendar.get(Calendar.MONTH),
				dateCalendar.get(Calendar.DAY_OF_MONTH),
				timeCalendar.get(Calendar.HOUR_OF_DAY),
				timeCalendar.get(Calendar.MINUTE));
		return calendar.getTimeInMillis();
	}

	private Calendar parseFromTextView(int viewId, DateFormat format) {
		String calendarStr = ((TextView) findViewById(viewId)).getText().toString();
		if (calendarStr.isEmpty()) {
			return null;
		}

		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(format.parse(calendarStr));
			return calendar;
		} catch (ParseException exception) {
			Log.w(TAG, MessageFormat.format("Parsing event start time values {0} from view {1} failed", calendarStr, viewId), exception);
			return null;
		}
	}
}
