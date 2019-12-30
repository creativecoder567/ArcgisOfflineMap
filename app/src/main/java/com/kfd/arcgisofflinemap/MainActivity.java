package com.kfd.arcgisofflinemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ListActivity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.TimeExtent;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.DefaultSceneViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG ="SARATH" ;
    private MapView mMapView;


    private static String filename;
    LocationDisplay mLocationDisplay;
    private Spinner mSpinner;
    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};
    ArcGISMap map;
    private ServiceFeatureTable mServiceFeatureTable;
    private Callout mCallout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the Spinner from layout
        mSpinner = (Spinner) findViewById(R.id.spinner);

        mMapView = findViewById(R.id.mapView);
        filename = this.getResources().getString(R.string.local_tpk2);
//        We have tpk file in this path
        File storageDir = new File(Environment.getExternalStorageDirectory(), "Efelling");
        // create the url
        String base = storageDir + File.separator + filename;

        ArcGISTiledLayer tiledLayerBaseMap = new ArcGISTiledLayer(base);
        ArcGISTiledLayer tiledLayerBaseMap1 = new ArcGISTiledLayer(getResources().getString(R.string.world_topo_service));

        // create a service feature table and a feature layer from it
        mServiceFeatureTable = new ServiceFeatureTable(getString(R.string.us_daytime_population_url));

// set tiled layer as basemap
        Basemap basemap = new Basemap(tiledLayerBaseMap1);
//        Basemap basemap1 = new Basemap(tiledLayerBaseMap1);
//        basemap.getBaseLayers().add(tiledLayerBaseMap);
// create a map with the basemap
         map = new ArcGISMap(basemap);
//        map.getBasemap().getBaseLayers().add(tiledLayerBaseMap);

// set the map to be displayed in this view
        mMapView.setMap(map);

