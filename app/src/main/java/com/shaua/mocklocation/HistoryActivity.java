package com.shaua.mocklocation;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.view.Gravity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.shaua.mocklocation.data.FavoriteLocation;
import com.shaua.mocklocation.data.LocationRepository;
import com.shaua.mocklocation.utils.GoUtils;

public class HistoryActivity extends BaseActivity {
    public static final String KEY_ID = "KEY_ID";
    public static final String KEY_LOCATION = "KEY_LOCATION";
    public static final String KEY_TIME = "KEY_TIME";
    public static final String KEY_LNG_LAT_WGS = "KEY_LNG_LAT_WGS";
    public static final String KEY_LNG_LAT_CUSTOM = "KEY_LNG_LAT_CUSTOM";

    private ListView mRecordListView;
    private TextView noRecordText;
    private LinearLayout mSearchLayout;
    private LocationRepository locationRepository;
    private List<Map<String, Object>> mAllRecord;
    private android.content.SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(GoUtils.getThemePrimaryDarkColor(this));
        setContentView(R.layout.activity_history);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        locationRepository = new LocationRepository(this);

        initRecordListView();
        initSearchView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        } else if (id == R.id.action_delete) {
            AlertDialog dialog = new AlertDialog.Builder(HistoryActivity.this)
                    .setTitle("警告")
                    .setMessage("确定要删除全部收藏吗?")
                    .setPositiveButton("确定", (dialogInterface, which) -> {
                        locationRepository.deleteAll();
                        GoUtils.DisplayToast(this, getResources().getString(R.string.history_delete_ok));
                        updateRecordList();
                    })
                    .setNegativeButton("取消", (dialogInterface, which) -> {})
                    .create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(GoUtils.getThemeAccentColor(this));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF616161);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private List<Map<String, Object>> fetchAllRecord() {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            // 先清理过期记录（固定有效期 7 天）
            long limits = 7;
            locationRepository.archiveExpired(limits);

            List<FavoriteLocation> list = locationRepository.findAll();
            for (FavoriteLocation loc : list) {
                Map<String, Object> item = new HashMap<>();
                BigDecimal bdLng = BigDecimal.valueOf(Double.parseDouble(loc.getLongitudeWgs84()));
                BigDecimal bdLat = BigDecimal.valueOf(Double.parseDouble(loc.getLatitudeWgs84()));
                BigDecimal bdLngBd = BigDecimal.valueOf(Double.parseDouble(loc.getLongitudeBd09()));
                BigDecimal bdLatBd = BigDecimal.valueOf(Double.parseDouble(loc.getLatitudeBd09()));

                item.put(KEY_ID, Integer.toString(loc.getId()));
                item.put(KEY_LOCATION, loc.getName());
                item.put(KEY_TIME, GoUtils.timeStamp2Date(Long.toString(loc.getTimestamp())));
                item.put(KEY_LNG_LAT_WGS, "[经度:" + bdLng.setScale(11, RoundingMode.HALF_UP).doubleValue()
                        + " 纬度:" + bdLat.setScale(11, RoundingMode.HALF_UP).doubleValue() + "]");
                item.put(KEY_LNG_LAT_CUSTOM, "[经度:" + bdLngBd.setScale(11, RoundingMode.HALF_UP).doubleValue()
                        + " 纬度:" + bdLatBd.setScale(11, RoundingMode.HALF_UP).doubleValue() + "]");
                data.add(item);
            }
        } catch (Exception e) {
            data.clear();
            Log.e("HistoryActivity", "ERROR - fetchAllRecord");
        }
        return data;
    }

    private void initSearchView() {
        SearchView mSearchView = findViewById(R.id.searchView);
        mSearchView.onActionViewExpanded();
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setFocusable(false);
        mSearchView.clearFocus();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    setListAdapter(mAllRecord);
                } else {
                    List<Map<String, Object>> searchRet = new ArrayList<>();
                    for (int i = 0; i < mAllRecord.size(); i++) {
                        if (mAllRecord.get(i).toString().indexOf(newText) > 0) {
                            searchRet.add(mAllRecord.get(i));
                        }
                    }
                    if (!searchRet.isEmpty()) {
                        setListAdapter(searchRet);
                    } else {
                        GoUtils.DisplayToast(HistoryActivity.this, getResources().getString(R.string.history_error_search));
                        setListAdapter(mAllRecord);
                    }
                }
                return false;
            }
        });
    }

    private void setListAdapter(List<Map<String, Object>> list) {
        SimpleAdapter simAdapt = new SimpleAdapter(
                this, list, R.layout.history_item,
                new String[]{KEY_ID, KEY_LOCATION, KEY_TIME, KEY_LNG_LAT_WGS, KEY_LNG_LAT_CUSTOM},
                new int[]{R.id.LocationID, R.id.LocationText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                // 图标圆形背景使用主题强调色的半透明版本，随主题变化
                ImageView icon = view.findViewById(R.id.history_icon);
                if (icon != null) {
                    int accent = GoUtils.getThemeAccentColor(HistoryActivity.this);
                    int tintColor = (accent & 0x00FFFFFF) | (0x14 << 24);
                    icon.getBackground().setTint(tintColor);
                }
                return view;
            }
        };
        mRecordListView.setAdapter(simAdapt);
    }

    private void showDeleteDialog(String locID) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("警告")
                .setMessage("确定要删除该项收藏吗?")
                .setPositiveButton("确定", (dialogInterface, whichButton) -> {
                    locationRepository.deleteById(Integer.parseInt(locID));
                    GoUtils.DisplayToast(HistoryActivity.this, getResources().getString(R.string.history_delete_ok));
                    updateRecordList();
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(GoUtils.getThemeAccentColor(this));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF616161);
    }

    private void showInputDialog(String locID, String name) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(name);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("名称")
                .setView(input)
                .setPositiveButton("确认", (dialogInterface, whichButton) -> {
                    locationRepository.updateName(Integer.parseInt(locID), input.getText().toString());
                    updateRecordList();
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(GoUtils.getThemeAccentColor(this));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF616161);
    }

    private String[] randomOffset(String longitude, String latitude) {
        String max_offset_default = getResources().getString(R.string.setting_random_offset_default);
        double lon_max_offset = Double.parseDouble(sharedPreferences.getString("setting_lon_max_offset", max_offset_default));
        double lat_max_offset = Double.parseDouble(sharedPreferences.getString("setting_lat_max_offset", max_offset_default));
        double lon = Double.parseDouble(longitude);
        double lat = Double.parseDouble(latitude);

        double randomLonOffset = (Math.random() * 2 - 1) * lon_max_offset;
        double randomLatOffset = (Math.random() * 2 - 1) * lat_max_offset;

        lon += randomLonOffset / 111320;
        lat += randomLatOffset / 110574;

        String offsetMessage = String.format(Locale.US, "经度偏移: %.2f米\n纬度偏移: %.2f米", randomLonOffset, randomLatOffset);
        GoUtils.DisplayToast(this, offsetMessage);

        return new String[]{String.valueOf(lon), String.valueOf(lat)};
    }

    private void initRecordListView() {
        noRecordText = findViewById(R.id.record_no_textview);
        mSearchLayout = findViewById(R.id.search_linear);
        mRecordListView = findViewById(R.id.record_list_view);
        mRecordListView.setOnItemClickListener((adapterView, view, i, l) -> {
            String bd09LatLng = (String) ((TextView) view.findViewById(R.id.BDLatLngText)).getText();
            bd09LatLng = bd09LatLng.substring(bd09LatLng.indexOf('[') + 1, bd09LatLng.indexOf(']'));
            String[] latLngStr = bd09LatLng.split(" ");
            String bd09Longitude = latLngStr[0].substring(latLngStr[0].indexOf(':') + 1);
            String bd09Latitude = latLngStr[1].substring(latLngStr[1].indexOf(':') + 1);

            if (sharedPreferences.getBoolean("setting_random_offset", false)) {
                String[] offsetResult = randomOffset(bd09Longitude, bd09Latitude);
                bd09Longitude = offsetResult[0];
                bd09Latitude = offsetResult[1];
            }

            String name = (String) ((TextView) view.findViewById(R.id.LocationText)).getText();
            Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.FAV_LNG, bd09Longitude);
            intent.putExtra(MainActivity.FAV_LAT, bd09Latitude);
            intent.putExtra(MainActivity.FAV_NAME, name);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            this.finish();
        });

        mRecordListView.setOnItemLongClickListener((parent, view, position, id) -> {
            PopupMenu popupMenu = new PopupMenu(HistoryActivity.this, view);
            popupMenu.setGravity(Gravity.END | Gravity.BOTTOM);
            popupMenu.getMenu().add("编辑");
            popupMenu.getMenu().add("删除");

            popupMenu.setOnMenuItemClickListener(item -> {
                String locID = ((TextView) view.findViewById(R.id.LocationID)).getText().toString();
                String name = ((TextView) view.findViewById(R.id.LocationText)).getText().toString();
                switch (item.getTitle().toString()) {
                    case "编辑":
                        showInputDialog(locID, name);
                        return true;
                    case "删除":
                        showDeleteDialog(locID);
                        return true;
                    default:
                        return false;
                }
            });
            popupMenu.show();
            return true;
        });

        updateRecordList();
    }

    private void updateRecordList() {
        mAllRecord = fetchAllRecord();

        if (mAllRecord.isEmpty()) {
            mRecordListView.setVisibility(View.GONE);
            mSearchLayout.setVisibility(View.GONE);
            noRecordText.setVisibility(View.VISIBLE);
        } else {
            noRecordText.setVisibility(View.GONE);
            mRecordListView.setVisibility(View.VISIBLE);
            mSearchLayout.setVisibility(View.VISIBLE);
            setListAdapter(mAllRecord);
        }
    }
}