package com.shaua.mocklocation;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shaua.mocklocation.data.FavoriteLocation;
import com.shaua.mocklocation.data.LocationRepository;
import com.shaua.mocklocation.database.DataBaseHistorySearch;
import com.shaua.mocklocation.service.MockLocationService;
import com.shaua.mocklocation.utils.GoUtils;
import com.shaua.mocklocation.utils.MapUtils;
import com.shaua.mocklocation.utils.ShareUtils;

import com.elvishew.xlog.XLog;

import io.noties.markwon.Markwon;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends BaseActivity
        implements FavoritesFragment.OnFavoriteSelectedListener, MineFragment.OnMineActionListener {

    /* 对外 */
    public static final String LAT_MSG_ID = "LAT_VALUE";
    public static final String LNG_MSG_ID = "LNG_VALUE";
    public static final String ALT_MSG_ID = "ALT_VALUE";

    public static final String POI_NAME = "POI_NAME";
    public static final String POI_ADDRESS = "POI_ADDRESS";
    public static final String POI_LONGITUDE = "POI_LONGITUDE";
    public static final String POI_LATITUDE = "POI_LATITUDE";

    /** 从收藏页传入的位置（BD09） */
    public static final String FAV_LNG = "FAV_LNG";
    public static final String FAV_LAT = "FAV_LAT";
    public static final String FAV_NAME = "FAV_NAME";

    public static final BitmapDescriptor mMapIndicator = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);

    private MainViewModel viewModel;
    private OkHttpClient mOkHttpClient;
    private SharedPreferences sharedPreferences;
    private LocationRepository locationRepository;

    /*============================== 主界面地图 相关 ==============================*/
    private MapView mMapView;
    private BaiduMap mBaiduMap = null;
    private GeoCoder mGeoCoder;
    /************** 定位 *****************/
    private LocationClient mLocClient = null;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private boolean isFirstLoc = true;
    private MockLocationService.MockLocationBinder mServiceBinder;
    private ServiceConnection mConnection;
    private FloatingActionButton mButtonStart;
    /*============================== 搜索历史 相关 ==============================*/
    private android.database.sqlite.SQLiteDatabase mSearchHistoryDB;
    /*============================== SearchView 相关 ==============================*/
    private SearchView searchView;
    private ListView mSearchList;
    private LinearLayout mSearchLayout;
    private ListView mSearchHistoryList;
    private LinearLayout mHistoryLayout;
    private MenuItem searchItem;
    private SuggestionSearch mSuggestionSearch;
    /*============================== 底部导航栏 相关 ==============================*/
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNav;
    private FrameLayout contentContainer;
    /*============================== 更新 相关 ==============================*/
    private DownloadManager mDownloadManager = null;
    private long mDownloadId;
    private BroadcastReceiver mDownloadBdRcv;
    private String mUpdateFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 使用 OnBackPressedDispatcher 处理返回手势（兼容 Android 13+ 预测性返回）
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(false);
            }
        });

        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        XLog.i("MainActivity: onCreate");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mOkHttpClient = new OkHttpClient();
        locationRepository = new LocationRepository(this);

        initBottomNav();
        initMap();
        initMapLocation();
        initMapButton();
        initGoBtn();

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mServiceBinder = (MockLocationService.MockLocationBinder) service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };

        initStoreHistory();
        initSearchView();
        initUpdateVersion();
        checkUpdateVersion(false);
    }

    @Override
    protected void onPause() {
        XLog.i("MainActivity: onPause");
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        XLog.i("MainActivity: onResume");
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onStop() {
        XLog.i("MainActivity: onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        XLog.i("MainActivity: onDestroy");

        if (Boolean.TRUE.equals(viewModel.getMockRunning().getValue())) {
            unbindService(mConnection);
            Intent serviceGoIntent = new Intent(MainActivity.this, MockLocationService.class);
            stopService(serviceGoIntent);
        }
        unregisterReceiver(mDownloadBdRcv);

        mLocClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mSuggestionSearch.destroy();
        mSearchHistoryDB.close();

        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleFavoriteLocation(intent);
    }

    /**
     * 处理从收藏页传入的位置并在地图上标记
     */
    private void handleFavoriteLocation(Intent intent) {
        if (intent == null) return;
        String lngStr = intent.getStringExtra(FAV_LNG);
        String latStr = intent.getStringExtra(FAV_LAT);
        String name = intent.getStringExtra(FAV_NAME);
        if (lngStr == null || latStr == null) return;
        try {
            LatLng target = new LatLng(Double.parseDouble(latStr), Double.parseDouble(lngStr));
            markLocation(target);
            if (name != null) {
                viewModel.setMarkedName(name);
            }
            MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(target);
            mBaiduMap.setMapStatus(mapstatusupdate);
        } catch (NumberFormatException e) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.history_error_location));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        searchItem = menu.findItem(R.id.action_search);
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSearchLayout.setVisibility(View.INVISIBLE);
                mHistoryLayout.setVisibility(View.INVISIBLE);
                // 恢复底部导航栏
                if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
                if (contentContainer != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) contentContainer.getLayoutParams();
                    lp.bottomMargin = (int) (80 * getResources().getDisplayMetrics().density);
                    contentContainer.setLayoutParams(lp);
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchLayout.setVisibility(View.INVISIBLE);
                // 隐藏底部导航栏，避免遮挡搜索面板
                if (bottomNav != null) bottomNav.setVisibility(View.GONE);
                if (contentContainer != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) contentContainer.getLayoutParams();
                    lp.bottomMargin = 0;
                    contentContainer.setLayoutParams(lp);
                }
                List<Map<String, Object>> data = getSearchHistory();
                if (!data.isEmpty()) {
                    SimpleAdapter simAdapt = new SimpleAdapter(
                            MainActivity.this, data, R.layout.search_item,
                            new String[]{DataBaseHistorySearch.DB_COLUMN_KEY,
                                    DataBaseHistorySearch.DB_COLUMN_DESCRIPTION,
                                    DataBaseHistorySearch.DB_COLUMN_TIMESTAMP,
                                    DataBaseHistorySearch.DB_COLUMN_IS_LOCATION,
                                    DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM,
                                    DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM},
                            new int[]{R.id.search_key, R.id.search_description,
                                    R.id.search_timestamp, R.id.search_isLoc,
                                    R.id.search_longitude, R.id.search_latitude});
                    mSearchHistoryList.setAdapter(simAdapt);
                    mHistoryLayout.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });

        searchView = (SearchView) searchItem.getActionView();
        searchView.setIconified(false);
        searchView.onActionViewExpanded();
        searchView.setIconifiedByDefault(true);
        searchView.setSubmitButtonEnabled(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                requestSuggestion(query);
                saveSearchKeyword(query);
                mBaiduMap.clear();
                mSearchLayout.setVisibility(View.INVISIBLE);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mHistoryLayout.setVisibility(View.INVISIBLE);
                if (newText != null && !newText.isEmpty()) {
                    requestSuggestion(newText);
                }
                return true;
            }
        });

        ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setOnClickListener(v -> {
            EditText et = findViewById(androidx.appcompat.R.id.search_src_text);
            et.setText("");
            searchView.setQuery("", false);
            mSearchLayout.setVisibility(View.INVISIBLE);
            mHistoryLayout.setVisibility(View.VISIBLE);
        });

        return true;
    }

    /*============================== 主界面地图 相关 ==============================*/
    private void initMap() {
        mMapView = findViewById(R.id.bdMapView);
        mMapView.showZoomControls(false);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setMyLocationEnabled(true);

        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                markLocation(point);
            }

            @Override
            public void onMapPoiClick(MapPoi poi) {
                markLocation(poi.getPosition());
            }
        });

        mBaiduMap.setOnMapLongClickListener(point -> {
            markLocation(point);
            mGeoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(point));
        });

        mBaiduMap.setOnMapDoubleClickListener(point ->
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.zoomIn()));

        View poiView = View.inflate(MainActivity.this, R.layout.location_poi_info, null);
        TextView poiAddress = poiView.findViewById(R.id.poi_address);
        TextView poiLongitude = poiView.findViewById(R.id.poi_longitude);
        TextView poiLatitude = poiView.findViewById(R.id.poi_latitude);

        ImageButton ibSave = poiView.findViewById(R.id.poi_save);
        ibSave.setOnClickListener(v -> {
            LatLng marked = viewModel.getMarkedLocation().getValue();
            if (marked != null) {
                saveFavoriteLocation(marked.longitude, marked.latitude);
                GoUtils.DisplayToast(this, getResources().getString(R.string.app_location_save));
            }
        });

        ImageButton ibCopy = poiView.findViewById(R.id.poi_copy);
        ibCopy.setOnClickListener(v -> {
            LatLng marked = viewModel.getMarkedLocation().getValue();
            if (marked != null) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Label", marked.toString()));
                GoUtils.DisplayToast(this, getResources().getString(R.string.app_location_copy));
            }
        });

        ImageButton ibShare = poiView.findViewById(R.id.poi_share);
        ibShare.setOnClickListener(v -> ShareUtils.shareText(MainActivity.this, "分享位置",
                poiLongitude.getText() + "," + poiLatitude.getText()));

        ImageButton ibFly = poiView.findViewById(R.id.poi_fly);
        ibFly.setOnClickListener(this::doGoLocation);

        mGeoCoder = GeoCoder.newInstance();
        mGeoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
                XLog.i(String.valueOf(geoCodeResult.getLocation()));
            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    XLog.i("逆地理位置失败!");
                } else {
                    viewModel.setMarkedName(String.valueOf(result.getAddress()));
                    poiLatitude.setText(String.valueOf(result.getLocation().latitude));
                    poiLongitude.setText(String.valueOf(result.getLocation().longitude));
                    poiAddress.setText(result.getAddress());
                    InfoWindow mInfoWindow = new InfoWindow(poiView, result.getLocation(), -100);
                    mBaiduMap.showInfoWindow(mInfoWindow);
                }
            }
        });
    }

    private void initMapLocation() {
        try {
            mLocClient = new LocationClient(this);
            mLocClient.registerLocationListener(new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation bdLocation) {
                    if (bdLocation == null || mMapView == null) {
                        return;
                    }

                    String city = bdLocation.getCity();
                    if (city != null && !city.isEmpty()) {
                        viewModel.setCurrentCity(city);
                    }
                    mCurrentLat = bdLocation.getLatitude();
                    mCurrentLon = bdLocation.getLongitude();

                    MyLocationData locData = new MyLocationData.Builder()
                            .accuracy(bdLocation.getRadius())
                            .latitude(bdLocation.getLatitude())
                            .longitude(bdLocation.getLongitude()).build();
                    mBaiduMap.setMyLocationData(locData);

                    int err = bdLocation.getLocType();
                    if (err == BDLocation.TypeCriteriaException || err == BDLocation.TypeNetWorkException) {
                        mLocClient.requestLocation();
                    } else if (isFirstLoc) {
                        isFirstLoc = false;
                        viewModel.setMarkedLocation(new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude()));
                        MapStatus.Builder builder = new MapStatus.Builder();
                        builder.target(viewModel.getMarkedLocation().getValue()).zoom(18.0f);
                        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                        XLog.i("First Baidu LatLng: " + viewModel.getMarkedLocation().getValue());
                    }
                }

                @Override
                public void onLocDiagnosticMessage(int locType, int diagnosticType, String diagnosticMessage) {
                    XLog.i("Baidu ERROR: " + locType + "-" + diagnosticType + "-" + diagnosticMessage);
                }
            });
            mLocClient.setLocOption(getLocationClientOption());
            mLocClient.start();
        } catch (Exception e) {
            XLog.e("ERROR: initMapLocation");
        }
    }

    @NonNull
    private static LocationClientOption getLocationClientOption() {
        LocationClientOption locationOption = new LocationClientOption();
        locationOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        locationOption.setCoorType("bd09ll");
        locationOption.setScanSpan(1000);
        locationOption.setIsNeedAddress(true);
        locationOption.setNeedDeviceDirect(false);
        locationOption.setLocationNotify(true);
        locationOption.setIgnoreKillProcess(true);
        locationOption.setIsNeedLocationDescribe(false);
        locationOption.setIsNeedLocationPoiList(false);
        locationOption.SetIgnoreCacheException(true);
        locationOption.setOpenGnss(true);
        locationOption.setIsNeedAltitude(false);
        return locationOption;
    }

    private void initMapButton() {
        RadioGroup mGroupMapType = findViewById(R.id.RadioGroupMapType);
        mGroupMapType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.mapNormal) {
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            } else if (checkedId == R.id.mapSatellite) {
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            }
        });

        findViewById(R.id.cur_position).setOnClickListener(v -> resetMap());
        findViewById(R.id.zoom_in).setOnClickListener(v -> mBaiduMap.animateMapStatus(MapStatusUpdateFactory.zoomIn()));
        findViewById(R.id.zoom_out).setOnClickListener(v -> mBaiduMap.animateMapStatus(MapStatusUpdateFactory.zoomOut()));
        findViewById(R.id.input_pos).setOnClickListener(v -> showInputPositionDialog());
    }

    private void showInputPositionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("请输入经度和纬度");
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.location_input, null);
        builder.setView(view);
        AlertDialog dialog = builder.show();

        EditText dialog_coord = view.findViewById(R.id.input_coord_string);
        EditText dialog_lng = view.findViewById(R.id.input_longitude);
        EditText dialog_lat = view.findViewById(R.id.input_latitude);
        RadioButton rbBD = view.findViewById(R.id.pos_type_bd);

        // 坐标串输入框：实时解析并自动填入下方经度/纬度
        final boolean[] isApplying = {false};
        dialog_coord.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isApplying[0]) return;
                String src = s.toString().trim();
                if (src.isEmpty()) return;
                // 仅当包含非数字字符（说明是坐标串）才尝试解析
                if (!src.matches(".*[^\\d.\\-+].*")) return;

                double[] result = tryParseCoordinatePair(src);
                if (result != null) {
                    isApplying[0] = true;
                    dialog_lng.setText(String.valueOf(result[0]));
                    dialog_lat.setText(String.valueOf(result[1]));
                    isApplying[0] = false;
                    GoUtils.DisplayToast(MainActivity.this, "已解析为经度 " + result[0] + "，纬度 " + result[1]);
                }
            }
        });

        view.findViewById(R.id.input_position_ok).setOnClickListener(v2 -> {
            String lngStr = dialog_lng.getText().toString().trim();
            String latStr = dialog_lat.getText().toString().trim();

            if (TextUtils.isEmpty(lngStr) || TextUtils.isEmpty(latStr)) {
                GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.app_error_input));
                return;
            }

            double lng, lat;
            try {
                lng = Double.parseDouble(lngStr);
                lat = Double.parseDouble(latStr);
            } catch (NumberFormatException e) {
                GoUtils.DisplayToast(MainActivity.this, "经纬度格式不正确，请输入数字");
                return;
            }

            LatLng target;
            if (rbBD.isChecked()) {
                target = new LatLng(lat, lng);
            } else {
                double[] bdLonLat = MapUtils.wgs2bd09(lng, lat);
                target = new LatLng(bdLonLat[1], bdLonLat[0]);
            }
            markLocation(target);
            MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(target);
            mBaiduMap.setMapStatus(mapstatusupdate);
            dialog.dismiss();
        });

        view.findViewById(R.id.input_position_cancel).setOnClickListener(v1 -> dialog.dismiss());
    }

    /**
     * 尝试解析坐标串，返回 [经度, 纬度]，无法解析返回 null
     * 支持格式：
     *   latitude: 31.365, longitude: 118.486  （纬度在前）
     *   lat/lng: (31.365,118.486)              （纬度在前，百度格式）
     *   118.486,31.365 或 118.486 31.365       （经度在前）
     */
    private double[] tryParseCoordinatePair(String src) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[-+]?\\d+\\.?\\d*").matcher(src);
            java.util.List<Double> nums = new java.util.ArrayList<>();
            while (m.find()) {
                nums.add(Double.parseDouble(m.group()));
            }
            if (nums.size() < 2) return null;

            // 纬度在前的格式（包含 latitude 或 lat/lng 或括号）
            if (src.contains("latitude") || src.contains("lat/lng") || src.contains("(")) {
                return new double[]{nums.get(1), nums.get(0)}; // [经度, 纬度]
            }
            // 经度在前的格式（逗号/空格分隔）
            return new double[]{nums.get(0), nums.get(1)};
        } catch (Exception e) {
            return null;
        }
    }

    private void markLocation(LatLng point) {
        viewModel.setMarkedLocation(point);
        MarkerOptions ooA = new MarkerOptions().position(point).icon(mMapIndicator);
        mBaiduMap.clear();
        mBaiduMap.addOverlay(ooA);
    }

    /*============================== 底部导航 相关 ==============================*/
    private FavoritesFragment favoritesFragment;
    private MineFragment mineFragment;

    private void initBottomNav() {
        bottomNav = findViewById(R.id.bottom_nav);
        contentContainer = findViewById(R.id.content_container);

        // 底部导航选中颜色随主题变化
        int primaryColor = GoUtils.getThemePrimaryColor(this);
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] colors = new int[]{primaryColor, GoUtils.getThemeOnSurfaceVariantColor(this)};
        android.content.res.ColorStateList navColorList = new android.content.res.ColorStateList(states, colors);
        bottomNav.setItemIconTintList(navColorList);
        bottomNav.setItemTextColor(navColorList);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                showMap();
            } else if (id == R.id.nav_favorites) {
                showFavorites();
            } else if (id == R.id.nav_mine) {
                showMine();
            }
            return true;
        });

        // 适配系统导航栏 inset，避免图标/文字被系统导航条遮挡
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            int navBarInset = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), navBarInset);
            return insets;
        });

        // 默认显示地图
        showMap();
    }

    private void showMap() {
        View mapContent = findViewById(R.id.map_content);
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (mapContent != null) mapContent.setVisibility(View.VISIBLE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
    }

    private void showFavorites() {
        View mapContent = findViewById(R.id.map_content);
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (mapContent != null) mapContent.setVisibility(View.GONE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
        if (favoritesFragment == null) {
            favoritesFragment = new FavoritesFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, favoritesFragment)
                .commit();
    }

    private void showMine() {
        View mapContent = findViewById(R.id.map_content);
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (mapContent != null) mapContent.setVisibility(View.GONE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
        if (mineFragment == null) {
            mineFragment = new MineFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mineFragment)
                .commit();
    }

    @Override
    public void onFavoriteSelected(LatLng latLng, String name) {
        // 切换到地图并定位到该位置
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_map);
        showMap();
        markLocation(latLng);
        MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.setMapStatus(mapstatusupdate);
    }

    @Override
    public void onCheckUpdate() {
        checkUpdateVersion(true);
    }

    private void resetMap() {
        mBaiduMap.clear();
        viewModel.setMarkedLocation(null);

        MyLocationData locData = new MyLocationData.Builder()
                .latitude(mCurrentLat)
                .longitude(mCurrentLon)
                .build();
        mBaiduMap.setMyLocationData(locData);

        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(new LatLng(mCurrentLat, mCurrentLon)).zoom(18.0f);
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }

    private void initGoBtn() {
        mButtonStart = findViewById(R.id.faBtnStart);
        mButtonStart.setOnClickListener(this::doGoLocation);

        viewModel.getMockRunning().observe(this, running -> {
            ActionBar actionBar = getSupportActionBar();
            if (running) {
                mButtonStart.setImageResource(R.drawable.ic_fly);
                if (actionBar != null) {
                    actionBar.setSubtitle("● 模拟位置运行中");
                }
            } else {
                mButtonStart.setImageResource(R.drawable.ic_position);
                if (actionBar != null) {
                    actionBar.setSubtitle(null);
                }
            }
        });
    }

    private void startGoLocation() {
        LatLng marked = viewModel.getMarkedLocation().getValue();
        if (marked == null) {
            return;
        }
        Intent serviceIntent = new Intent(MainActivity.this, MockLocationService.class);
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
        double[] latLng = MapUtils.bd2wgs(marked.longitude, marked.latitude);
        serviceIntent.putExtra(LNG_MSG_ID, latLng[0]);
        serviceIntent.putExtra(LAT_MSG_ID, latLng[1]);
        double alt = Double.parseDouble(sharedPreferences.getString("setting_altitude", "55.0"));
        serviceIntent.putExtra(ALT_MSG_ID, alt);

        startForegroundService(serviceIntent);
        XLog.d("startForegroundService: MockLocationService");
        viewModel.setMockRunning(true);
    }

    private void stopGoLocation() {
        unbindService(mConnection);
        Intent serviceIntent = new Intent(MainActivity.this, MockLocationService.class);
        stopService(serviceIntent);
        viewModel.setMockRunning(false);
    }

    private void doGoLocation(View v) {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_network));
            return;
        }

        if (!GoUtils.isGpsOpened(this)) {
            GoUtils.showEnableGpsDialog(this);
            return;
        }

        boolean running = Boolean.TRUE.equals(viewModel.getMockRunning().getValue());
        LatLng marked = viewModel.getMarkedLocation().getValue();

        if (running) {
            if (marked == null) {
                stopGoLocation();
                Snackbar.make(v, "模拟位置已终止", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            } else {
                double[] latLng = MapUtils.bd2wgs(marked.longitude, marked.latitude);
                double alt = Double.parseDouble(sharedPreferences.getString("setting_altitude", "55.0"));
                if (mServiceBinder != null) {
                    mServiceBinder.setPosition(latLng[0], latLng[1], alt);
                }
                Snackbar.make(v, "已传送到新位置", Snackbar.LENGTH_LONG).show();

                mBaiduMap.clear();
                viewModel.setMarkedLocation(null);

                if (GoUtils.isWifiEnabled(MainActivity.this)) {
                    GoUtils.showDisableWifiDialog(MainActivity.this);
                }
            }
        } else {
            if (!GoUtils.isAllowMockLocation(this)) {
                GoUtils.showEnableMockLocationDialog(this);
                XLog.e("无模拟位置权限!");
            } else if (marked == null) {
                Snackbar.make(v, "请先点击地图位置或者搜索位置", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            } else {
                startGoLocation();
                Snackbar.make(v, "模拟位置已启动", Snackbar.LENGTH_LONG).show();

                mBaiduMap.clear();
                viewModel.setMarkedLocation(null);

                if (GoUtils.isWifiEnabled(MainActivity.this)) {
                    GoUtils.showDisableWifiDialog(MainActivity.this);
                }
            }
        }
    }

    /*============================== 收藏位置 相关 ==============================*/
    private void initStoreHistory() {
        try {
            DataBaseHistorySearch dbHistory = new DataBaseHistorySearch(getApplicationContext());
            mSearchHistoryDB = dbHistory.getWritableDatabase();
        } catch (Exception e) {
            XLog.e("ERROR: sqlite init error");
        }
    }

    private void saveFavoriteLocation(double bd09Lng, double bd09Lat) {
        final String safeCode = BuildConfig.MAPS_SAFE_CODE;
        final String ak = sharedPreferences.getString("setting_map_key", BuildConfig.MAPS_API_KEY);
        double[] wgs = MapUtils.bd2wgs(bd09Lng, bd09Lat);

        String mapApiUrl = "https://api.map.baidu.com/reverse_geocoding/v3/?ak=" + ak
                + "&output=json&coordtype=bd09ll&location=" + bd09Lat + "," + bd09Lng
                + "&mcode=" + safeCode;

        okhttp3.Request request = new okhttp3.Request.Builder().url(mapApiUrl).get().build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                XLog.e("HTTP: HTTP GET FAILED");
                insertFavorite(viewModel.getMarkedName().getValue(), wgs, bd09Lng, bd09Lat);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    insertFavorite(viewModel.getMarkedName().getValue(), wgs, bd09Lng, bd09Lat);
                    return;
                }
                String resp = responseBody.string();
                try {
                    JSONObject json = new JSONObject(resp);
                    String name;
                    if (Integer.parseInt(json.getString("status")) == 0) {
                        name = json.getJSONObject("result").getString("formatted_address");
                    } else {
                        name = viewModel.getMarkedName().getValue();
                        if (name == null) {
                            name = json.getString("message");
                        }
                    }
                    insertFavorite(name, wgs, bd09Lng, bd09Lat);
                } catch (JSONException e) {
                    XLog.e("JSON: resolve json error");
                    insertFavorite(viewModel.getMarkedName().getValue(), wgs, bd09Lng, bd09Lat);
                }
            }
        });
    }

    private void insertFavorite(String name, double[] wgs, double bd09Lng, double bd09Lat) {
        FavoriteLocation location = new FavoriteLocation();
        location.setName(name == null ? getResources().getString(R.string.history_location_default_name) : name);
        location.setLongitudeWgs84(String.valueOf(wgs[0]));
        location.setLatitudeWgs84(String.valueOf(wgs[1]));
        location.setTimestamp(System.currentTimeMillis() / 1000);
        location.setLongitudeBd09(Double.toString(bd09Lng));
        location.setLatitudeBd09(Double.toString(bd09Lat));
        locationRepository.save(location);
    }

    /*============================== SearchView 相关 ==============================*/
    private void initSearchView() {
        mSearchLayout = findViewById(R.id.search_linear);
        mHistoryLayout = findViewById(R.id.search_history_linear);

        mSearchList = findViewById(R.id.search_list_view);
        mSearchList.setOnItemClickListener((parent, view, position, id) -> {
            String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
            String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
            String name = ((TextView) view.findViewById(R.id.poi_name)).getText().toString();

            LatLng target = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            markLocation(target);
            viewModel.setMarkedName(name);
            MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(target);
            mBaiduMap.setMapStatus(mapstatusupdate);

            double[] wgs = MapUtils.bd2wgs(target.longitude, target.latitude);
            ContentValues contentValues = new ContentValues();
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, name);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION,
                    ((TextView) view.findViewById(R.id.poi_address)).getText().toString());
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_RESULT);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, lng);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, lat);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(wgs[0]));
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_WGS84, String.valueOf(wgs[1]));
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
            DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);

            mSearchLayout.setVisibility(View.INVISIBLE);
            searchItem.collapseActionView();
        });

        mSearchHistoryList = findViewById(R.id.search_history_list_view);
        mSearchHistoryList.setOnItemClickListener((parent, view, position, id) -> {
            String searchIsLoc = ((TextView) view.findViewById(R.id.search_isLoc)).getText().toString();
            String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();

            if (searchIsLoc.equals("1")) {
                String lng = ((TextView) view.findViewById(R.id.search_longitude)).getText().toString();
                String lat = ((TextView) view.findViewById(R.id.search_latitude)).getText().toString();
                LatLng target = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                markLocation(target);
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(target);
                mBaiduMap.setMapStatus(mapstatusupdate);

                mHistoryLayout.setVisibility(View.INVISIBLE);
                searchItem.collapseActionView();
            } else if (searchIsLoc.equals("0")) {
                try {
                    searchView.setQuery(searchKey, true);
                } catch (Exception e) {
                    GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_search));
                    XLog.e(getResources().getString(R.string.app_error_search));
                }
            }
        });

        mSearchHistoryList.setOnItemLongClickListener((parent, view, position, id) -> {
            AlertDialog delDialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("警告")
                    .setMessage("确定要删除该项搜索记录吗?")
                    .setPositiveButton("确定", (dialogInterface, which) -> {
                        String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();
                        try {
                            mSearchHistoryDB.delete(DataBaseHistorySearch.TABLE_NAME,
                                    DataBaseHistorySearch.DB_COLUMN_KEY + " = ?", new String[]{searchKey});
                            List<Map<String, Object>> data = getSearchHistory();
                            if (!data.isEmpty()) {
                                SimpleAdapter simAdapt = new SimpleAdapter(
                                        MainActivity.this, data, R.layout.search_item,
                                        new String[]{DataBaseHistorySearch.DB_COLUMN_KEY,
                                                DataBaseHistorySearch.DB_COLUMN_DESCRIPTION,
                                                DataBaseHistorySearch.DB_COLUMN_TIMESTAMP,
                                                DataBaseHistorySearch.DB_COLUMN_IS_LOCATION,
                                                DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM,
                                                DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM},
                                        new int[]{R.id.search_key, R.id.search_description,
                                                R.id.search_timestamp, R.id.search_isLoc,
                                                R.id.search_longitude, R.id.search_latitude});
                                mSearchHistoryList.setAdapter(simAdapt);
                                mHistoryLayout.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            XLog.e("ERROR: delete database error");
                            GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.history_delete_error));
                        }
                    })
                    .setNegativeButton("取消", (dialogInterface, which) -> {})
                    .create();
            delDialog.show();
            delDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(GoUtils.getThemeAccentColor(MainActivity.this));
            delDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(GoUtils.getThemeOnSurfaceVariantColor(MainActivity.this));
            return true;
        });

        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(suggestionResult -> {
            if (suggestionResult == null || suggestionResult.getAllSuggestions() == null) {
                GoUtils.DisplayToast(this, getResources().getString(R.string.app_search_null));
                return;
            }
            List<Map<String, Object>> data = getMapList(suggestionResult);
            if (data.isEmpty()) {
                GoUtils.DisplayToast(this, getResources().getString(R.string.app_search_null));
                return;
            }
            SimpleAdapter simAdapt = new SimpleAdapter(
                    MainActivity.this, data, R.layout.search_poi_item,
                    new String[]{POI_NAME, POI_ADDRESS, POI_LONGITUDE, POI_LATITUDE},
                    new int[]{R.id.poi_name, R.id.poi_address, R.id.poi_longitude, R.id.poi_latitude});
            mSearchList.setAdapter(simAdapt);
            mSearchLayout.setVisibility(View.VISIBLE);
        });
    }

    /**
     * 发起 POI 联想搜索
     * 若当前城市为空则不限制城市（全国搜索），避免 city 为 null 导致搜索失败
     */
    private void requestSuggestion(String keyword) {
        try {
            SuggestionSearchOption option = new SuggestionSearchOption().keyword(keyword);
            String city = viewModel.getCurrentCity().getValue();
            if (city != null && !city.isEmpty()) {
                option.city(city);
            }
            mSuggestionSearch.requestSuggestion(option);
        } catch (Exception e) {
            GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.app_error_search));
            XLog.e("SuggestionSearch error: " + e.getMessage());
        }
    }

    private void saveSearchKeyword(String query) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, query);
        contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, "搜索关键字");
        contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_KEY);
        contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
        DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
    }

    private List<Map<String, Object>> getSearchHistory() {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            android.database.Cursor cursor = mSearchHistoryDB.query(DataBaseHistorySearch.TABLE_NAME, null,
                    DataBaseHistorySearch.DB_COLUMN_ID + " > ?", new String[]{"0"},
                    null, null, DataBaseHistorySearch.DB_COLUMN_TIMESTAMP + " DESC", null);
            while (cursor.moveToNext()) {
                Map<String, Object> item = new HashMap<>();
                item.put(DataBaseHistorySearch.DB_COLUMN_KEY, cursor.getString(1));
                item.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, cursor.getString(2));
                item.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, "" + cursor.getInt(3));
                item.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, "" + cursor.getInt(4));
                item.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, cursor.getString(7));
                item.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, cursor.getString(8));
                data.add(item);
            }
            cursor.close();
        } catch (Exception e) {
            XLog.e("ERROR: getSearchHistory");
        }
        return data;
    }

    @NonNull
    private static List<Map<String, Object>> getMapList(SuggestionResult suggestionResult) {
        List<Map<String, Object>> data = new ArrayList<>();
        int retCnt = suggestionResult.getAllSuggestions().size();
        for (int i = 0; i < retCnt; i++) {
            if (suggestionResult.getAllSuggestions().get(i).pt == null) {
                continue;
            }
            Map<String, Object> poiItem = new HashMap<>();
            poiItem.put(POI_NAME, suggestionResult.getAllSuggestions().get(i).key);
            poiItem.put(POI_ADDRESS, suggestionResult.getAllSuggestions().get(i).city + " "
                    + suggestionResult.getAllSuggestions().get(i).district);
            poiItem.put(POI_LONGITUDE, "" + suggestionResult.getAllSuggestions().get(i).pt.longitude);
            poiItem.put(POI_LATITUDE, "" + suggestionResult.getAllSuggestions().get(i).pt.latitude);
            data.add(poiItem);
        }
        return data;
    }

    /*============================== 更新 相关 ==============================*/
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void initUpdateVersion() {
        mDownloadManager = (DownloadManager) MainActivity.this.getSystemService(DOWNLOAD_SERVICE);
        mDownloadBdRcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                installNewVersion();
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mDownloadBdRcv, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mDownloadBdRcv, filter);
        }
    }

    private void checkUpdateVersion(boolean result) {
        String mapApiUrl = "https://api.github.com/repos/zwbgap/shaua_mocklocation/releases/latest";
        okhttp3.Request request = new okhttp3.Request.Builder().url(mapApiUrl).get().build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                XLog.i("更新检测失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    return;
                }
                String resp = responseBody.string();
                runOnUiThread(() -> {
                    try {
                        JSONObject getRetJson = new JSONObject(resp);
                        String curVersion = GoUtils.getVersionName(MainActivity.this);
                        if (curVersion != null
                                && (!getRetJson.getString("name").contains(curVersion)
                                || !getRetJson.getString("tag_name").contains(curVersion))) {
                            showUpdateDialog(getRetJson);
                        } else if (result) {
                            GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.update_last));
                        }
                    } catch (JSONException e) {
                        XLog.e("ERROR: resolve json");
                    }
                });
            }
        });
    }

    private void showUpdateDialog(JSONObject getRetJson) throws JSONException {
        final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(MainActivity.this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window == null) {
            return;
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setContentView(R.layout.update);
        window.setGravity(Gravity.CENTER);
        window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

        ((TextView) window.findViewById(R.id.update_title)).setText(getRetJson.getString("name"));
        ((TextView) window.findViewById(R.id.update_time)).setText(getRetJson.getString("created_at"));
        ((TextView) window.findViewById(R.id.update_commit)).setText(getRetJson.getString("target_commitish"));

        TextView updateContent = window.findViewById(R.id.update_content);
        final Markwon markwon = Markwon.create(MainActivity.this);
        markwon.setMarkdown(updateContent, getRetJson.getString("body"));

        window.findViewById(R.id.update_ignore).setOnClickListener(v -> alertDialog.cancel());

        JSONArray jsonArray = new JSONArray(getRetJson.getString("assets"));
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        String download_url = jsonObject.getString("browser_download_url");
        mUpdateFilename = jsonObject.getString("name");

        window.findViewById(R.id.update_agree).setOnClickListener(v -> {
            alertDialog.cancel();
            GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.update_downloading));
            downloadNewVersion(download_url);
        });
    }

    private void downloadNewVersion(String url) {
        if (mDownloadManager == null) {
            return;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedOverRoaming(false);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(GoUtils.getAppName(this));
        request.setDescription("正在下载新版本...");
        request.setMimeType("application/vnd.android.package-archive");

        File file = new File(getExternalFilesDir("Updates"), mUpdateFilename);
        if (file.exists() && !file.delete()) {
            return;
        }
        request.setDestinationUri(Uri.fromFile(file));
        mDownloadId = mDownloadManager.enqueue(request);
    }

    private void installNewVersion() {
        File file = new File(getExternalFilesDir("Updates"), mUpdateFilename);
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        install.addCategory("android.intent.category.DEFAULT");
        install.setDataAndType(ShareUtils.getUriFromFile(MainActivity.this, file), "application/vnd.android.package-archive");
        startActivity(install);
    }
}