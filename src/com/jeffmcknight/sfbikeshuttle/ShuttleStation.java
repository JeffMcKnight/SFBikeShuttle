/**
 * 
 */
package com.jeffmcknight.sfbikeshuttle;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

// **********************************************************************************************
// ******************** Class - ShuttleStation ********************
/**
 * This class describes bike shuttle station stops
 * @author jeffmcknight
 *
 */
public class ShuttleStation 
{
	private int riderCount;
//	private int seatsLeft;
	private float distanceFromRider;
	Marker marker;

	// ******************** ShuttleStation() ********************
	public ShuttleStation()
	{
		super();
	}

	// ******************** ShuttleStation(LatLng) ********************
	public ShuttleStation(LatLng latlngInitial)
	{
		super();
		marker.setPosition(latlngInitial);
	}

	// ******************** ShuttleStation(Marker) ********************
	public ShuttleStation(Marker markerInitial)
	{
		marker = markerInitial;
	}

	// ******************** getDistanceFromRider() ********************
	protected float getDistanceFromRider()
	{
		return distanceFromRider;
	}

	// ******************** getMarker() ********************
	protected Marker getMarker()
	{
		return marker;
	}

	// ******************** getRiderCount() ********************
	protected int getRiderCount()
	{
		return riderCount;
	}

	// ******************** setRiderCount() ********************
	protected void setRiderCount(int riderCount)
	{
		this.riderCount = riderCount;
	}

	// ******************** setMarker() ********************
	protected void setMarker(Marker marker)
	{
		this.marker = marker;
	}

	// ******************** setDistanceToRider() ********************
	public void setDistanceFromRider(float f)
	{
		distanceFromRider = f;
	}


} // END ******************** Class - ShuttleStation ********************

