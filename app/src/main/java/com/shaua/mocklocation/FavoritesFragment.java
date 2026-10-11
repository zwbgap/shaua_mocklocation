package com.shaua.mocklocation;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.baidu.mapapi.model.LatLng;
import com.shaua.mocklocation.data.FavoriteLocation;
import com.shaua.mocklocation.data.LocationRepository;
import com.shaua.mocklocation.utils.GoUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 收藏位置列表 Fragment，点击收藏项后返回地图并定位，支持删除
 */
public class FavoritesFragment extends Fragment {

    public interface OnFavoriteSelectedListener {
        void onFavoriteSelected(LatLng latLng, String name);
    }

    private OnFavoriteSelectedListener listener;
    private LocationRepository locationRepository;
    private MainViewModel viewModel;
    private ListView listView;
    private TextView emptyView;
    private FavoritesAdapter adapter;
    private List<FavoriteLocation> locations;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFavoriteSelectedListener) {
            listener = (OnFavoriteSelectedListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        Toolbar toolbar = view.findViewById(R.id.favorites_toolbar);
        toolbar.inflateMenu(R.menu.menu_favorites);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_refresh) {
                loadFavorites();
                return true;
            }
            return false;
        });
        listView = view.findViewById(R.id.favorites_list);
        emptyView = view.findViewById(R.id.favorites_empty);
        locationRepository = new LocationRepository(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        loadFavorites();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 视图销毁时清空 adapter 引用，避免重建后旧 adapter 未绑定到新 ListView
        adapter = null;
        listView = null;
        emptyView = null;
    }

    private void loadFavorites() {
        if (listView == null || locationRepository == null) return;
        locations = locationRepository.findAll();
        if (locations == null || locations.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        listView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        if (adapter == null) {
            adapter = new FavoritesAdapter();
            listView.setOnItemClickListener((parent, v, position, id) -> {
                FavoriteLocation loc = locations.get(position);
                double lat = Double.parseDouble(loc.getLatitudeBd09());
                double lng = Double.parseDouble(loc.getLongitudeBd09());
                LatLng target = new LatLng(lat, lng);
                viewModel.setMarkedLocation(target);
                viewModel.setMarkedName(loc.getName());
                if (listener != null) {
                    listener.onFavoriteSelected(target, loc.getName());
                }
            });
        }
        // 始终重新绑定 adapter，确保 ListView 重建后能正确显示数据
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void deleteFavorite(FavoriteLocation loc) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("警告")
                .setMessage("确定要删除「" + loc.getName() + "」吗?")
                .setPositiveButton("确定", (d, which) -> {
                    locationRepository.deleteById(loc.getId());
                    loadFavorites();
                    GoUtils.DisplayToast(requireContext(), "已删除");
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(GoUtils.getThemeAccentColor(requireContext()));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(GoUtils.getThemeOnSurfaceVariantColor(requireContext()));
    }

    private class FavoritesAdapter extends BaseAdapter {
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        @Override
        public int getCount() {
            return locations.size();
        }

        @Override
        public Object getItem(int position) {
            return locations.get(position);
        }

        @Override
        public long getItemId(int position) {
            return locations.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.history_item, parent, false);
                holder = new ViewHolder();
                holder.tvId = convertView.findViewById(R.id.LocationID);
                holder.tvName = convertView.findViewById(R.id.LocationText);
                holder.tvTime = convertView.findViewById(R.id.TimeText);
                holder.tvWgs = convertView.findViewById(R.id.WGSLatLngText);
                holder.btnDelete = convertView.findViewById(R.id.btn_delete);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FavoriteLocation loc = locations.get(position);
            holder.tvName.setText(loc.getName());
            holder.tvTime.setText(sdf.format(new Date(loc.getTimestamp() * 1000)));
            holder.tvWgs.setText(getString(R.string.favorites_wgs_format, loc.getLongitudeWgs84(), loc.getLatitudeWgs84()));

            holder.btnDelete.setOnClickListener(v -> deleteFavorite(loc));
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView tvId;
        TextView tvName;
        TextView tvTime;
        TextView tvWgs;
        ImageView btnDelete;
    }
}
