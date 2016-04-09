/*
 * Copyright (C) 2016 Liza Lukicheva
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package me.jtalk.android.geotasks.activity.item;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.TextView;

import java.util.Calendar;

import me.jtalk.android.geotasks.R;
import me.jtalk.android.geotasks.activity.MakeTaskActivity;
import me.jtalk.android.geotasks.source.Event;
import me.jtalk.android.geotasks.source.EventsSource;
import me.jtalk.android.geotasks.util.CoordinatesFormat;
import me.jtalk.android.geotasks.util.CursorHelper;
import me.jtalk.android.geotasks.util.TimeFormat;

public class EventElementAdapter extends CursorTreeAdapter {
	private final int INACTIVE_EVENT;

	public EventElementAdapter(Context context) {
		super(null, context, true);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			INACTIVE_EVENT = context.getResources().getColor(R.color.inactive_event, context.getTheme());
		} else {
			INACTIVE_EVENT = context.getResources().getColor(R.color.inactive_event);
		}
	}

	@Override
	protected Cursor getChildrenCursor(Cursor groupCursor) {
		// Information about group object must be displayed.
		// Object groupCursor cannot be placed by itself (all data from groupCursor -
		// data for other cursors - will be displayed), therefore clone
		// object must be created and returned.
		return CursorHelper.clone(groupCursor, 1);
	}

	@Override
	protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(R.layout.item_event, parent, false);
	}

	@Override
	protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
		Event event = EventsSource.extractEvent(cursor);
		TextView titleView = (TextView) view.findViewById(R.id.event_element_title);
		titleView.setText(event.getTitle());

		if (!event.isActive(Calendar.getInstance())) {
			titleView.setTextColor(INACTIVE_EVENT);
		}
	}

	@Override
	protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(R.layout.item_event_expanded, parent, false);
	}

	@Override
	protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
		Event event = EventsSource.extractEvent(cursor);

		view.setOnClickListener(v -> {
			Intent intent = new Intent(context, MakeTaskActivity.class);
			intent.putExtra(MakeTaskActivity.INTENT_EDIT_TASK, event.getId());
			context.startActivity(intent);
		});

		TextView descriptionView = (TextView) view.findViewById(R.id.item_event_expanded_descripion);
		descriptionView.setText(event.getDescription());

		TextView startTimeView = (TextView) view.findViewById(R.id.event_element_start_time);
		startTimeView.setText(TimeFormat.formatDateTime(context, event.getStartTime()));

		TextView locationView = (TextView) view.findViewById(R.id.event_element_location);
		locationView.setText(CoordinatesFormat.prettyFormat(event.getCoordinates()));

		if (!event.isActive(Calendar.getInstance())) {
			descriptionView.setTextColor(INACTIVE_EVENT);
			startTimeView.setTextColor(INACTIVE_EVENT);
			locationView.setTextColor(INACTIVE_EVENT);
		}
	}
}