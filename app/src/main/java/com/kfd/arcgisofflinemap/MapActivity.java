package com.kfd.arcgisofflinemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.InputDeviceCompat;

import android.Manifest;
import android.app.ListActivity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import com.esri.arcgisruntime.arcgisservices.LabelDefinition;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.data.FeatureCollectionTable;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ShapefileFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.layers.FeatureCollectionLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

public class MapActivity extends AppCompatActivity {

    private static final String TAG ="SARATH" ;
    private MapView mMapView;
    Feature feature = null;
    FeatureLayer featureLayer;

    private static String filename;
    LocationDisplay mLocationDisplay;
    private Spinner mSpinner;
    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};
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


// set tiled layer as basemap
        Basemap basemap = new Basemap(tiledLayerBaseMap1);
//        Basemap basemap1 = new Basemap(tiledLayerBaseMap1);
//        basemap.getBaseLayers().add(tiledLayerBaseMap);
// create a map with the basemap
        ArcGISMap map = new ArcGISMap(basemap);
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
                boolean permissionCheck1 = ContextCompat.checkSelfPermission(MapActivity.this, reqPermissions[0]) ==
                        PackageManager.PERMISSION_GRANTED;
                boolean permissionCheck2 = ContextCompat.checkSelfPermission(MapActivity.this, reqPermissions[1]) ==
                        PackageManager.PERMISSION_GRANTED;

                if (!(permissionCheck1 && permissionCheck2)) {
                    // If permissions are not already granted, request permission from the user.
                    ActivityCompat.requestPermissions(MapActivity.this, reqPermissions, requestCode);
                } else {
                    // Report other unknown failure types to the user - for example, location services may not
                    // be enabled on the device.
                    String message = String.format("Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
                            .getSource().getLocationDataSource().getError().getMessage());
                    Toast.makeText(MapActivity.this, message, Toast.LENGTH_LONG).show();

                    // Update UI to reflect that the location display did not actually start
                    mSpinner.setSelection(0, true);
                }
            }
        });
        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
        mLocationDisplay.startAsync();


    }

    private void featureLayerShapefile() {
        // load the shapefile with a local path
        ShapefileFeatureTable shapefileFeatureTable = new ShapefileFeatureTable(
                Environment.getExternalStorageDirectory() +"/Efelling/0201010001/0201010001.shp");

        shapefileFeatureTable.loadAsync();
        shapefileFeatureTable.addDoneLoadingListener(() -> {
            if (shapefileFeatureTable.getLoadStatus() == LoadStatus.LOADED) {

                // create a feature layer to display the shapefile
                featureLayer = new FeatureLayer(shapefileFeatureTable);

                featureLayer.setOpacity(0.7f);
                // add the feature layer to the map
//                mMapView.getMap().getOperationalLayers().add(featureLayer);

                // zoom the map to the extent of the shapefile
                mMapView.setViewpointAsync(new Viewpoint(featureLayer.getFullExtent()));
            } else {
                String error = "Shapefile feature table failed to load: " + shapefileFeatureTable.getLoadError().toString();
                Toast.makeText(MapActivity.this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, error);
            }


        });

        //create query parameters
        QueryParameters queryParams = new QueryParameters();

        // 1=1 will give all the features from the table
        queryParams.setWhereClause("SurveyNumb=25");

        //query feature from the table
        final ListenableFuture<FeatureQueryResult> queryResult = shapefileFeatureTable.queryFeaturesAsync(queryParams);
        queryResult.addDoneListener(new Runnable() {
            @Override public void run() {
                try {
                    Iterator<Feature> iterator = ((FeatureQueryResult) queryResult.get()).iterator();
                    MapActivity.this.feature = iterator.next();
                    String hardcode = MapActivity.this.feature.getGeometry().toJson();
                    Log.d("geomjson", hardcode);
                    TextSymbol textSymbol = new TextSymbol();
                    textSymbol.setSize(20.0f);
                    textSymbol.setColor(-16776961);
                    textSymbol.setHaloColor(InputDeviceCompat.SOURCE_ANY);
                    textSymbol.setHaloWidth(2.0f);
                    JsonObject json = new JsonObject();
                    JsonObject expressionInfo = new JsonObject();
                    expressionInfo.add("expression", new JsonPrimitive("$feature.surveynumb"));
                    Log.d(TAG, "run: "+"$feature.surveynumb");
                    Log.d(TAG, "run: "+expressionInfo.toString());
                    json.add("labelExpressionInfo", expressionInfo);
                    json.add("symbol", new JsonParser().parse(textSymbol.toJson()));
                    MapActivity.this.featureLayer.getLabelDefinitions().add(LabelDefinition.fromJson(json.toString()));
                    MapActivity.this.featureLayer.setLabelsEnabled(true);
                    MapActivity.this.mMapView.getMap().getOperationalLayers().add((Layer) MapActivity.this.featureLayer);
                    Envelope envelope = feature.getGeometry().getExtent();
                    mMapView.setViewpointGeometryAsync(envelope, 10);
                    // select the feature
                    featureLayer.selectFeature(feature);

//                    MapActivity.this.mMapView.setViewpointAsync(new Viewpoint(MapActivity.this.featureLayer.getFullExtent()));
                    if (hardcode.contains("]],[[")) {
                        String polygon_part1 = hardcode.split("]],\\[\\[")[0];
                        String polygon_part2 = hardcode.split("]]],")[1];
                        Log.d("hardcord", polygon_part1 + "]]]," + polygon_part2);
                        MapActivity.this.feature.setGeometry(Geometry.fromJson(polygon_part1 + "]]]," + polygon_part2));
                    }
                    Toast.makeText(MapActivity.this.getApplicationContext(), " features selected", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.d("Line no error", String.valueOf(e.getStackTrace()[0].getLineNumber()));
                    Log.e(MapActivity.this.getResources().getString(R.string.app_name), "Select feature failed: " + e.getMessage());
                }
            }
        });
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
            Toast.makeText(MapActivity.this, getResources().getString(R.string.location_permission_denied), Toast
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
        featureLayerShapefile();
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
