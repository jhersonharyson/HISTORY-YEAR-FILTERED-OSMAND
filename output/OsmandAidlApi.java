package net.osmand.aidl;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.aidl.favorite.AFavorite;
import net.osmand.aidl.favorite.group.AFavoriteGroup;
import net.osmand.aidl.gpx.AGpxFile;
import net.osmand.aidl.gpx.AGpxFileDetails;
import net.osmand.aidl.gpx.ASelectedGpxFile;
import net.osmand.aidl.gpx.StartGpxRecordingParams;
import net.osmand.aidl.gpx.StopGpxRecordingParams;
import net.osmand.aidl.maplayer.AMapLayer;
import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.aidl.mapmarker.AMapMarker;
import net.osmand.aidl.mapwidget.AMapWidget;
import net.osmand.aidl.search.SearchResult;
import net.osmand.aidl.tiles.ASqliteDbFile;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.helpers.ExternalApiHelper;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.AidlMapLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MapWidgetRegInfo;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static net.osmand.plus.OsmAndCustomizationConstants.DRAWER_ITEM_ID_SCHEME;


public class OsmandAidlApi {
	private static final Log LOG = PlatformUtil.getLog(OsmandAidlApi.class);
	private static final String AIDL_REFRESH_MAP = "aidl_refresh_map";
	private static final String AIDL_SET_MAP_LOCATION = "aidl_set_map_location";
	private static final String AIDL_LATITUDE = "aidl_latitude";
	private static final String AIDL_LONGITUDE = "aidl_longitude";
	private static final String AIDL_ZOOM = "aidl_zoom";
	private static final String AIDL_ANIMATED = "aidl_animated";

	private static final String AIDL_START_NAME = "aidl_start_name";
	private static final String AIDL_START_LAT = "aidl_start_lat";
	private static final String AIDL_START_LON = "aidl_start_lon";
	private static final String AIDL_DEST_NAME = "aidl_dest_name";
	private static final String AIDL_DEST_LAT = "aidl_dest_lat";
	private static final String AIDL_DEST_LON = "aidl_dest_lon";
	private static final String AIDL_PROFILE = "aidl_profile";
	private static final String AIDL_DATA = "aidl_data";
	private static final String AIDL_URI = "aidl_uri";
	private static final String AIDL_FORCE = "aidl_force";
	private static final String AIDL_SEARCH_QUERY = "aidl_search_query";
	private static final String AIDL_SEARCH_LAT = "aidl_search_lat";
	private static final String AIDL_SEARCH_LON = "aidl_search_lon";

	private static final String AIDL_OBJECT_ID = "aidl_object_id";

	private static final String AIDL_ADD_MAP_WIDGET = "aidl_add_map_widget";
	private static final String AIDL_REMOVE_MAP_WIDGET = "aidl_remove_map_widget";

	private static final String AIDL_ADD_MAP_LAYER = "aidl_add_map_layer";
	private static final String AIDL_REMOVE_MAP_LAYER = "aidl_remove_map_layer";

	private static final String AIDL_TAKE_PHOTO_NOTE = "aidl_take_photo_note";
	private static final String AIDL_START_VIDEO_RECORDING = "aidl_start_video_recording";
	private static final String AIDL_START_AUDIO_RECORDING = "aidl_start_audio_recording";
	private static final String AIDL_STOP_RECORDING = "aidl_stop_recording";

	private static final String AIDL_NAVIGATE = "aidl_navigate";
	private static final String AIDL_NAVIGATE_GPX = "aidl_navigate_gpx";
	private static final String AIDL_NAVIGATE_SEARCH = "aidl_navigate_search";
	private static final String AIDL_PAUSE_NAVIGATION = "pause_navigation";
	private static final String AIDL_RESUME_NAVIGATION = "resume_navigation";
	private static final String AIDL_STOP_NAVIGATION = "stop_navigation";
	private static final String AIDL_MUTE_NAVIGATION = "mute_navigation";
	private static final String AIDL_UNMUTE_NAVIGATION = "unmute_navigation";

	private static final String AIDL_SHOW_SQLITEDB_FILE = "aidl_show_sqlitedb_file";
	private static final String AIDL_HIDE_SQLITEDB_FILE = "aidl_hide_sqlitedb_file";
	private static final String AIDL_FILE_NAME = "aidl_file_name";

	private static final ApplicationMode DEFAULT_PROFILE = ApplicationMode.CAR;

	private static final ApplicationMode[] VALID_PROFILES = new ApplicationMode[]{
			ApplicationMode.CAR,
			ApplicationMode.BICYCLE,
			ApplicationMode.PEDESTRIAN
	};

	private static final int DEFAULT_ZOOM = 15;

	private static final int MAX_NAV_DRAWER_ITEMS_PER_APP = 3;

	private OsmandApplication app;
	private Map<String, AMapWidget> widgets = new ConcurrentHashMap<>();
	private Map<String, TextInfoWidget> widgetControls = new ConcurrentHashMap<>();
	private Map<String, AMapLayer> layers = new ConcurrentHashMap<>();
	private Map<String, OsmandMapLayer> mapLayers = new ConcurrentHashMap<>();
	private Map<String, BroadcastReceiver> receivers = new TreeMap<>();
	private Map<String, ConnectedApp> connectedApps = new ConcurrentHashMap<>();

	private boolean mapActivityActive = false;

	public OsmandAidlApi(OsmandApplication app) {
		this.app = app;
		loadConnectedApps();
	}

	public void onCreateMapActivity(MapActivity mapActivity) {
		mapActivityActive = true;
		registerRefreshMapReceiver(mapActivity);
		registerSetMapLocationReceiver(mapActivity);
		registerAddMapWidgetReceiver(mapActivity);
		registerRemoveMapWidgetReceiver(mapActivity);
		registerAddMapLayerReceiver(mapActivity);
		registerRemoveMapLayerReceiver(mapActivity);
		registerTakePhotoNoteReceiver(mapActivity);
		registerStartVideoRecordingReceiver(mapActivity);
		registerStartAudioRecordingReceiver(mapActivity);
		registerStopRecordingReceiver(mapActivity);
		registerNavigateReceiver(mapActivity);
		registerNavigateGpxReceiver(mapActivity);
		registerNavigateSearchReceiver(mapActivity);
		registerPauseNavigationReceiver(mapActivity);
		registerResumeNavigationReceiver(mapActivity);
		registerStopNavigationReceiver(mapActivity);
		registerMuteNavigationReceiver(mapActivity);
		registerUnmuteNavigationReceiver(mapActivity);
		registerShowSqliteDbFileReceiver(mapActivity);
		registerHideSqliteDbFileReceiver(mapActivity);
		initOsmandTelegram();
		app.getAppCustomization().addListener(mapActivity);
	}

