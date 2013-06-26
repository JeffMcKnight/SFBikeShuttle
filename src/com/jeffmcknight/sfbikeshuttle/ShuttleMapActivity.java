package com.jeffmcknight.sfbikeshuttle;

//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.lang.ref.WeakReference;
//import java.security.Provider;
import java.util.Calendar;
import java.util.Date;


import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
//import twitter4j.GeoLocation;
import twitter4j.ResponseList;
import twitter4j.Status;
//import twitter4j.Twitter;
import twitter4j.TwitterAdapter;
import twitter4j.TwitterException;
//import twitter4j.TwitterFactory;
import twitter4j.TwitterListener;
import twitter4j.TwitterMethod;
//import twitter4j.auth.AccessToken;
//import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
//import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

// ******************** Class - ShuttleMapActivity ********************
public class ShuttleMapActivity 
	extends Activity 
	implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener
{


	// *** Class CONSTANTS ***
	private static final String TAG = ShuttleMapActivity.class.getSimpleName();
	private static final boolean USE_MOCK_LOCATION = false;

	// flag for status when we try to retrieve the last 20 (call getUserTimeline() )
	public boolean recentStatusRetrieved; //set true when gotUserTimeline is called; set false when 
	
	public static final int SHUTTLE_CAPACTITY = 14;
    protected static final int TIMELINE_STATUS = 11;

    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = 60 * ONE_SECOND;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    private static final long ONE_DAY = 24 * ONE_HOUR;
    
	private static final String SHUTTLE_IS_FULL = "Shuttle is FULL";
	private static final String BIKE_SHUTTLE_TITLE = "Bike Shuttle";
	private static final String SHUTTLE_RIDER_TITLE = "You are here";
	private static final String MACARTHUR_STATION_TITLE = "Macarthur Station";
	private static final String SAN_FRANCISCO_STATION_TITLE = "San Francisco Station";
	private static final String INITIAL_SNIPPET = "Tap to Refresh";

	static final LatLng MACARTHUR_STATION = new LatLng(37.827639,-122.266564);
	static final LatLng SF_STATION = new LatLng(37.789224,-122.391719);
	static final LatLng TEMESCAL_LIBRARY = new LatLng(37.837898,-122.262085);
	static final LatLng CALTRAIN_4TH_ST = new LatLng(37.7768236,-122.3950076); 
	static final LatLng TOLL_PLAZA = new LatLng(37.824829,-122.313852); 
	static final LatLng MIDSPAN = new LatLng(
			0.5 * (MACARTHUR_STATION.latitude + SF_STATION.latitude),
			0.5 * (MACARTHUR_STATION.longitude + SF_STATION.longitude));

	private static final String GOOGLEMAP_URL = "https://maps.googleapis.com/maps/api/staticmap?center="
			+ MIDSPAN.latitude + "," 
			+ MIDSPAN.longitude
			+ "&zoom=12&size=640x640&markers=label:S%7C";

	private LatLng latLngRiderMock = TOLL_PLAZA;
	private LatLng latLngShuttleMock = TOLL_PLAZA;
	private LatLng latLngShuttle;
	private LatLng latLngShuttleRider;
	

	// *** Class field declarations ***
	int intMacarthurCount;
	int intTransbayCount;
	int intRiderCount = SHUTTLE_CAPACTITY/2;

	//	Declare GoogleMap objects. Used to set up map.
	private GoogleMap map;
	private OnInfoWindowClickListener listenerStationWindowClick;
	private OnMarkerClickListenerImplementation listenerMarkerClick;
	public Marker markerMacarthur;
	public Marker markerShuttle;
	private Marker markerShuttleRider;
	public Marker markerSF;
	public Marker markerCurrent;
	
	// Declare shuttle stops
	protected ShuttleStation stationCurrent;
	protected ShuttleStation stationMacarthur;
	protected ShuttleStation stationSF;

	// Declare Toast for network warnings
	protected Toast toastNetworkWarning;
	private int durationToast = Toast.LENGTH_LONG;
	private static final CharSequence NO_NETWORK_WARNING = "     Could not update shuttle location.\nPlease check your network connection.";
	private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	
    // Declare Twitter objects.  Used to post to Twitter feed.
	private String stringLatestTweet;
	private AsyncTwitter asyncTwitter;
    Status status;
    public ResponseList<Status> recentStatuses;

    // Declare DialogFragment objects. Used to input info like # riders waiting at station.
	private DialogFragment dialogRiderQueue;

	// Declare objects for Android framework location API
	// TODO [JAM] remove unneeded objects and switch to Google Location Services API (below) 
	private LocationManager locationManager;
	private Criteria criteriaLocactionProvider;
	private String providerBest;
	protected Location locationRider;
	protected LocationListenerImplementation locationListenerRider;
	MockLocationProvider mock;
	
	// Declare objects for Google Location Services API
	private boolean booleanLocationClientConnected;
	private LocationClient mLocationClient;
	private Location mCurrentLocation;

	private Context context;
	
//	public String stringTitleMarkerCurrent;

	
	// END *** Class field declarations ***
	