//            setupMap();
        // get the MapView's LocationDisplay
         mLocationDisplay = mMapView.getLocationDisplay();


        // Listen to changes in the status of the location data source.
        mLocationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {

            @Override
            public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                // If LocationDisplay started OK, then continue.
                if (dataSourceStatusChangedEvent.isStarted())
                    return;

                // No error is reported, then continue.
                if (dataSourceStatusChangedEvent.getError() == null)
                    return;

                // If an error is found, handle the failure to start.
                // Check permissions to see if failure may be due to lack of permissions.
                boolean permissionCheck1 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[0]) ==
                        PackageManager.PERMISSION_GRANTED;
                boolean permissionCheck2 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[1]) ==
                        PackageManager.PERMISSION_GRANTED;

                if (!(permissionCheck1 && permissionCheck2)) {
                    // If permissions are not already granted, request permission from the user.
                    ActivityCompat.requestPermissions(MainActivity.this, reqPermissions, requestCode);
                } else {
                    // Report other unknown failure types to the user - for example, location services may not
                    // be enabled on the device.
                    String message = String.format("Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
                            .getSource().getLocationDataSource().getError().getMessage());
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();

                    // Update UI to reflect that the location display did not actually start
                    mSpinner.setSelection(0, true);
                }
            }
        });
        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        mLocationDisplay.startAsync();

        symbolizeShapefile();

    }




    /**
     * Creates a ShapefileFeatureTable from a service and, on loading, creates a FeatureLayer and add it to the map.
     */
    private void featureLayerShapefile() {
        // load the shapefile with a local path
        ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(
                Environment.getExternalStorageDirectory()+File.separator+"Efelling/2001030001/"+ getString(R.string.shapefile_path));
        Log.d(TAG, "featureLayerShapefile: "+Environment.getExternalStorageDirectory()+File.separator+"Efelling/2001030001/"+ getString(R.string.shapefile_path));

        shapefileFeatureTable.loadAsync();
        shapefileFeatureTable.addDoneLoadingListener(() -> {
            if (shapefileFeatureTable.getLoadStatus() == LoadStatus.LOADED) {

                // create a feature layer to display the shapefile
                FeatureLayer shapefileFeatureLayer = new FeatureLayer(shapefileFeatureTable);

                // add the feature layer to the map
                mMapView.getMap().getOperationalLayers().add(shapefileFeatureLayer);

                // zoom the map to the extent of the shapefile
                mMapView.setViewpointAsync(new Viewpoint(shapefileFeatureLayer.getFullExtent()));
            } else {
                String error = "Shapefile feature table failed to load: " + shapefileFeatureTable.getLoadError().toString();
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
//                Log.e(TAG, error);
            }
        });
    }
    private void symbolizeShapefile() {

        // create a shapefile feature table from the local data
        ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(
                Environment.getExternalStorageDirectory()+File.separator+"Efelling/2001030001/"+ getString(R.string.shapefile_path));
        Log.d(TAG, "symbolizeShapefile: "+Environment.getExternalStorageDirectory()+File.separator+"Efelling/2001030001/"+ getString(R.string.shapefile_path));


        // use the shapefile feature table to create a feature layer
        FeatureLayer featureLayer = new FeatureLayer(shapefileFeatureTable);

        // create the Symbol
        SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.RED, 1.0f);
        SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.YELLOW, lineSymbol);

        // create the Renderer
        SimpleRenderer renderer = new SimpleRenderer(fillSymbol);

        // set the Renderer on the Layer
        featureLayer.setRenderer(renderer);

        // add the feature layer to the map
        map.getOperationalLayers().add(featureLayer);

        // create objects required to do a selection with a query
        QueryParameters query = new QueryParameters();
        // make search case insensitive
        query.setWhereClause("1=1" );
        // call select features
        final ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable.queryFeaturesAsync(query,ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
        // add done loading listener to fire when the selection returns
        future.addDoneListener(() -> {
            try {
                // call get on the future to get the result
                FeatureQueryResult result = future.get();


                // check there are some results
                Iterator<Feature> resultIterator = result.iterator();
                if (resultIterator.hasNext()) {
                    // get the extent of the first feature in the result to zoom to
                    Feature feature = resultIterator.next();
                    Log.d(TAG, "symbolizeShapefile: "+feature.getAttributes());
                    Log.d(TAG, "symbolizeShapefile: "+feature.getFeatureTable());
                    Envelope envelope = feature.getGeometry().getExtent();
                    mMapView.setViewpointGeometryAsync(envelope, 10);
                    // select the feature
                    featureLayer.selectFeature(feature);

                    Map<String, Object> attr = feature.getAttributes();
                    Set<String> keys = attr.keySet();
                    for (String key : keys){
                        Object value = attr.get(key);
                        Log.d(TAG, "symbolizeShapefile: "+value);
                    }

                } else {
                    Toast.makeText(this, "No states found with name: " , Toast.LENGTH_LONG).show();
                }





            } catch (Exception e) {
                String error = "Feature search failed for: " +  ". Error: " + e.getMessage();
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, error);
            }



        });


        // get the callout that shows attributes
        mCallout = mMapView.getCallout();
        // set an on touch listener to listen for click events
     /*   mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // remove any existing callouts
                if (mCallout.isShowing()) {
                    mCallout.dismiss();
                }
                // get the point that was clicked and convert it to a point in map coordinates
                final Point clickPoint = mMapView
                        .screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));
                // create a selection tolerance
                int tolerance = 10;
                double mapTolerance = tolerance * mMapView.getUnitsPerDensityIndependentPixel();
                // use tolerance to create an envelope to query
                Envelope envelope = new Envelope(clickPoint.getX() - mapTolerance, clickPoint.getY() - mapTolerance,
                        clickPoint.getX() + mapTolerance, clickPoint.getY() + mapTolerance, map.getSpatialReference());
                QueryParameters query = new QueryParameters();
                query.setGeometry(envelope);
                // request all available attribute fields
                final ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable
                        .queryFeaturesAsync(query, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
                // add done loading listener to fire when the selection returns
                future.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // call get on the future to get the result
                            FeatureQueryResult result = future.get();


                            // check there are some results
                            Iterator<Feature> resultIterator = result.iterator();
                            if (resultIterator.hasNext()) {
                                // get the extent of the first feature in the result to zoom to
                                Feature feature = resultIterator.next();
                                Log.d(TAG, "ontouch: "+feature.getAttributes());
                                Log.d(TAG, "ontouch: "+feature.getFeatureTable());
                                Envelope envelope = feature.getGeometry().getExtent();
                                mMapView.setViewpointGeometryAsync(envelope, 10);
                                // select the feature
                                featureLayer.selectFeature(feature);

                                Map<String, Object> attr = feature.getAttributes();
                                Set<String> keys = attr.keySet();
                                for (String key : keys){
                                    Object value = attr.get(key);
                                    Log.d(TAG, "ontouch: "+value);
                                }

                            } else {
                            }





                        } catch (Exception e) {
                            String error = "Feature search failed for: " +  ". Error: " + e.getMessage();
                            Log.e(TAG, error);
                        }
                    }
                });
                return super.onSingleTapConfirmed(e);
            }
        });*/
    }

    private void setupMap() {
        if (mMapView != null) {
            Basemap.Type basemapType = Basemap.Type.STREETS_VECTOR;
            double latitude = 34.09042;
            double longitude = -118.71511;
            int levelOfDetail = 11;
            ArcGISMap map = new ArcGISMap(basemapType, latitude, longitude, levelOfDetail);
            mMapView.setMap(map);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Location permission was granted. This would have been triggered in response to failing to start the
            // LocationDisplay, so try starting this again.
            mLocationDisplay.startAsync();
        } else {
            // If permission was denied, show toast to inform user what was chosen. If LocationDisplay is started again,
            // request permission UX will be shown again, option should be shown to allow never showing the UX again.
            // Alternative would be to disable functionality so request is not shown again.
            Toast.makeText(MainActivity.this, getResources().getString(R.string.location_permission_denied), Toast
                    .LENGTH_SHORT).show();

            // Update UI to reflect that the location display did not actually start
            mSpinner.setSelection(0, true);
        }
    }

    @Override
    protected void onPause() {
        if (mMapView != null) {
            mMapView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMapView != null) {
            mMapView.dispose();
        }
        super.onDestroy();
    }
}