	public void onDestroyMapActivity(MapActivity mapActivity) {
		app.getAppCustomization().removeListener(mapActivity);
		mapActivityActive = false;
		for (BroadcastReceiver b : receivers.values()) {
			if(b == null) {
				continue;
			}
			try {
				mapActivity.unregisterReceiver(b);
			} catch (IllegalArgumentException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		receivers = new TreeMap<>();
	}

	public boolean isUpdateAllowed() {
		return mapActivityActive;
	}

	private void initOsmandTelegram() {
		String[] packages = new String[]{"net.osmand.telegram", "net.osmand.telegram.debug"};
		Intent intent = new Intent("net.osmand.telegram.InitApp");
		for (String pack : packages) {
			intent.setComponent(new ComponentName(pack, "net.osmand.telegram.InitAppBroadcastReceiver"));
			app.sendBroadcast(intent);
		}
	}

	private void registerRefreshMapReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver refreshMapReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					mapActivity.refreshMap();
				}
			}
		};
		registerReceiver(refreshMapReceiver, mapActivity, AIDL_REFRESH_MAP);
	}

	private void registerSetMapLocationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver setMapLocationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					int zoom = intent.getIntExtra(AIDL_ZOOM, 0);
					boolean animated = intent.getBooleanExtra(AIDL_ANIMATED, false);
					if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
						OsmandMapTileView mapView = mapActivity.getMapView();
						if (zoom == 0) {
							zoom = mapView.getZoom();
						} else {
							zoom = zoom > mapView.getMaxZoom() ? mapView.getMaxZoom() : zoom;
							zoom = zoom < mapView.getMinZoom() ? mapView.getMinZoom() : zoom;
						}
						if (animated) {
							mapView.getAnimatedDraggingThread().startMoving(lat, lon, zoom, true);
						} else {
							mapView.setLatLon(lat, lon);
							mapView.setIntZoom(zoom);
						}
					}
					mapActivity.refreshMap();
				}
			}
		};
		registerReceiver(setMapLocationReceiver, mapActivity, AIDL_SET_MAP_LOCATION);
	}

	private int getDrawableId(String id) {
		if (Algorithms.isEmpty(id)) {
			return 0;
		} else {
			return app.getResources().getIdentifier(id, "drawable", app.getPackageName());
		}
	}

	private void registerAddMapWidgetReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver addMapWidgetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				String widgetId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (mapActivity != null && widgetId != null) {
					AMapWidget widget = widgets.get(widgetId);
					if (widget != null) {
						MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
						if (layer != null) {
							TextInfoWidget control = createWidgetControl(mapActivity, widgetId);
							widgetControls.put(widgetId, control);
							int menuIconId = getDrawableId(widget.getMenuIconName());
							MapWidgetRegInfo widgetInfo = layer.registerSideWidget(control,
									menuIconId, widget.getMenuTitle(), "aidl_widget_" + widgetId,
									false, widget.getOrder());
							if (!mapActivity.getMapLayers().getMapWidgetRegistry().isVisible(widgetInfo.key)) {
								mapActivity.getMapLayers().getMapWidgetRegistry().setVisibility(widgetInfo, true, false);
							}
							layer.recreateControls();
						}
					}
				}
			}
		};
		registerReceiver(addMapWidgetReceiver, mapActivity, AIDL_ADD_MAP_WIDGET);
	}

	private void registerReceiver(BroadcastReceiver rec, MapActivity ma,
			String filter) {
		receivers.put(filter, rec);
		ma.registerReceiver(rec, new IntentFilter(filter));
	}

	private void registerRemoveMapWidgetReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver removeMapWidgetReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				String widgetId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (mapActivity != null && widgetId != null) {
					MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
					TextInfoWidget widgetControl = widgetControls.get(widgetId);
					if (layer != null && widgetControl != null) {
						layer.removeSideWidget(widgetControl);
						widgetControls.remove(widgetId);
						layer.recreateControls();
					}
				}
			}
		};
		registerReceiver(removeMapWidgetReceiver, mapActivity, AIDL_REMOVE_MAP_WIDGET);
	}

	public void registerWidgetControls(MapActivity mapActivity) {
		for (AMapWidget widget : widgets.values()) {
			MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
			if (layer != null) {
				TextInfoWidget control = createWidgetControl(mapActivity, widget.getId());
				widgetControls.put(widget.getId(), control);
				int menuIconId = getDrawableId(widget.getMenuIconName());
				MapWidgetRegInfo widgetInfo = layer.registerSideWidget(control,
						menuIconId, widget.getMenuTitle(), "aidl_widget_" + widget.getId(),
						false, widget.getOrder());
				if (!mapActivity.getMapLayers().getMapWidgetRegistry().isVisible(widgetInfo.key)) {
					mapActivity.getMapLayers().getMapWidgetRegistry().setVisibility(widgetInfo, true, false);
				}
			}
		}
	}

	private void registerAddMapLayerReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver addMapLayerReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				String layerId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (mapActivity != null && layerId != null) {
					AMapLayer layer = layers.get(layerId);
					if (layer != null) {
						OsmandMapLayer mapLayer = mapLayers.get(layerId);
						if (mapLayer != null) {
							mapActivity.getMapView().removeLayer(mapLayer);
						}
						mapLayer = new AidlMapLayer(mapActivity, layer);
						mapActivity.getMapView().addLayer(mapLayer, layer.getZOrder());
						mapLayers.put(layerId, mapLayer);
					}
				}
			}
		};
		registerReceiver(addMapLayerReceiver, mapActivity, AIDL_ADD_MAP_LAYER);
	}

	private void registerRemoveMapLayerReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver removeMapLayerReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				String layerId = intent.getStringExtra(AIDL_OBJECT_ID);
				if (mapActivity != null && layerId != null) {
					OsmandMapLayer mapLayer = mapLayers.remove(layerId);
					if (mapLayer != null) {
						mapActivity.getMapView().removeLayer(mapLayer);
						mapActivity.refreshMap();
					}
				}
			}
		};
		registerReceiver(removeMapLayerReceiver, mapActivity, AIDL_REMOVE_MAP_LAYER);
	}

	private void registerTakePhotoNoteReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver takePhotoNoteReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.takePhoto(lat, lon, mapActivity, false, true);
				}
			}
		};
		registerReceiver(takePhotoNoteReceiver, mapActivity, AIDL_TAKE_PHOTO_NOTE);
	}

	private void registerStartVideoRecordingReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver startVideoRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.recordVideo(lat, lon, mapActivity, true);
				}
			}
		};
		registerReceiver(startVideoRecordingReceiver, mapActivity, AIDL_START_VIDEO_RECORDING);
	}

	private void registerStartAudioRecordingReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver startAudioRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					double lat = intent.getDoubleExtra(AIDL_LATITUDE, Double.NaN);
					double lon = intent.getDoubleExtra(AIDL_LONGITUDE, Double.NaN);
					plugin.recordAudio(lat, lon, mapActivity);
				}
			}
		};
		registerReceiver(startAudioRecordingReceiver, mapActivity, AIDL_START_AUDIO_RECORDING);
	}

	private void registerStopRecordingReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver stopRecordingReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				final AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
				if (mapActivity != null && plugin != null) {
					plugin.stopRecording(mapActivity, false);
				}
			}
		};
		registerReceiver(stopRecordingReceiver, mapActivity, AIDL_STOP_RECORDING);
	}

	private void registerNavigateReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver navigateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String profileStr = intent.getStringExtra(AIDL_PROFILE);
				final ApplicationMode profile = ApplicationMode.valueOfStringKey(profileStr, DEFAULT_PROFILE);
				boolean validProfile = false;
				for (ApplicationMode mode : VALID_PROFILES) {
					if (mode == profile) {
						validProfile = true;
						break;
					}
				}
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null && validProfile) {
					String startName = intent.getStringExtra(AIDL_START_NAME);
					if (Algorithms.isEmpty(startName)) {
						startName = "";
					}
					String destName = intent.getStringExtra(AIDL_DEST_NAME);
					if (Algorithms.isEmpty(destName)) {
						destName = "";
					}

					final LatLon start;
					final PointDescription startDesc;
					double startLat = intent.getDoubleExtra(AIDL_START_LAT, 0);
					double startLon = intent.getDoubleExtra(AIDL_START_LON, 0);
					if (startLat != 0 && startLon != 0) {
						start = new LatLon(startLat, startLon);
						startDesc = new PointDescription(PointDescription.POINT_TYPE_LOCATION, startName);
					} else {
						start = null;
						startDesc = null;
					}

					double destLat = intent.getDoubleExtra(AIDL_DEST_LAT, 0);
					double destLon = intent.getDoubleExtra(AIDL_DEST_LON, 0);
					final LatLon dest = new LatLon(destLat, destLon);
					final PointDescription destDesc = new PointDescription(PointDescription.POINT_TYPE_LOCATION, destName);

					final RoutingHelper routingHelper = app.getRoutingHelper();
					boolean force = intent.getBooleanExtra(AIDL_FORCE, true);
					if (routingHelper.isFollowingMode() && !force) {
						AlertDialog dlg = mapActivity.getMapActions().stopNavigationActionConfirm();
						dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

							@Override
							public void onDismiss(DialogInterface dialog) {
								MapActivity mapActivity = mapActivityRef.get();
								if (mapActivity != null && !routingHelper.isFollowingMode()) {
									ExternalApiHelper.startNavigation(mapActivity, start, startDesc, dest, destDesc, profile);
								}
							}
						});
					} else {
						ExternalApiHelper.startNavigation(mapActivity, start, startDesc, dest, destDesc, profile);
					}
				}
			}
		};
		registerReceiver(navigateReceiver, mapActivity, AIDL_NAVIGATE);
	}

	private void registerNavigateSearchReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver navigateSearchReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String profileStr = intent.getStringExtra(AIDL_PROFILE);
				final ApplicationMode profile = ApplicationMode.valueOfStringKey(profileStr, DEFAULT_PROFILE);
				boolean validProfile = false;
				for (ApplicationMode mode : VALID_PROFILES) {
					if (mode == profile) {
						validProfile = true;
						break;
					}
				}
				MapActivity mapActivity = mapActivityRef.get();
				final String searchQuery = intent.getStringExtra(AIDL_SEARCH_QUERY);
				if (mapActivity != null && validProfile && !Algorithms.isEmpty(searchQuery)) {
					String startName = intent.getStringExtra(AIDL_START_NAME);
					if (Algorithms.isEmpty(startName)) {
						startName = "";
					}

					final LatLon start;
					final PointDescription startDesc;
					double startLat = intent.getDoubleExtra(AIDL_START_LAT, 0);
					double startLon = intent.getDoubleExtra(AIDL_START_LON, 0);
					if (startLat != 0 && startLon != 0) {
						start = new LatLon(startLat, startLon);
						startDesc = new PointDescription(PointDescription.POINT_TYPE_LOCATION, startName);
					} else {
						start = null;
						startDesc = null;
					}

					final LatLon searchLocation;
					double searchLat = intent.getDoubleExtra(AIDL_SEARCH_LAT, 0);
					double searchLon = intent.getDoubleExtra(AIDL_SEARCH_LON, 0);
					if (searchLat != 0 && searchLon != 0) {
						searchLocation = new LatLon(searchLat, searchLon);
					} else {
						searchLocation = null;
					}

					if (searchLocation != null) {
						final RoutingHelper routingHelper = app.getRoutingHelper();
						boolean force = intent.getBooleanExtra(AIDL_FORCE, true);
						if (routingHelper.isFollowingMode() && !force) {
							AlertDialog dlg = mapActivity.getMapActions().stopNavigationActionConfirm();
							dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

								@Override
								public void onDismiss(DialogInterface dialog) {
									MapActivity mapActivity = mapActivityRef.get();
									if (mapActivity != null && !routingHelper.isFollowingMode()) {
										ExternalApiHelper.searchAndNavigate(mapActivity, searchLocation, start, startDesc, profile, searchQuery, false);
									}
								}
							});
						} else {
							ExternalApiHelper.searchAndNavigate(mapActivity, searchLocation, start, startDesc, profile, searchQuery, false);
						}
					}
				}
			}
		};
		registerReceiver(navigateSearchReceiver, mapActivity, AIDL_NAVIGATE_SEARCH);
	}

	private void registerNavigateGpxReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver navigateGpxReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					boolean force = intent.getBooleanExtra(AIDL_FORCE, false);
					GPXFile gpx = null;
					if (intent.getStringExtra(AIDL_DATA) != null) {
						String gpxStr = intent.getStringExtra(AIDL_DATA);
						if (!Algorithms.isEmpty(gpxStr)) {
							gpx = GPXUtilities.loadGPXFile(mapActivity, new ByteArrayInputStream(gpxStr.getBytes()));
						}
					} else if (intent.getParcelableExtra(AIDL_URI) != null) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							Uri gpxUri = intent.getParcelableExtra(AIDL_URI);

							ParcelFileDescriptor gpxParcelDescriptor = null;
							try {
								gpxParcelDescriptor = mapActivity.getContentResolver().openFileDescriptor(gpxUri, "r");
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							}
							if (gpxParcelDescriptor != null) {
								FileDescriptor fileDescriptor = gpxParcelDescriptor.getFileDescriptor();
								gpx = GPXUtilities.loadGPXFile(mapActivity, new FileInputStream(fileDescriptor));
							}
						}
					}

					if (gpx != null) {
						final RoutingHelper routingHelper = app.getRoutingHelper();
						if (routingHelper.isFollowingMode() && !force) {
							final GPXFile gpxFile = gpx;
							AlertDialog dlg = mapActivity.getMapActions().stopNavigationActionConfirm();
							dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {

								@Override
								public void onDismiss(DialogInterface dialog) {
									MapActivity mapActivity = mapActivityRef.get();
									if (mapActivity != null && !routingHelper.isFollowingMode()) {
										ExternalApiHelper.startNavigation(mapActivity, gpxFile);
									}
								}
							});
						} else {
							ExternalApiHelper.startNavigation(mapActivity, gpx);
						}
					}
				}
			}
		};
		registerReceiver(navigateGpxReceiver, mapActivity, AIDL_NAVIGATE_GPX);
	}

	private void registerPauseNavigationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver pauseNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					RoutingHelper routingHelper = mapActivity.getRoutingHelper();
					if (routingHelper.isRouteCalculated() && !routingHelper.isRoutePlanningMode()) {
						routingHelper.setRoutePlanningMode(true);
						routingHelper.setFollowingMode(false);
						routingHelper.setPauseNavigation(true);
					}
				}
			}
		};
		registerReceiver(pauseNavigationReceiver, mapActivity, AIDL_PAUSE_NAVIGATION);
	}

	private void registerResumeNavigationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver resumeNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					RoutingHelper routingHelper = mapActivity.getRoutingHelper();
					if (routingHelper.isRouteCalculated() && routingHelper.isRoutePlanningMode()) {
						routingHelper.setRoutePlanningMode(false);
						routingHelper.setFollowingMode(true);
					}
				}
			}
		};
		registerReceiver(resumeNavigationReceiver, mapActivity, AIDL_RESUME_NAVIGATION);
	}

	private void registerStopNavigationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver stopNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					RoutingHelper routingHelper = mapActivity.getRoutingHelper();
					if (routingHelper.isPauseNavigation() || routingHelper.isFollowingMode()) {
						mapActivity.getMapLayers().getMapControlsLayer().stopNavigationWithoutConfirm();
					}
				}
			}
		};
		registerReceiver(stopNavigationReceiver, mapActivity, AIDL_STOP_NAVIGATION);
	}

	private void registerMuteNavigationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver muteNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					mapActivity.getMyApplication().getSettings().VOICE_MUTE.set(true);
					mapActivity.getRoutingHelper().getVoiceRouter().setMute(true);
				}
			}
		};
		registerReceiver(muteNavigationReceiver, mapActivity, AIDL_MUTE_NAVIGATION);
	}

	private void registerUnmuteNavigationReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver unmuteNavigationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				MapActivity mapActivity = mapActivityRef.get();
				if (mapActivity != null) {
					mapActivity.getMyApplication().getSettings().VOICE_MUTE.set(false);
					mapActivity.getRoutingHelper().getVoiceRouter().setMute(false);
				}
			}
		};
		registerReceiver(unmuteNavigationReceiver, mapActivity, AIDL_UNMUTE_NAVIGATION);
	}

	private void registerShowSqliteDbFileReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver showSqliteDbFileReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				OsmandSettings settings = app.getSettings();
				String fileName = intent.getStringExtra(AIDL_FILE_NAME);
				if (!Algorithms.isEmpty(fileName)) {
					settings.MAP_OVERLAY.set(fileName);
					settings.MAP_OVERLAY_PREVIOUS.set(fileName);
					MapActivity mapActivity = mapActivityRef.get();
					if (mapActivity != null) {
						OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
						if (plugin != null) {
							plugin.updateMapLayers(mapActivity.getMapView(), settings.MAP_OVERLAY, mapActivity.getMapLayers());
						}
					}
				}
			}
		};
		registerReceiver(showSqliteDbFileReceiver, mapActivity, AIDL_SHOW_SQLITEDB_FILE);
	}

	private void registerHideSqliteDbFileReceiver(MapActivity mapActivity) {
		final WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);
		BroadcastReceiver hideSqliteDbFileReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				OsmandSettings settings = app.getSettings();
				String fileName = intent.getStringExtra(AIDL_FILE_NAME);
				if (!Algorithms.isEmpty(fileName) && fileName.equals(settings.MAP_OVERLAY.get())) {
					settings.MAP_OVERLAY.set(null);
					settings.MAP_OVERLAY_PREVIOUS.set(null);
					MapActivity mapActivity = mapActivityRef.get();
					if (mapActivity != null) {
						OsmandRasterMapsPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class);
						if (plugin != null) {
							plugin.updateMapLayers(mapActivity.getMapView(), settings.MAP_OVERLAY, mapActivity.getMapLayers());
						}
					}
				}
			}
		};
		registerReceiver(hideSqliteDbFileReceiver, mapActivity, AIDL_HIDE_SQLITEDB_FILE);
	}

	public void registerMapLayers(MapActivity mapActivity) {
		for (AMapLayer layer : layers.values()) {
			OsmandMapLayer mapLayer = mapLayers.get(layer.getId());
			if (mapLayer != null) {
				mapActivity.getMapView().removeLayer(mapLayer);
			}
			mapLayer = new AidlMapLayer(mapActivity, layer);
			mapActivity.getMapView().addLayer(mapLayer, layer.getZOrder());
			mapLayers.put(layer.getId(), mapLayer);
		}
	}

	private void refreshMap() {
		Intent intent = new Intent();
		intent.setAction(AIDL_REFRESH_MAP);
		app.sendBroadcast(intent);
	}

	private TextInfoWidget createWidgetControl(MapActivity mapActivity, final String widgetId) {
		final TextInfoWidget control = new TextInfoWidget(mapActivity) {

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				AMapWidget widget = widgets.get(widgetId);
				if (widget != null) {
					String txt = widget.getText();
					String subtxt = widget.getDescription();
					boolean night = drawSettings != null && drawSettings.isNightMode();
					int icon = night ? getDrawableId(widget.getDarkIconName()) : getDrawableId(widget.getLightIconName());
					setText(txt, subtxt);
					if (icon != 0) {
						setImageDrawable(icon);
					} else {
						setImageDrawable(null);
					}
					return true;
				} else {
					return false;
				}
			}
		};
		control.updateInfo(null);

		control.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AMapWidget widget = widgets.get(widgetId);
				if (widget != null && widget.getIntentOnClick() != null) {
					app.startActivity(widget.getIntentOnClick());
				}
			}
		});
		return control;
	}

	boolean reloadMap() {
		refreshMap();
		return true;
	}

	boolean addFavoriteGroup(AFavoriteGroup favoriteGroup) {
		if (favoriteGroup != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			List<FavouritesDbHelper.FavoriteGroup> groups = favoritesHelper.getFavoriteGroups();
			for (FavouritesDbHelper.FavoriteGroup g : groups) {
				if (g.name.equals(favoriteGroup.getName())) {
					return false;
				}
			}
			int color = 0;
			if (!Algorithms.isEmpty(favoriteGroup.getColor())) {
				color = ColorDialogs.getColorByTag(favoriteGroup.getColor());
			}
			favoritesHelper.addEmptyCategory(favoriteGroup.getName(), color, favoriteGroup.isVisible());
			return true;
		} else {
			return false;
		}
	}

	boolean removeFavoriteGroup(AFavoriteGroup favoriteGroup) {
		if (favoriteGroup != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			List<FavouritesDbHelper.FavoriteGroup> groups = favoritesHelper.getFavoriteGroups();
			for (FavouritesDbHelper.FavoriteGroup g : groups) {
				if (g.name.equals(favoriteGroup.getName())) {
					favoritesHelper.deleteGroup(g);
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean updateFavoriteGroup(AFavoriteGroup gPrev, AFavoriteGroup gNew) {
		if (gPrev != null && gNew != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			List<FavouritesDbHelper.FavoriteGroup> groups = favoritesHelper.getFavoriteGroups();
			for (FavouritesDbHelper.FavoriteGroup g : groups) {
				if (g.name.equals(gPrev.getName())) {
					int color = 0;
					if (!Algorithms.isEmpty(gNew.getColor())) {
						color = ColorDialogs.getColorByTag(gNew.getColor());
					}
					favoritesHelper.editFavouriteGroup(g, gNew.getName(), color, gNew.isVisible());
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean addFavorite(AFavorite favorite) {
		if (favorite != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			FavouritePoint point = new FavouritePoint(favorite.getLat(), favorite.getLon(), favorite.getName(), favorite.getCategory());
			point.setDescription(favorite.getDescription());
			int color = 0;
			if (!Algorithms.isEmpty(favorite.getColor())) {
				color = ColorDialogs.getColorByTag(favorite.getColor());
			}
			point.setColor(color);
			point.setVisible(favorite.isVisible());
			favoritesHelper.addFavourite(point);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeFavorite(AFavorite favorite) {
		if (favorite != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			List<FavouritePoint> favorites = favoritesHelper.getFavouritePoints();
			for (FavouritePoint f : favorites) {
				if (f.getName().equals(favorite.getName()) && f.getCategory().equals(favorite.getCategory()) &&
						f.getLatitude() == favorite.getLat() && f.getLongitude() == favorite.getLon()) {
					favoritesHelper.deleteFavourite(f);
					refreshMap();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean updateFavorite(AFavorite fPrev, AFavorite fNew) {
		if (fPrev != null && fNew != null) {
			FavouritesDbHelper favoritesHelper = app.getFavorites();
			List<FavouritePoint> favorites = favoritesHelper.getFavouritePoints();
			for (FavouritePoint f : favorites) {
				if (f.getName().equals(fPrev.getName()) && f.getCategory().equals(fPrev.getCategory()) &&
						f.getLatitude() == fPrev.getLat() && f.getLongitude() == fPrev.getLon()) {
					if (fNew.getLat() != f.getLatitude() || fNew.getLon() != f.getLongitude()) {
						favoritesHelper.editFavourite(f, fNew.getLat(), fNew.getLon());
					}
					if (!fNew.getName().equals(f.getName()) || !fNew.getDescription().equals(f.getDescription()) ||
							!fNew.getCategory().equals(f.getCategory())) {
						favoritesHelper.editFavouriteName(f, fNew.getName(), fNew.getCategory(), fNew.getDescription());
					}
					refreshMap();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean addMapMarker(AMapMarker marker) {
		if (marker != null) {
			PointDescription pd = new PointDescription(
					PointDescription.POINT_TYPE_MAP_MARKER, marker.getName() != null ? marker.getName() : "");
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			markersHelper.addMapMarker(new LatLon(marker.getLatLon().getLatitude(), marker.getLatLon().getLongitude()), pd);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeMapMarker(AMapMarker marker) {
		if (marker != null) {
			LatLon latLon = new LatLon(marker.getLatLon().getLatitude(), marker.getLatLon().getLongitude());
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			List<MapMarker> mapMarkers = markersHelper.getMapMarkers();
			for (MapMarker m : mapMarkers) {
				if (m.getOnlyName().equals(marker.getName()) && latLon.equals(new LatLon(m.getLatitude(), m.getLongitude()))) {
					markersHelper.moveMapMarkerToHistory(m);
					refreshMap();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean updateMapMarker(AMapMarker markerPrev, AMapMarker markerNew) {
		if (markerPrev != null && markerNew != null) {
			LatLon latLon = new LatLon(markerPrev.getLatLon().getLatitude(), markerPrev.getLatLon().getLongitude());
			LatLon latLonNew = new LatLon(markerNew.getLatLon().getLatitude(), markerNew.getLatLon().getLongitude());
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			List<MapMarker> mapMarkers = markersHelper.getMapMarkers();
			for (MapMarker m : mapMarkers) {
				if (m.getOnlyName().equals(markerPrev.getName()) && latLon.equals(new LatLon(m.getLatitude(), m.getLongitude()))) {
					PointDescription pd = new PointDescription(
							PointDescription.POINT_TYPE_MAP_MARKER, markerNew.getName() != null ? markerNew.getName() : "");
					MapMarker marker = new MapMarker(m.point, pd, m.colorIndex, m.selected, m.index);
					marker.id = m.id;
					marker.creationDate = m.creationDate;
					marker.visitedDate = m.visitedDate;
					markersHelper.moveMapMarker(marker, latLonNew);
					refreshMap();
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	boolean addMapWidget(AMapWidget widget) {
		if (widget != null) {
			if (widgets.containsKey(widget.getId())) {
				updateMapWidget(widget);
			} else {
				widgets.put(widget.getId(), widget);
				Intent intent = new Intent();
				intent.setAction(AIDL_ADD_MAP_WIDGET);
				intent.putExtra(AIDL_OBJECT_ID, widget.getId());
				app.sendBroadcast(intent);
			}
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeMapWidget(String widgetId) {
		if (!Algorithms.isEmpty(widgetId) && widgets.containsKey(widgetId)) {
			widgets.remove(widgetId);
			Intent intent = new Intent();
			intent.setAction(AIDL_REMOVE_MAP_WIDGET);
			intent.putExtra(AIDL_OBJECT_ID, widgetId);
			app.sendBroadcast(intent);
			return true;
		} else {
			return false;
		}
	}

	boolean updateMapWidget(AMapWidget widget) {
		if (widget != null && widgets.containsKey(widget.getId())) {
			widgets.put(widget.getId(), widget);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean addMapLayer(AMapLayer layer) {
		if (layer != null) {
			if (layers.containsKey(layer.getId())) {
				updateMapLayer(layer);
			} else {
				layers.put(layer.getId(), layer);
				Intent intent = new Intent();
				intent.setAction(AIDL_ADD_MAP_LAYER);
				intent.putExtra(AIDL_OBJECT_ID, layer.getId());
				app.sendBroadcast(intent);
			}
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean removeMapLayer(String layerId) {
		if (!Algorithms.isEmpty(layerId) && layers.containsKey(layerId)) {
			layers.remove(layerId);
			Intent intent = new Intent();
			intent.setAction(AIDL_REMOVE_MAP_LAYER);
			intent.putExtra(AIDL_OBJECT_ID, layerId);
			app.sendBroadcast(intent);
			return true;
		} else {
			return false;
		}
	}

	boolean updateMapLayer(AMapLayer layer) {
		if (layer != null && layers.containsKey(layer.getId())) {
			AMapLayer existingLayer = layers.get(layer.getId());
			for (AMapPoint point : layer.getPoints()) {
				existingLayer.putPoint(point);
			}
			existingLayer.copyZoomBounds(layer);
			refreshMap();
			return true;
		} else {
			return false;
		}
	}

	boolean showMapPoint(String layerId, AMapPoint point) {
		if (point != null) {
			if (!TextUtils.isEmpty(layerId)) {
				AMapLayer layer = layers.get(layerId);
				if (layer != null) {
					AMapPoint p = layer.getPoint(point.getId());
					if (p != null) {
						point = p;
					}
				}
			}
			app.getSettings().setMapLocationToShow(
					point.getLocation().getLatitude(),
					point.getLocation().getLongitude(),
					DEFAULT_ZOOM,
					new PointDescription(PointDescription.POINT_TYPE_MARKER, point.getFullName()),
					false,
					point
			);
			MapActivity.launchMapActivityMoveToTop(app);

			return true;
		}
		return false;
	}

	boolean putMapPoint(String layerId, AMapPoint point) {
		if (point != null) {
			AMapLayer layer = layers.get(layerId);
			if (layer != null) {
				layer.putPoint(point);
				refreshMap();
				return true;
			}
		}
		return false;
	}

	boolean removeMapPoint(String layerId, String pointId) {
		if (pointId != null) {
			AMapLayer layer = layers.get(layerId);
			if (layer != null) {
				layer.removePoint(pointId);
				refreshMap();
				return true;
			}
		}
		return false;
	}

	@SuppressLint("StaticFieldLeak")
	private void finishGpxImport(boolean destinationExists, File destination, String color, boolean show) {
		int col = ConfigureMapMenu.GpxAppearanceAdapter.parseTrackColor(
					app.getRendererRegistry().getCurrentSelectedRenderer(), color);
		if (!destinationExists) {
			GpxDataItem gpxDataItem = new GpxDataItem(destination, col);
			gpxDataItem.setApiImported(true);
			app.getGpxDatabase().add(gpxDataItem);
		} else {
			GpxDataItem item = app.getGpxDatabase().getItem(destination);
			if (item != null) {
				app.getGpxDatabase().updateColor(item, col);
			}
		}
		final GpxSelectionHelper helper = app.getSelectedGpxHelper();
		final SelectedGpxFile selectedGpx = helper.getSelectedFileByName(destination.getName());
		if (selectedGpx != null) {
			if (show) {
				new AsyncTask<File, Void, GPXFile>() {

					@Override
					protected GPXFile doInBackground(File... files) {
						return GPXUtilities.loadGPXFile(app, files[0]);
					}

					@Override
					protected void onPostExecute(GPXFile gpx) {
						if (gpx.warning == null) {
							selectedGpx.setGpxFile(gpx);
							refreshMap();
						}
					}

				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, destination);
			} else {
				helper.selectGpxFile(selectedGpx.getGpxFile(), false, false);
				refreshMap();
			}
		} else if (show) {
			new AsyncTask<File, Void, GPXFile>() {

				@Override
				protected GPXFile doInBackground(File... files) {
					return GPXUtilities.loadGPXFile(app, files[0]);
				}

				@Override
				protected void onPostExecute(GPXFile gpx) {
					if (gpx.warning == null) {
						helper.selectGpxFile(gpx, true, false);
						refreshMap();
					}
				}

			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, destination);
		}
	}

	boolean importGpxFromFile(File source, String destinationPath, String color, boolean show) {
		if (source != null && !Algorithms.isEmpty(destinationPath)) {
			if (source.exists() && source.canRead()) {
				File destination = app.getAppPath(IndexConstants.GPX_INDEX_DIR + destinationPath);
				if (destination.getParentFile().canWrite()) {
					boolean destinationExists = destination.exists();
					try {
						Algorithms.fileCopy(source, destination);
						finishGpxImport(destinationExists, destination, color, show);
						return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}

	boolean importGpxFromUri(Uri gpxUri, String destinationPath, String color, boolean show) {
		if (gpxUri != null && !Algorithms.isEmpty(destinationPath)) {
			File destination = app.getAppPath(IndexConstants.GPX_INDEX_DIR + destinationPath);
			ParcelFileDescriptor gpxParcelDescriptor;
			try {
				gpxParcelDescriptor = app.getContentResolver().openFileDescriptor(gpxUri, "r");
				if (gpxParcelDescriptor != null) {
					boolean destinationExists = destination.exists();
					FileDescriptor fileDescriptor = gpxParcelDescriptor.getFileDescriptor();
					InputStream is = new FileInputStream(fileDescriptor);
					FileOutputStream fout = new FileOutputStream(destination);
					try {
						Algorithms.streamCopy(is, fout);
						finishGpxImport(destinationExists, destination, color, show);
					} finally {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						try {
							fout.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	boolean importGpxFromData(String sourceRawData, String destinationPath, String color, boolean show) {
		if (!Algorithms.isEmpty(sourceRawData) && !Algorithms.isEmpty(destinationPath)) {
			File destination = app.getAppPath(IndexConstants.GPX_INDEX_DIR + destinationPath);
			try {
				InputStream is = new ByteArrayInputStream(sourceRawData.getBytes());
				FileOutputStream fout = new FileOutputStream(destination);
				boolean destinationExists = destination.exists();
				try {
					Algorithms.streamCopy(is, fout);
					finishGpxImport(destinationExists, destination, color, show);
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						fout.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@SuppressLint("StaticFieldLeak")
	boolean showGpx(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			File f = app.getAppPath(IndexConstants.GPX_INDEX_DIR + fileName);
			File fi = app.getAppPath(IndexConstants.GPX_IMPORT_DIR + fileName);
			AsyncTask<File, Void, GPXFile> asyncTask = new AsyncTask<File, Void, GPXFile>() {

				@Override
				protected GPXFile doInBackground(File... files) {
					return GPXUtilities.loadGPXFile(app, files[0]);
				}

				@Override
				protected void onPostExecute(GPXFile gpx) {
					if (gpx.warning == null) {
						app.getSelectedGpxHelper().selectGpxFile(gpx, true, false);
						refreshMap();
					}
				}
			};

			if (f.exists()) {
				asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, f);
				return true;
			} else if (fi.exists()) {
				asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fi);
				return true;
			}
		}
		return false;
	}

	boolean hideGpx(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByName(fileName);
			if (selectedGpxFile != null) {
				app.getSelectedGpxHelper().selectGpxFile(selectedGpxFile.getGpxFile(), false, false);
				refreshMap();
				return true;
			}
		}
		return false;
	}

	boolean getActiveGpx(List<ASelectedGpxFile> files) {
		if (files != null) {
			List<SelectedGpxFile> selectedGpxFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
			String gpxPath = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath();
			for (SelectedGpxFile selectedGpxFile : selectedGpxFiles) {
				GPXFile gpxFile = selectedGpxFile.getGpxFile();
				String path = gpxFile.path;
				if (!Algorithms.isEmpty(path)) {
					if (path.startsWith(gpxPath)) {
						path = path.substring(gpxPath.length() + 1);
					}
					long modifiedTime = gpxFile.modifiedTime;
					long fileSize = new File(gpxFile.path).length();
					files.add(new ASelectedGpxFile(path, modifiedTime, fileSize, createGpxFileDetails(selectedGpxFile.getTrackAnalysis())));
				}
			}
			return true;
		}
		return false;
	}

	boolean getImportedGpx(List<AGpxFile> files) {
		if (files != null) {
			List<GpxDataItem> gpxDataItems = app.getGpxDatabase().getItems();
			for (GpxDataItem dataItem : gpxDataItems) {
				//if (dataItem.isApiImported()) {
					File file = dataItem.getFile();
					if (file.exists()) {
						String fileName = file.getName();
						boolean active = app.getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath()) != null;
						long modifiedTime = dataItem.getFileLastModifiedTime();
						long fileSize = file.length();
						AGpxFileDetails details = null;
						GPXTrackAnalysis analysis = dataItem.getAnalysis();
						if (analysis != null) {
							details = createGpxFileDetails(analysis);
						}
						files.add(new AGpxFile(fileName, modifiedTime, fileSize, active, details));
					}
				//}
			}
			return true;
		}
		return false;
	}

	boolean removeGpx(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			final File f = app.getAppPath(IndexConstants.GPX_INDEX_DIR + fileName);
			if (f.exists()) {
				GpxDataItem item = app.getGpxDatabase().getItem(f);
				if (item != null && item.isApiImported()) {
					Algorithms.removeAllFiles(f);
					app.getGpxDatabase().remove(f);
					return true;
				}
			}
		}
		return false;
	}

	private boolean getSqliteDbFiles(List<ASqliteDbFile> fileNames, boolean activeOnly) {
		if (fileNames != null) {
			File tilesPath = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
			if (tilesPath.canRead()) {
				File[] files = tilesPath.listFiles();
				if (files != null) {
					String activeFile = app.getSettings().MAP_OVERLAY.get();
					for (File tileFile : files) {
						String fileName = tileFile.getName();
						String fileNameLC = fileName.toLowerCase();
						if (tileFile.isFile() && !fileNameLC.startsWith("hillshade") && fileNameLC.endsWith(SQLiteTileSource.EXT)) {
							boolean active = fileName.equals(activeFile);
							if (!activeOnly || active) {
								fileNames.add(new ASqliteDbFile(fileName, tileFile.lastModified(), tileFile.length(), active));
							}
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	boolean getSqliteDbFiles(List<ASqliteDbFile> fileNames) {
		return getSqliteDbFiles(fileNames, false);
	}

	boolean getActiveSqliteDbFiles(List<ASqliteDbFile> fileNames) {
		return getSqliteDbFiles(fileNames, true);
	}

	boolean showSqliteDbFile(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			File tileFile = new File(app.getAppPath(IndexConstants.TILES_INDEX_DIR), fileName);
			String fileNameLC = fileName.toLowerCase();
			if (tileFile.isFile() && !fileNameLC.startsWith("hillshade") && fileNameLC.endsWith(SQLiteTileSource.EXT)) {
				OsmandSettings settings = app.getSettings();
				settings.MAP_OVERLAY.set(fileName);
				settings.MAP_OVERLAY_PREVIOUS.set(fileName);

				Intent intent = new Intent();
				intent.setAction(AIDL_SHOW_SQLITEDB_FILE);
				intent.putExtra(AIDL_FILE_NAME, fileName);
				app.sendBroadcast(intent);
			}
			return true;
		}
		return false;
	}

	boolean hideSqliteDbFile(String fileName) {
		if (!Algorithms.isEmpty(fileName)) {
			if (fileName.equals(app.getSettings().MAP_OVERLAY.get())) {
				OsmandSettings settings = app.getSettings();
				settings.MAP_OVERLAY.set(null);
				settings.MAP_OVERLAY_PREVIOUS.set(null);

				Intent intent = new Intent();
				intent.setAction(AIDL_HIDE_SQLITEDB_FILE);
				intent.putExtra(AIDL_FILE_NAME, fileName);
				app.sendBroadcast(intent);
				return true;
			}
		}
		return false;
	}

	boolean setMapLocation(double latitude, double longitude, int zoom, boolean animated) {
		Intent intent = new Intent();
		intent.setAction(AIDL_SET_MAP_LOCATION);
		intent.putExtra(AIDL_LATITUDE, latitude);
		intent.putExtra(AIDL_LONGITUDE, longitude);
		intent.putExtra(AIDL_ZOOM, zoom);
		intent.putExtra(AIDL_ANIMATED, animated);
		app.sendBroadcast(intent);
		return true;
	}

	boolean startGpxRecording(StartGpxRecordingParams params) {
		final OsmandMonitoringPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			plugin.startGPXMonitoring(null);
			plugin.updateControl();
			return true;
		}
		return false;
	}

	boolean stopGpxRecording(StopGpxRecordingParams params) {
		final OsmandMonitoringPlugin plugin = OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
		if (plugin != null) {
			plugin.stopRecording();
			plugin.updateControl();
			return true;
		}
		return false;
	}

	boolean takePhotoNote(double latitude, double longitude) {
		Intent intent = new Intent();
		intent.setAction(AIDL_TAKE_PHOTO_NOTE);
		intent.putExtra(AIDL_LATITUDE, latitude);
		intent.putExtra(AIDL_LONGITUDE, longitude);
		app.sendBroadcast(intent);
		return true;
	}

	boolean startVideoRecording(double latitude, double longitude) {
		Intent intent = new Intent();
		intent.setAction(AIDL_START_VIDEO_RECORDING);
		intent.putExtra(AIDL_LATITUDE, latitude);
		intent.putExtra(AIDL_LONGITUDE, longitude);
		app.sendBroadcast(intent);
		return true;
	}

	boolean startAudioRecording(double latitude, double longitude) {
		Intent intent = new Intent();
		intent.setAction(AIDL_START_AUDIO_RECORDING);
		intent.putExtra(AIDL_LATITUDE, latitude);
		intent.putExtra(AIDL_LONGITUDE, longitude);
		app.sendBroadcast(intent);
		return true;
	}

	boolean stopRecording() {
		Intent intent = new Intent();
		intent.setAction(AIDL_STOP_RECORDING);
		app.sendBroadcast(intent);
		return true;
	}

	boolean navigate(String startName, double startLat, double startLon,
					 String destName, double destLat, double destLon,
					 String profile, boolean force) {
		Intent intent = new Intent();
		intent.setAction(AIDL_NAVIGATE);
		intent.putExtra(AIDL_START_NAME, startName);
		intent.putExtra(AIDL_START_LAT, startLat);
		intent.putExtra(AIDL_START_LON, startLon);
		intent.putExtra(AIDL_DEST_NAME, destName);
		intent.putExtra(AIDL_DEST_LAT, destLat);
		intent.putExtra(AIDL_DEST_LON, destLon);
		intent.putExtra(AIDL_PROFILE, profile);
		intent.putExtra(AIDL_FORCE, force);
		app.sendBroadcast(intent);
		return true;
	}

	boolean navigateSearch(String startName, double startLat, double startLon,
						   String searchQuery, double searchLat, double searchLon,
						   String profile, boolean force) {
		Intent intent = new Intent();
		intent.setAction(AIDL_NAVIGATE_SEARCH);
		intent.putExtra(AIDL_START_NAME, startName);
		intent.putExtra(AIDL_START_LAT, startLat);
		intent.putExtra(AIDL_START_LON, startLon);
		intent.putExtra(AIDL_SEARCH_QUERY, searchQuery);
		intent.putExtra(AIDL_SEARCH_LAT, searchLat);
		intent.putExtra(AIDL_SEARCH_LON, searchLon);
		intent.putExtra(AIDL_PROFILE, profile);
		intent.putExtra(AIDL_FORCE, force);
		app.sendBroadcast(intent);
		return true;
	}

	boolean pauseNavigation() {
		Intent intent = new Intent();
		intent.setAction(AIDL_PAUSE_NAVIGATION);
		app.sendBroadcast(intent);
		return true;
	}

	boolean resumeNavigation() {
		Intent intent = new Intent();
		intent.setAction(AIDL_RESUME_NAVIGATION);
		app.sendBroadcast(intent);
		return true;
	}

	boolean stopNavigation() {
		Intent intent = new Intent();
		intent.setAction(AIDL_STOP_NAVIGATION);
		app.sendBroadcast(intent);
		return true;
	}

	boolean muteNavigation() {
		Intent intent = new Intent();
		intent.setAction(AIDL_MUTE_NAVIGATION);
		app.sendBroadcast(intent);
		return true;
	}

	boolean unmuteNavigation() {
		Intent intent = new Intent();
		intent.setAction(AIDL_UNMUTE_NAVIGATION);
		app.sendBroadcast(intent);
		return true;
	}

	boolean navigateGpx(String data, Uri uri, boolean force) {
		Intent intent = new Intent();
		intent.setAction(AIDL_NAVIGATE_GPX);
		intent.putExtra(AIDL_DATA, data);
		intent.putExtra(AIDL_URI, uri);
		intent.putExtra(AIDL_FORCE, force);
		app.sendBroadcast(intent);
		return true;
	}

	boolean search(final String searchQuery, final int searchType, final double latitude, final double longitude,
				   final int radiusLevel, final int totalLimit, final SearchCompleteCallback callback) {
		if (Algorithms.isEmpty(searchQuery) || latitude == 0 || longitude == 0 || callback == null) {
			return false;
		}
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializer.AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					ExternalApiHelper.runSearch(app, searchQuery, searchType, latitude, longitude, radiusLevel, totalLimit, callback);
				}
			});
		} else {
			ExternalApiHelper.runSearch(app, searchQuery, searchType, latitude, longitude, radiusLevel, totalLimit, callback);
		}
		return true;
	}

	boolean setNavDrawerItems(String appPackage, List<net.osmand.aidl.navdrawer.NavDrawerItem> items) {
		if (!TextUtils.isEmpty(appPackage) && items != null) {
			if (items.isEmpty()) {
				clearNavDrawerItems(appPackage);
				return true;
			}
			List<NavDrawerItem> newItems = new ArrayList<>(MAX_NAV_DRAWER_ITEMS_PER_APP);
			boolean success = true;
			for (int i = 0; i < items.size() && i <= MAX_NAV_DRAWER_ITEMS_PER_APP; i++) {
				net.osmand.aidl.navdrawer.NavDrawerItem item = items.get(i);
				String name = item.getName();
				String uri = item.getUri();
				if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(uri)) {
					newItems.add(new NavDrawerItem(name, uri, item.getIconName(), item.getFlags()));
				} else {
					success = false;
					break;
				}
			}
			if (success) {
				saveNavDrawerItems(appPackage, newItems);
			}
			return success;
		}
		return false;
	}

	public void registerNavDrawerItems(final Activity activity, ContextMenuAdapter adapter) {
		PackageManager pm = activity.getPackageManager();
		for (Map.Entry<String, List<NavDrawerItem>> entry : getNavDrawerItems().entrySet()) {
			String appPackage = entry.getKey();
			for (NavDrawerItem item : entry.getValue()) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.uri));
				if (intent.resolveActivity(pm) == null) {
					intent = pm.getLaunchIntentForPackage(appPackage);
				}
				if (intent != null) {
					if (item.flags != -1) {
						intent.addFlags(item.flags);
					}
					final Intent finalIntent = intent;
					adapter.addItem(new ContextMenuItem.ItemBuilder()
							.setId(item.getId())
							.setTitle(item.name)
							.setIcon(getIconId(item.iconName))
							.setListener(new ContextMenuAdapter.ItemClickListener() {
								@Override
								public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int position, boolean isChecked, int[] viewCoordinates) {
									activity.startActivity(finalIntent);
									return true;
								}
							})
							.createItem());
				}
			}
		}
	}

	private int getIconId(@Nullable String iconName) {
		if (!TextUtils.isEmpty(iconName)) {
			int id = app.getResources().getIdentifier(iconName, "drawable", app.getPackageName());
			return id == 0 ? -1 : id;
		}
		return -1;
	}

	private void clearNavDrawerItems(String appPackage) {
		try {
			JSONObject allItems = new JSONObject(app.getSettings().API_NAV_DRAWER_ITEMS_JSON.get());
			allItems.put(appPackage, null);
			app.getSettings().API_NAV_DRAWER_ITEMS_JSON.set(allItems.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void saveNavDrawerItems(String appPackage, List<NavDrawerItem> items) {
		try {
			JSONArray jArray = new JSONArray();
			for (NavDrawerItem item : items) {
				JSONObject obj = new JSONObject();
				obj.put(NavDrawerItem.NAME_KEY, item.name);
				obj.put(NavDrawerItem.URI_KEY, item.uri);
				obj.put(NavDrawerItem.ICON_NAME_KEY, item.iconName);
				obj.put(NavDrawerItem.FLAGS_KEY, item.flags);
				jArray.put(obj);
			}
			JSONObject allItems = new JSONObject(app.getSettings().API_NAV_DRAWER_ITEMS_JSON.get());
			allItems.put(appPackage, jArray);
			app.getSettings().API_NAV_DRAWER_ITEMS_JSON.set(allItems.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private Map<String, List<NavDrawerItem>> getNavDrawerItems() {
		Map<String, List<NavDrawerItem>> res = new LinkedHashMap<>();
		try {
			JSONObject allItems = new JSONObject(app.getSettings().API_NAV_DRAWER_ITEMS_JSON.get());
			for (Iterator<?> it = allItems.keys(); it.hasNext(); ) {
				String appPackage = (String) it.next();
				JSONArray jArray = allItems.getJSONArray(appPackage);
				List<NavDrawerItem> list = new ArrayList<>();
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject obj = jArray.getJSONObject(i);
					list.add(new NavDrawerItem(
							obj.optString(NavDrawerItem.NAME_KEY),
							obj.optString(NavDrawerItem.URI_KEY),
							obj.optString(NavDrawerItem.ICON_NAME_KEY),
							obj.optInt(NavDrawerItem.FLAGS_KEY, -1)
					));
				}
				res.put(appPackage, list);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return res;
	}

	public List<ConnectedApp> getConnectedApps() {
		List<ConnectedApp> res = new ArrayList<>(connectedApps.size());
		PackageManager pm = app.getPackageManager();
		for (ConnectedApp app : connectedApps.values()) {
			try {
				ApplicationInfo ai = pm.getPackageInfo(app.pack, 0).applicationInfo;
				app.name = ai.loadLabel(pm).toString();
				app.icon = ai.loadIcon(pm);
				res.add(app);
			} catch (PackageManager.NameNotFoundException e) {
				// ignore
			}
		}
		Collections.sort(res);
		return res;
	}

	public void switchEnabled(@NonNull ConnectedApp app) {
		app.enabled = !app.enabled;
		saveConnectedApps();
	}

	boolean isAppEnabled(@NonNull String pack) {
		ConnectedApp app = connectedApps.get(pack);
		if (app == null) {
			app = new ConnectedApp(pack, true);
			connectedApps.put(pack, app);
			saveConnectedApps();
		}
		return app.enabled;
	}

	private void saveConnectedApps() {
		try {
			JSONArray array = new JSONArray();
			for (ConnectedApp app : connectedApps.values()) {
				JSONObject obj = new JSONObject();
				obj.put(ConnectedApp.ENABLED_KEY, app.enabled);
				obj.put(ConnectedApp.PACK_KEY, app.pack);
				array.put(obj);
			}
			app.getSettings().API_CONNECTED_APPS_JSON.set(array.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void loadConnectedApps() {
		try {
			JSONArray array = new JSONArray(app.getSettings().API_CONNECTED_APPS_JSON.get());
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.getJSONObject(i);
				String pack = obj.optString(ConnectedApp.PACK_KEY, "");
				boolean enabled = obj.optBoolean(ConnectedApp.ENABLED_KEY, true);
				connectedApps.put(pack, new ConnectedApp(pack, enabled));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	boolean setNavDrawerLogo(@Nullable String uri) {
		return app.getAppCustomization().setNavDrawerLogo(uri);
	}

	boolean setEnabledIds(Collection<String> ids) {
		app.getAppCustomization().setFeaturesEnabledIds(ids);
		return true;
	}

	boolean setDisabledIds(Collection<String> ids) {
		app.getAppCustomization().setFeaturesDisabledIds(ids);
		return true;
	}

	boolean setEnabledPatterns(Collection<String> patterns) {
		app.getAppCustomization().setFeaturesEnabledPatterns(patterns);
		return true;
	}

	boolean setDisabledPatterns(Collection<String> patterns) {
		app.getAppCustomization().setFeaturesDisabledPatterns(patterns);
		return true;
	}

	boolean regWidgetVisibility(@NonNull String widgetId, @Nullable List<String> appModeKeys) {
		app.getAppCustomization().regWidgetVisibility(widgetId, appModeKeys);
		return true;
	}

	boolean regWidgetAvailability(@NonNull String widgetId, @Nullable List<String> appModeKeys) {
		app.getAppCustomization().regWidgetAvailability(widgetId, appModeKeys);
		return true;
	}

	boolean customizeOsmandSettings(@NonNull String sharedPreferencesName, @Nullable Bundle bundle) {
		app.getAppCustomization().customizeOsmandSettings(sharedPreferencesName, bundle);
		return true;
	}

	private static AGpxFileDetails createGpxFileDetails(@NonNull GPXTrackAnalysis a) {
		return new AGpxFileDetails(a.totalDistance, a.totalTracks, a.startTime, a.endTime,
				a.timeSpan, a.timeMoving, a.totalDistanceMoving, a.diffElevationUp, a.diffElevationDown,
				a.avgElevation, a.minElevation, a.maxElevation, a.minSpeed, a.maxSpeed, a.avgSpeed,
				a.points, a.wptPoints, a.wptCategoryNames);
	}

	public static class ConnectedApp implements Comparable<ConnectedApp> {

		static final String PACK_KEY = "pack";
		static final String ENABLED_KEY = "enabled";

		private String pack;
		private boolean enabled;
		private String name;
		private Drawable icon;

		ConnectedApp(String pack, boolean enabled) {
			this.pack = pack;
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public String getName() {
			return name;
		}

		public Drawable getIcon() {
			return icon;
		}

		@Override
		public int compareTo(@NonNull ConnectedApp app) {
			if (name != null && app.name != null) {
				return name.compareTo(app.name);
			}
			return 0;
		}
	}

	private static class NavDrawerItem {

		static final String NAME_KEY = "name";
		static final String URI_KEY = "uri";
		static final String ICON_NAME_KEY = "icon_name";
		static final String FLAGS_KEY = "flags";

		private String name;
		private String uri;
		private String iconName;
		private int flags;

		NavDrawerItem(String name, String uri, String iconName, int flags) {
			this.name = name;
			this.uri = uri;
			this.iconName = iconName;
			this.flags = flags;
		}

		public String getId() {
			return DRAWER_ITEM_ID_SCHEME + name;
		}
	}

	public interface SearchCompleteCallback {
		void onSearchComplete(List<SearchResult> resultSet);
	}
}