	// ******************** onCreate() ********************
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_shuttle_map);
		context = getApplicationContext();

		intMacarthurCount = 0;
		intTransbayCount = 0;
		
		// Configure network warning Toast
		toastNetworkWarning = Toast.makeText(context, NO_NETWORK_WARNING, durationToast);
		toastNetworkWarning.setGravity(Gravity.CENTER_VERTICAL, 0, 0);


		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

		
		// Check whether we can connect to GooglePlay services for LocationClient
		servicesConnected();
		// Create a new location client, using the enclosing class to handle callbacks.
        mLocationClient = new LocationClient(this, this, this);

        // Create/initialize async Twitter object to access @BikeShuttle feed
        initializeTwitter(null);

        
        /*
        
		// Create LocationProvider for best location
		providerBest = chooseLocationProvider();
		// Create location listener to get location updates for rider
		locationListenerRider = new LocationListenerImplementation();
		// Get most recent rider location using LocationProvider
		
		// Assign rider to mock location for testing
		if (USE_MOCK_LOCATION)
		{
			locationRider = assignMockLocation(latLngRiderMock);
		    setupMockLocation();
		    Toast.makeText(this, "Using Mock Location", Toast.LENGTH_SHORT).show();
		}
		else 
		{
			locationRider = locationManager.getLastKnownLocation(providerBest);
		}
		latLngShuttleRider = new LatLng(locationRider.getLatitude(), locationRider.getLongitude());
		
		Log.i(TAG, "onCreate() - locationRider.getLatitude(): " + locationRider.getLatitude());
		Log.i(TAG, "onCreate() - locationRider.getLongitude(): " + locationRider.getLongitude());
		
*/		// create Markers for shuttle and stations and show on map
		createMapMarkers();
		
//		markerShuttle.setPosition(new LatLng(locationRider.getLatitude(), locationRider.getLongitude()));

//		mCurrentLocation = mLocationClient.getLastLocation();
////		stationCurrent = getClosestStation(locationRider);
//		stationCurrent = getClosestStation(mCurrentLocation);
//		stationCurrent.getMarker().showInfoWindow();		

		// Set the current station to the closest station and show its InfoWindow
	    new ShowClosestStationAsyncTask().execute();
		

		// Move the camera instantly to MIDSPAN with a zoom of 15.
		// MIDSPAN is halfway between SF and Macarthur stations
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(MIDSPAN, 15));

		// Zoom in , animating the camera.
		map.animateCamera(CameraUpdateFactory.zoomTo(11), 2000, null);

		// Set up the Marker listener so we can retrieve updated seat counts when user taps the Marker
		listenerMarkerClick = new OnMarkerClickListenerImplementation();
		map.setOnMarkerClickListener(listenerMarkerClick);

		// Set up the InfoWindow listener so we can tweet out updated rider counts when the user taps the InfoWindow
		listenerStationWindowClick = new OnInfoWindowClickListenerImplementation();
		map.setOnInfoWindowClickListener(listenerStationWindowClick);

	    // Get the 20 most recent statuses posted from the @BikeShuttle feed.
	    // Pass the title of the current marker to the AsyncTask so it can get the rider count for the current station.
//	    new UpdateMarkerAsyncTask().execute(markerCurrent.getTitle(), markerCurrent.getSnippet());

	} 	// END ******************** onCreate() ********************


	/**
	 * 
	 */
	protected void setupMockLocation()
	{
		mock = new MockLocationProvider(LocationManager.NETWORK_PROVIDER, this);
		
		//Set test location
		mock.pushLocation(-12.34, 23.45);
 
		LocationManager locMgr = (LocationManager) 
		   getSystemService(LOCATION_SERVICE);
		LocationListener lis = new LocationListener() 
		{
		  public void onLocationChanged(Location location) 
		  {
		      //You will get the mock location
		  }
		  //...

		@Override
		public void onProviderDisabled(String provider)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String provider, int status,
				Bundle extras)
		{
			// TODO Auto-generated method stub
			
		}
		};
 
		locMgr.requestLocationUpdates(
		  LocationManager.NETWORK_PROVIDER, 1000, 1, lis);
	}


	// ******************** onCreateOptionsMenu() ********************
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.shuttle_map, menu);
		return true;
	}

	// ******************** onStart() ********************
    /*
     * Connect to LocationClient here, so that LocationServices maintains the 
     * current location while activity is fully visible.
     */
    @Override
    protected void onStart() 
    {
        super.onStart();
        // Connect the client if it is not already connected or connecting.
        if (!mLocationClient.isConnected() && !mLocationClient.isConnecting())
		{
        	mLocationClient.connect();
		}
    }

    
	// ******************** onStop() ********************
    /*
     * Disconnect the LocationClient here so LocationServices stops 
     * maintaining the current location when app is not visible. 
     * This will help save battery power.
     */
    @Override
    protected void onStop() 
    {
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
        super.onStop();
    }

	// ******************** onDestroy() ********************
	/* 
	 *  Cleans up Activity before it is destroyed.
	 *  Shutdown & null out Twitter object.
	 */
	@Override
	protected void onDestroy()
	{
		try
		{
			asyncTwitter.shutdown();
			asyncTwitter = null;
			Log.i(TAG, "onDestroy() - asyncTwitter.shutdown");
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		super.onDestroy();
	}

	

	// ******************** onConnectionFailed ********************
			/*
		     * Called by Location Services if the attempt to
		     * Location Services fails.
		     */
			@Override
			public void onConnectionFailed(ConnectionResult connectionResult)
			{
		        /*
		         * Google Play services can resolve some errors it detects.
		         * If the error has a resolution, try sending an Intent to
		         * start a Google Play services activity that can resolve
		         * error.
		         */
		        if (connectionResult.hasResolution()) 
		        {
		            try 
		            {
		                // Start an Activity that tries to resolve the error
		                connectionResult.startResolutionForResult(
		                        this,
		                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
		                /*
		                * Thrown if Google Play services canceled the original
		                * PendingIntent
		                */
		            } 
		            catch (IntentSender.SendIntentException e) 
		            {
		                // Log the error
		                e.printStackTrace();
		            }
		        } 
		        else 
		        {
		            /*
		             * If no resolution is available, display a dialog to the
		             * user with the error.
		             */
	//	            showErrorDialog(connectionResult.getErrorCode());
		        	toastNetworkWarning.show();
		        }
		    }


	// ******************** onConnected() ********************
	/*
	 * Called by Location Services when the request to connect the
	 * client finishes successfully. At this point, you can
	 * request the current location or start periodic updates
	 */
	@Override
	public void onConnected(Bundle dataBundle) 
	{
		booleanLocationClientConnected = true;
		// Get rider's most recent location
        mCurrentLocation = mLocationClient.getLastLocation();
        
        // TODO [JAM] - Remove when done testing. 
        // Display the connection status. 
//	    Toast.makeText(this, "Connected to Google Location Services", Toast.LENGTH_SHORT).show();
	}


	// ******************** onDisconnected() ********************
	/*
	 * Called by Location Services if the connection to the
	 * location client drops because of an error.
	 */
	@Override
	public void onDisconnected() 
	{
		booleanLocationClientConnected = true;
	    // Display the connection status
	    Toast.makeText(this, "Disconnected from Google Location Services.\nPlease restart app to re-connect.",
	            Toast.LENGTH_SHORT).show();
	}


	// ******************** onActivityResult() ********************
    /*
     * Handle results returned to the MapActivity
     * by Google Play services
     */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		  // Decide what to do based on the original request code
        switch (requestCode) 
        {
//            ...
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) 
                {
                    case Activity.RESULT_OK :
                    /*
                     * Try the request again
                     */
//                    ...
                    break;
                }
//            ...
        }
	}


	// ******************** assignMockLocation() ********************
	/**
	 * @param latLngMock - mock latitude/longitude pair assigned to mock location
	 * @return - mock location for testing
	 * 
	 */
	protected Location assignMockLocation(LatLng latLngMock)
	{
		Location locationMock;
	
		
		Calendar c = Calendar.getInstance();
		// year, month, day, hourOfDay, minute
		c.set(2013, Calendar.JUNE, 8, 12, 00);
		long millis = c.getTimeInMillis();
		
		// Set up test provider so we can set mock locations
		locationMock = new Location(providerBest);
		locationMock.setLatitude(latLngMock.latitude);
		locationMock.setLongitude(latLngMock.longitude);
		locationMock.setAccuracy(1000);
		locationMock.setTime(millis);
		
		Log.i(TAG, "assignMockLocation() - locationMock.getLatitude(): " + locationMock.getLatitude());
		Log.i(TAG, "assignMockLocation() - locationMock.getLongitude(): " + locationMock.getLongitude());
		
		return locationMock;
	}


	// ******************** createMapMarkers() ********************
	/** Set up map markers for shuttle and stations
	 * 
	 */
	protected void createMapMarkers()
	{
		markerMacarthur = map.addMarker(new MarkerOptions()
		.position(MACARTHUR_STATION)
		.title(MACARTHUR_STATION_TITLE)
		.snippet(INITIAL_SNIPPET)
		.visible(true)
				);
		markerSF = map.addMarker(new MarkerOptions()
		.position(SF_STATION)
		.title(SAN_FRANCISCO_STATION_TITLE)
		.snippet(INITIAL_SNIPPET)
		.visible(true)
				);
		markerShuttle = map.addMarker(new MarkerOptions()
		.position(latLngShuttleMock)
		.title(BIKE_SHUTTLE_TITLE)
		.snippet(INITIAL_SNIPPET)
		.visible(false) // hide shuttle marker for now; we will show it after we update position
		.icon(BitmapDescriptorFactory.fromResource(R.drawable.shuttle_marker))
		.anchor(0.5f, 0.5f)  // center icon on current marker location
				);
/*		markerShuttleRider = map.addMarker(new MarkerOptions()
		.position(latLngShuttleRider)
		.title(SHUTTLE_RIDER_TITLE)
		.snippet("Tap to Share Location")
//		.visible(false) // hide shuttle marker for now; we will show it after we update position
//		.icon(BitmapDescriptorFactory.fromResource(R.drawable.shuttle_marker))
//		.anchor(0.5f, 0.5f)  // center icon on current marker location
				);
*/		
		map.setMyLocationEnabled(true);

		// Create stations and set current station to SF by default
		stationMacarthur = new ShuttleStation(markerMacarthur);
		stationSF = new ShuttleStation(markerSF);
		//		stationCurrent = stationSF;

		// set the current map Marker to the SF station and show its InfoWindow
		//		setMarkerCurrent(markerSF);
		//		markerCurrent.showInfoWindow();
	}	// END ******************** createMapMarkers() ********************


	// ******************** generateTweetString() ********************
	/**
	 * @param markerStation
	 * @param intRiderQueue
	 * @return stringTweetText - text to post to Twitter feed
	 */
	protected String generateTweetString(Marker markerStation,
			int intRiderQueue)
	{
		int intSpotsLeft = SHUTTLE_CAPACTITY - intRiderQueue;
		String stringTweetText;
		String stringRider;
		String stringSpotsLeft;
		
		if (intRiderQueue == 1)  
			{ stringRider = "1 rider queued. "; } 
		else  
			{ 	stringRider = intRiderQueue + " riders queued. "; }
	
		if (intSpotsLeft == 1)  
			{ stringSpotsLeft = "1 seat left."; } 
		else  
			{ stringSpotsLeft = intSpotsLeft + " seats left."; }
		
		if (intRiderQueue >= SHUTTLE_CAPACTITY)
			{ stringSpotsLeft = " " + SHUTTLE_IS_FULL; }
		
		stringTweetText = markerStation.getTitle().toString() + ": " + stringRider + stringSpotsLeft
				+ "\n" 
//				+ "{" + markerStation.getTitle().toString().charAt(0) + ":" + intRiderQueue + "}"
				+ SystemClock.elapsedRealtime();
		return stringTweetText;
	}	// END ******************** generateTweetString()  ********************

	
	// ******************** getClosestStation() ********************
		/**
		 * @param locationUser - current location of user/rider
		 * @return - shuttle station closest to rider
		 * 
		 */
		protected ShuttleStation getClosestStation(Location locationUser)
		{
			ShuttleStation closestStation;
			// determine distance from rider to each station
			float[] distanceResults = new float[1]; // temp variable to get distance between to LatLng points; size is 1 because we only want the distance (don't care about bearing)
	
			Location.distanceBetween(markerMacarthur.getPosition().latitude, markerMacarthur.getPosition().longitude, locationUser.getLatitude(), locationUser.getLongitude(), distanceResults);
			stationMacarthur.setDistanceFromRider(distanceResults[0]);
			Log.i(TAG, "onCreate() - stationMacarthur.getDistanceFromRider(): " + stationMacarthur.getDistanceFromRider());
			
			Location.distanceBetween(markerSF.getPosition().latitude, markerSF.getPosition().longitude, locationUser.getLatitude(), locationUser.getLongitude(), distanceResults);
			stationSF.setDistanceFromRider(distanceResults[0]);
			Log.i(TAG, "onCreate() - stationSF.getDistanceFromRider(): " + stationSF.getDistanceFromRider());
			
			if (stationMacarthur.getDistanceFromRider() < stationSF.getDistanceFromRider())
			{
	//			markerCurrent = markerMacarthur;
				closestStation = stationMacarthur;
			}
			else 
			{
	//			markerCurrent = markerSF;
				closestStation = stationSF;	
			}
			markerCurrent = closestStation.getMarker();
			return closestStation;
		}


	// ******************** getIntMacarthurCount() ********************
	protected int getIntMacarthurCount()
	{
		return intMacarthurCount;
	}

	// ******************** getIntRiderCount() ********************
	protected int getIntRiderCount()
	{
		return intRiderCount;
	}

	// ******************** setIntRiderCount() ********************
	protected void setIntRiderCount(int intRiderCount)
	{
		this.intRiderCount = intRiderCount;
	}

	// ******************** getIntTransbayCount() ********************
	protected int getIntTransbayCount()
	{
		return intTransbayCount;
	}

	// ******************** getMarkerCurrent() ********************
	protected Marker getMarkerCurrent()
	{
		return markerCurrent;
	}

	// ******************** setIntMacarthurCount() ********************
	protected void setIntMacarthurCount(int intCount)
	{
		intMacarthurCount = intCount;
	}

	// ******************** setIntTransbayCount() ********************
	protected void setIntTransbayCount(int intCount)
	{
		intTransbayCount = intCount;
	}

	// ******************** setMarkerCurrent() ********************
	protected void setMarkerCurrent(Marker markerCurrent)
	{
		this.markerCurrent = markerCurrent;
	}

	
	// ******************** chooseLocationProvider() ********************
	/**
	 * @return - string that identifies best LocationProvider to use
	 * 
	 */
	protected String chooseLocationProvider()
	{
		// get current location and show InfoWindow for closest station
		criteriaLocactionProvider = new Criteria();
		criteriaLocactionProvider.setPowerRequirement(Criteria.NO_REQUIREMENT);
		criteriaLocactionProvider.setAccuracy(Criteria.NO_REQUIREMENT);
		criteriaLocactionProvider.setBearingAccuracy(Criteria.NO_REQUIREMENT);
		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		// Get name of best location provider (based on low-power criteria set above) 
		return locationManager.getBestProvider(criteriaLocactionProvider, true);
	}


	// ******************** generateInfoWindowSnippet() ********************
	public String generateInfoWindowSnippet(twitter4j.Status result)
	{
		String stringTimeElapsed = "";
		String stringSeatsLeft = "";
	
		if (result != null)
		{
			stringSeatsLeft = (result.getText().startsWith(BIKE_SHUTTLE_TITLE)) 
						? ""  : retrieveStringSeatsLeft(result);
			stringTimeElapsed = generateStringTimeElapsed(result);
		} 
		else
		{
			return "";
		}
		
		return stringSeatsLeft + stringTimeElapsed;
	}


	// ******************** generateStaleSnippet ********************
	public String generateStaleSnippet(String snippet)
	// return stringSeatInfo - 
	//  strip out the portion of the string after the open parens (if any)
	//  then append "(Stale)" and return
	{
		String stringSeatInfo = "";
		if ( snippet.contains("(") )
		{
			stringSeatInfo = snippet.substring(0, snippet.indexOf("("));
		}
		else 
		{
			stringSeatInfo = snippet;
		}
		return stringSeatInfo + "(Stale)";
	}


	// ******************** generateStringTimeElapsed() ********************
	public String generateStringTimeElapsed(twitter4j.Status statusFresh)
	// generate time string with the appropriate units for the elapsed time
	{
		
		Date dateNow = new Date();
		Log.i(TAG, "generateStringTimeElapsed() - dateNow:" + dateNow);
		long millisecondsElapsed = dateNow.getTime() - statusFresh.getCreatedAt().getTime();
		String stringTimeElapsed;
		
		if (millisecondsElapsed < ONE_MINUTE)
		{
			stringTimeElapsed = " (" + millisecondsElapsed/ONE_SECOND + "s ago)";
		} 
		else if (millisecondsElapsed < ONE_HOUR)
		{
			stringTimeElapsed = " (" + millisecondsElapsed/ONE_MINUTE + "m ago)";
		} 
		else if (millisecondsElapsed < ONE_DAY)
		{
			stringTimeElapsed = " (" + millisecondsElapsed/ONE_HOUR + "h ago)";
		} 
		else 
			stringTimeElapsed = " (" + millisecondsElapsed/ONE_DAY + "d ago)";
		{
		}
	
		return stringTimeElapsed;
	} // END ******************** retrieveStringTimeElapsed() ********************


	// ******************** initializeTwitter() ********************
	/**
	 * @param args - [JAM] use to pass OAuth info if we need multiple twitter feeds 
	 * 
	 */
	protected void initializeTwitter(String[] args)
	{
		TwitterListener listener;
		ConfigurationBuilder configBuilderTwitterFeed;
	    AsyncTwitterFactory asyncFactory;
	    
	    // Config Twitter4J properties. 
	    // These OAuth tokens are for the @BikeShuttle Twitter feed.
	    // TODO [JAM] - should move these to a class (xml?) that can be obscured
	    configBuilderTwitterFeed = new ConfigurationBuilder();
	    configBuilderTwitterFeed.setDebugEnabled(true)
	      .setOAuthConsumerKey("sNAH8YaxBzUhRIPVHu8zA")
	      .setOAuthConsumerSecret("CfA7AMdKHAp6AkrNqvC9wttrKyqIrd0xvynCqe0C6c")
	      .setOAuthAccessToken("373728709-uzNKbZerlSdA58cIVmdjKwcwShXqGK0J4Z2wLugj")
	      .setOAuthAccessTokenSecret("kbHpyQlqlxAgC57rrUqhPTe28Br5H2eEjxN7x9im8Y");
	   
	    listener = new AsyncTwitterListener(); 
	    asyncFactory = new AsyncTwitterFactory(configBuilderTwitterFeed.build());
	    asyncTwitter = asyncFactory.getInstance();
	    asyncTwitter.addListener(listener);
	} 	// END ******************** initializeTwitter() ********************

	
	// ******************** isOnline() ********************
	public boolean isOnline() 
	{
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) 
	    {
	        return true;
	    }
	    return false;
	}
	
	
	// ******************** retrieveRiderCount() ********************
	/**
	 * @param statuses
	 * @return 
	 */
	protected int retrieveRiderCount(Status status)
	{
		int parsedRiderCount;
		String stringStatus = status.getText();
		int beginIndex = stringStatus.indexOf(":") + 2;
		int endIndex = stringStatus.indexOf(" rider");  // FIXME [JAM] - replace " rider" with const; could break if we change tweeted text
		String stringRiderCount = stringStatus.substring(beginIndex, endIndex) ;
		parsedRiderCount = Integer.parseInt(stringRiderCount);

		intRiderCount = parsedRiderCount;  // FIXME [JAM] - remove if possible; may cause side effects
				
		// return the rider count retrieved from the "status" parameter from the @BikeShuttle feed
		return parsedRiderCount;
	}	// END ******************** retrieveRiderCount() ********************


	// ******************** retrieveShuttleLocation() ********************
	public LatLng retrieveShuttleLocation(twitter4j.Status result)
	{
		final String COMMA = ",";
		final String OPEN_PAREN = "(";
		final String CLOSE_PAREN = ")";
		
		// get indexes of latitude and longitude strings in the "result" (most recent @BikeShuttle feed status)
		String stringResultText = result.getText();
		int intLatStart = stringResultText.indexOf(OPEN_PAREN) + 1;
		int intLatEnd = stringResultText.indexOf(COMMA);
		int intLngStart = intLatEnd + 1;
		int intLngEnd = stringResultText.indexOf(CLOSE_PAREN);
		
		// extract latitude and longitude strings 
		String stringLat = stringResultText.substring(intLatStart, intLatEnd);
		String stringLng = stringResultText.substring(intLngStart, intLngEnd);
	
		// latitude and longitude strings to type double (to match LatLng)
		double latResult = Double.parseDouble(stringLat);
		double lngResult = Double.parseDouble(stringLng);
		
		// latitude and longitude doubles into LatLng and return 
		LatLng latlngShuttle = new LatLng(latResult, lngResult);
		return latlngShuttle;
	}


	// ******************** retrieveStatusFresh() ********************
	// retrieve the most recent Tweet/status that matches the Marker with the title markerTitle
	public twitter4j.Status retrieveStatusFresh(ResponseList<twitter4j.Status> recentStatuses, String markerTitle)
	{
		Date dateTweeted = new Date(0);
		Status statusTwitter = null;
		for (Status status : recentStatuses)
		{
			boolean dateIsNewer = status.getCreatedAt().after(dateTweeted);
			boolean statusMatchesMarker = status.getText().startsWith(markerTitle);
			if (statusMatchesMarker && dateIsNewer)
			{
				dateTweeted = status.getCreatedAt();
				statusTwitter = status;
			}
		}
		return statusTwitter;
	}


	// ******************** retrieveStringSeatsLeft() ********************
			/**
			 * @param statusFresh - most recent Tweet/Status for this station/Marker [DO NULL CHECK BEFORE CALLING]
			 * @return
			 */
			protected String retrieveStringSeatsLeft(Status statusFresh)
			{
				String stringSeatsLeft;
	//			int intRetrievedCount;
				int intSeatsLeft;
				intSeatsLeft = SHUTTLE_CAPACTITY - retrieveRiderCount(statusFresh);
				stringSeatsLeft = (intSeatsLeft < 1) 
						? SHUTTLE_IS_FULL 
						: intSeatsLeft + " Seats left";
				return stringSeatsLeft;
			}


	// ******************** showDialogFragment() ********************
	void showDialogFragment(DialogFragment newDialogFragment, Marker markerStation) 
	{
		if (markerStation.getTitle().equalsIgnoreCase(markerShuttle.getTitle()) )
		{
			newDialogFragment = TweetShuttleLocationDialogFragment.newInstance(R.string.alert_dialog_shuttle_location_title, intRiderCount);
		} 
		else
		{
			newDialogFragment = TweetRiderCountDialogFragment.newInstance(R.string.alert_dialog_two_buttons_title, intRiderCount);
		}
		newDialogFragment.show(getFragmentManager(), "dialog");
	}
	
	// ******************** servicesConnected() ********************
	// Check that Google Play services is available
    private boolean servicesConnected() 
    {
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) 
        {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
        // Google Play services was not available for some reason
        } 
        else 
        {
            // Get the error code
//            int errorCode = connectionResult.getErrorCode();
            int errorCode = resultCode;
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) 
            {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getFragmentManager(),
                        "Location Updates");
            }
            return false;
        }
    }

	// ******************** doRiderCountPositiveClick() ********************
	public void doRiderCountPositiveClick() 
	{
		int intSeatsLeft = SHUTTLE_CAPACTITY - intRiderCount;
		String stringSnippet;

		Log.i(TAG, "doPositiveClick()");

		// Update InfoWindow for current map Marker
		stringSnippet = (intRiderCount >= SHUTTLE_CAPACTITY) ? SHUTTLE_IS_FULL : intSeatsLeft + " Seats left (Just tweeted)";
		getMarkerCurrent().setSnippet(stringSnippet);
		getMarkerCurrent().showInfoWindow();

		// Update @BikeShuttle Twitter feed (use async API so we don't get ANR errors)
		stringLatestTweet = generateTweetString(getMarkerCurrent(), intRiderCount);
		asyncTwitter.updateStatus(stringLatestTweet);
	}

	
	// ******************** doShuttleLocationPositiveClick() ********************
	public void doShuttleLocationPositiveClick() 
	{
		String stringSnippet;

		// Update shuttle GeoLocation
        mCurrentLocation = mLocationClient.getLastLocation();
        LatLng latLngTemp = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
		markerShuttle.setPosition(latLngTemp );
		
//		locationRider = locationManager.getLastKnownLocation(providerBest);
//		getMarkerCurrent().setPosition(new LatLng(locationRider.getLatitude(), locationRider.getLongitude()));

		//		getMarkerCurrent().setPosition(new LatLng(latLngShuttleMock.latitude, latLngShuttleMock.longitude));
		
		// Unimplemented - Maybe we can embed GeoLocation metadata in a tweet?
		//		GeoLocation geolocationShuttle;
		//		geolocationShuttle = new GeoLocation(locationRider.getLatitude(), locationRider.getLongitude());
		
		// Update InfoWindow for current map Marker (bike shuttle)
		stringSnippet = "Just now";
//		getMarkerCurrent().setSnippet(stringSnippet);
//		getMarkerCurrent().showInfoWindow();
		markerShuttle.setSnippet(stringSnippet);
		markerShuttle.showInfoWindow();

		stringLatestTweet = markerShuttle.getTitle() 
				+ " GeoLocation: " 
				+ "("
				+ (float)markerShuttle.getPosition().latitude + ","
				+ (float)markerShuttle.getPosition().longitude 
				+ ")"
				+ "\n View Map: "
				+ GOOGLEMAP_URL
				+ (float)markerShuttle.getPosition().latitude + ","
				+ (float)markerShuttle.getPosition().longitude 
				+ "&sensor=false" 
				+ "\n "
				+ SystemClock.elapsedRealtime();
				;

		// Post to @BikeShuttle Twitter feed (use async API so we don't get ANR errors)
		asyncTwitter.updateStatus(stringLatestTweet);
	}

	
	// ******************** doNegativeClick() ********************
	public void doRiderCountNegativeClick() 
	{
		Log.i("FragmentAlertDialog", "doRiderCountNegativeClick() - ");
	}
		
	
	// ***************************************************************************
	// ******************** InnerClass - AsyncTwitterListener ********************
		/*		This this inner class handles communication with the BikeShuttle Twitter feed
		 * 		via the AsyncTwitter class.  Async communication is required because Android does 
		 * 		not support network activities on the main/UI thread.
		 */
		private final class AsyncTwitterListener extends TwitterAdapter
		{
			// ******************** updatedStatus() ********************
			@Override public void updatedStatus(Status status) 
			{
	        	Log.w(TAG,"TwitterAdapter.updatedStatus() - Successfully updated the status to [" +
	                   status.getText() + "].");
	        }

			// ******************** gotUserTimeline() ********************
			/*  Called when the ResponseList (last 20 tweets) has been successfully retrieved
			 */
			@Override
			public void gotUserTimeline(ResponseList<Status> statuses)
			{
				super.gotUserTimeline(statuses);
				// save 20 most recent status updates to activity variable 
				recentStatuses = statuses;
				// set recentStatusRetrieved flag to exit while loop in UpdateMarkerAsyncTask.doInBackground()
				recentStatusRetrieved = true;
				Log.i(TAG,  "gotUserTimeline() - recentStatusRetrieved: " + recentStatusRetrieved);
			}

			// ******************** onException() ********************
			public void onException(TwitterException e, TwitterMethod method) 
	        {
				Log.e(TAG, "onException() - method.getClass().getCanonicalName(): " + method.getClass().getCanonicalName() + "." + method);
				toastNetworkWarning.show();
				e.printStackTrace();
/*	        	if (method == TwitterMethod.UPDATE_STATUS) 
	        	{
		        	Log.e(TAG,"TwitterAdapter.onException() - method == TwitterMethod.UPDATE_STATUS");
	        	} else 
	        	{
	        		throw new AssertionError("Should not happen");
	        	}
*/
	        }
		}	// END ******************** InnerClass - AsyncTwitterListener ********************

		// **********************************************************************************************
		// ******************** InnerClass - ErrorDialogFragment  ********************
	    // Define a DialogFragment that displays the error dialog
	    public static class ErrorDialogFragment extends DialogFragment
	    {
	        // Global field to contain the error dialog
	        private Dialog mDialog;
	        // Default constructor. Sets the dialog field to null
	        public ErrorDialogFragment() 
	        {
	            super();
	            mDialog = null;
	        }
	        // Set the dialog to display
	        public void setDialog(Dialog dialog) 
	        {
	            mDialog = dialog;
	        }
	        // Return a Dialog to the DialogFragment.
	        @Override
	        public Dialog onCreateDialog(Bundle savedInstanceState) 
	        {
	            return mDialog;
	        }
	    }
	    
	    
		// **********************************************************************************************
				// ******************** InnerClass - LocationListenerImplementation  ********************
				/*	This class is necessary to implement the onLocationChanged callback method,
				 *  which controls what happens when we get an updated location.
				 */	
				private final class LocationListenerImplementation implements LocationListener
				{
		
					@Override
					public void onLocationChanged(Location location)
					{
		//				locationRider = location;
						locationRider.set(location);
						Log.v(TAG, "onLocationChanged() - locationRider.getLatitude(): " + locationRider.getLatitude() );
						Log.v(TAG, "onLocationChanged() - locationRider.getLongitude(): " + locationRider.getLongitude() );
						Log.v(TAG, "onLocationChanged() - locationRider.getTime(): " + locationRider.getTime() );
					}
		
					@Override
					public void onProviderDisabled(String provider)
					{
						Log.v(TAG, "onProviderDisabled() - CALLED");
					}
		
					@Override
					public void onProviderEnabled(String provider)
					{
						Log.v(TAG, "onProviderEnabled() - CALLED");
					}
		
					@Override
					public void onStatusChanged(String provider, int status,
							Bundle extras)
					{
						Log.v(TAG, "onStatusChanged() - CALLED");
					}
		
				}

				// **********************************************************************************************
				// ******************** InnerClass - MockLocationProvider ********************
				/*	This class provides mock locations for testing.
				 * The constructor takes the name of the location provider that this mock provider will replace. 
				 * For example, LocationManager.GPS_PROVIDER. 
				 * The calls to the addTestProvider() and setTestProviderEnabled() tell 
				 * the LocationManager that the given provider will be replaced by mock data. 
				 * The pushLocation() method supplies mock location data for a given provide
				 */	
				public class MockLocationProvider 
				{
					  String providerName;
					  Context ctx;
					 
					  public MockLocationProvider(String name, Context ctx) 
					  {
					    this.providerName = name;
					    this.ctx = ctx;
					 
					    LocationManager lm = (LocationManager) ctx.getSystemService(
					      Context.LOCATION_SERVICE);
					    lm.addTestProvider(providerName, false, false, false, false, false, 
					      true, true, 0, 5);
					    lm.setTestProviderEnabled(providerName, true);
					  }
					 
					  public void pushLocation(double lat, double lon) 
					  {
					    LocationManager lm = (LocationManager) ctx.getSystemService(
					      Context.LOCATION_SERVICE);
					 
					    Location mockLocation = new Location(providerName);
					    mockLocation.setLatitude(lat);
					    mockLocation.setLongitude(lon); 
					    mockLocation.setAltitude(0); 
					    mockLocation.setTime(System.currentTimeMillis()); 
					    lm.setTestProviderLocation(providerName, mockLocation);
					  }
					 
					  public void shutdown() 
					  {
					    LocationManager lm = (LocationManager) ctx.getSystemService(
					      Context.LOCATION_SERVICE);
					    lm.removeTestProvider(providerName);
					  }
					} // END ******************** InnerClass - MockLocationProvider ********************


		// **********************************************************************************************
		// ******************** InnerClass - OnInfoWindowClickListenerImplementation ********************
		/*	This class is necessary to implement the onInfoWindowClick callback method,
		 *  which controls what happens when user taps the InfoWindow for a map marker.
		 */	
		private final class OnInfoWindowClickListenerImplementation implements OnInfoWindowClickListener
		{
			// ******************** onInfoWindowClick() ********************
			@Override
			public void onInfoWindowClick(Marker markerStation)
			{
				// set the markerCurrent to the current station so we can apply changes to the proper marker
				setMarkerCurrent(markerStation);

				// show AlertDialog so user can tweet how many riders are at the station
				showDialogFragment(dialogRiderQueue, markerStation);
				
			}
		}	// END ******************** InnerClass - OnInfoWindowClickListenerImplementation ********************

		
		// **********************************************************************************************
		// ******************** InnerClass - OnMarkerClickListenerImplementation ********************
		/*	This class is necessary to implement the onInfoWindowClick callback method,
		 *  which controls what happens when user taps the info window for a map marker.
		 */	
		private final class OnMarkerClickListenerImplementation implements OnMarkerClickListener
		{
			// ******************** onMarkerClick() ********************
			public boolean onMarkerClick(Marker markerStation)
			{
				// set the markerCurrent to the current station so we can apply changes to the proper marker
				setMarkerCurrent(markerStation);

				// Request updated rider location from device
//				locationManager.requestSingleUpdate(providerBest, locationListenerRider, getMainLooper());
				mLocationClient.getLastLocation();
				
				// Get updated station info from Twitter (AsyncTask puts info in the Marker's InfoWindow)
				new UpdateMarkerAsyncTask().execute(markerStation.getTitle(), markerStation.getSnippet());
				return true;
			}
		}	// END ******************** InnerClass - OnInfoWindowClickListenerImplementation ********************

		// ******************** AsyncTask - UpdateMarkerAsyncTask ********************
		protected class ShowClosestStationAsyncTask extends AsyncTask<Void, Void, Void>
		{

			@Override
			protected Void doInBackground(Void... arg0)
			{
				Thread.currentThread().setName("- " + this.getClass().getSimpleName());
				while (!booleanLocationClientConnected && !isCancelled())
				{
					try
					{ Thread.sleep((long) (0.2 * ONE_SECOND)); } 
					catch (InterruptedException e)
					{ e.printStackTrace(); }
				}				
				return null;
			}

			// ******************** onPostExecute() ********************
			/* 
			 * Show InfoWindow for nearest shuttle station
			 */
			@Override
			protected void onPostExecute(Void result)
			{
				super.onPostExecute(result);
				// Get rider's most recent location
		        mCurrentLocation = mLocationClient.getLastLocation();
				stationCurrent = getClosestStation(mCurrentLocation);
				stationCurrent.getMarker().showInfoWindow();		
			    new UpdateMarkerAsyncTask().execute(stationCurrent.getMarker().getTitle(), stationCurrent.getMarker().getSnippet());
		        
			}

			// ******************** onPreExecute() ********************
			/* 
			 * Start connecting to mLocationClient if not already connected or connecting
			 */
			@Override
			protected void onPreExecute()
			{
				super.onPreExecute();
		        // Connect the client if it is not already connected or connecting.
		        if (!mLocationClient.isConnected() && !mLocationClient.isConnecting())
				{
		        	mLocationClient.connect();
				}

			}	
			
		}

		// ******************** AsyncTask - UpdateMarkerAsyncTask ********************
		protected class UpdateMarkerAsyncTask extends AsyncTask<String, String, twitter4j.Status>
		{		     

			private String stringSavedSnippet;
			// ******************** doInBackground() ********************
			// params[0] - marker title
			// params[1] - marker snippet
			@Override
			protected twitter4j.Status doInBackground(String... params)
			{
				Thread.currentThread().setName("- " + this.getClass().getSimpleName() + " - " + params[0]);
				String stringUpdating;
				String stringCurrentSnippet = "Updating ";
				String stringProgress = "....        ";
				if ( params[1].contains("(") )
				{
					stringCurrentSnippet = params[1].substring(0, params[1].indexOf("("));
				}
				while (!recentStatusRetrieved && !isCancelled())
				{
					// Escape early if cancel() is called
//					if (isCancelled()) break;
					// Update text for InfoWindow snippet
					String lastChar = stringProgress.substring(stringProgress.length() - 1);
					String stringLeading = stringProgress.substring(0,stringProgress.length() - 1);
					stringProgress = lastChar.concat(stringLeading);
					stringUpdating = stringCurrentSnippet + " (" + stringProgress + ")";
					publishProgress(stringUpdating);
					try
					{ Thread.sleep((long) (0.2 * ONE_SECOND)); } 
					catch (InterruptedException e)
					{ e.printStackTrace(); }
//					intLoopCount = intLoopCount + 1;
				}
				recentStatusRetrieved = false; // reset while loop flag for next marker update
//				String newSnippet = generateInfoWindowSnippet(params[0]);
//				return newSnippet;
				return retrieveStatusFresh (recentStatuses, params[0]);
			}

			// ******************** onCancelled() ********************
			/* 
			 *  This method is called instead of onPostExecute if cancel() is invoked
			 *  The original Snippet for the current map Marker is restored 
			 *  and then displayed
			 */
			@Override
			protected void onCancelled()
			{
				// TODO Auto-generated method stub
				super.onCancelled();
				getMarkerCurrent().setSnippet(stringSavedSnippet);
				getMarkerCurrent().showInfoWindow();
			}

			// ******************** onPreExecute() ********************
			/* 
			 * start retrieving timeline from BikeShuttle Twitter feed
			 */
			@Override
			protected void onPreExecute()
			{
				super.onPreExecute();
				stringSavedSnippet = getMarkerCurrent().getSnippet();
				if (isOnline())
				{
					asyncTwitter.getUserTimeline();
				}
				else 
				{
					cancel(true);
					toastNetworkWarning.show();
				}
			}

			// ******************** onProgressUpdate() ********************
			// show progress update in current map Marker's InfoWindow
			protected void onProgressUpdate(String... progress) 
			{
				getMarkerCurrent().setSnippet(progress[0]);
				getMarkerCurrent().showInfoWindow();

			}
			// ******************** onPostExecute() ********************
			// Update all map Markers and show InfoWindow for current station
			protected void onPostExecute(twitter4j.Status result) 
			{
				Log.i(TAG, "onPostExecute() - result: " + result + "/n - END of result");
//				if (result.length() != 0)
				
				twitter4j.Status statusFresh;

				statusFresh = retrieveStatusFresh (recentStatuses, markerShuttle.getTitle());
				if (statusFresh != null)
				{
					LatLng latlngShuttle = retrieveShuttleLocation(statusFresh);
					markerShuttle.setPosition(latlngShuttle);
					markerShuttle.setSnippet(generateInfoWindowSnippet(statusFresh));
				}
				markerShuttle.setVisible(true);
				
				statusFresh = retrieveStatusFresh (recentStatuses, markerMacarthur.getTitle());
				if (statusFresh != null)
				{
					markerMacarthur.setSnippet(generateInfoWindowSnippet(statusFresh));
				}
				else 
				{
					markerMacarthur.setSnippet(generateStaleSnippet(markerMacarthur.getSnippet()));
				}
				
				statusFresh = retrieveStatusFresh (recentStatuses, markerSF.getTitle());
				if (statusFresh != null)
				{
					markerSF.setSnippet(generateInfoWindowSnippet(statusFresh));
				}
				else 
				{
					markerSF.setSnippet(generateStaleSnippet(markerSF.getSnippet()));
				}
				
				getMarkerCurrent().showInfoWindow();

			}
		}  // END ******************** AsyncTask - UpdateMarkerAsyncTask ********************


}	// END ******************** Class - ShuttleMapActivity ********************
