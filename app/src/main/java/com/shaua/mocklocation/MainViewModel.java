package com.shaua.mocklocation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.baidu.mapapi.model.LatLng;

/**
 * 主界面 ViewModel，持有地图选点、模拟定位状态、搜索城市等可观察数据
 */
public class MainViewModel extends ViewModel {

    /** 当前标记的地图点 (BD09) */
    private final MutableLiveData<LatLng> markedLocation = new MutableLiveData<>();

    /** 当前标记点的名称 */
    private final MutableLiveData<String> markedName = new MutableLiveData<>();

    /** 模拟定位是否运行中 */
    private final MutableLiveData<Boolean> mockRunning = new MutableLiveData<>(false);

    /** 当前城市（用于 POI 搜索） */
    private final MutableLiveData<String> currentCity = new MutableLiveData<>();

    public LiveData<LatLng> getMarkedLocation() {
        return markedLocation;
    }

    public void setMarkedLocation(LatLng latLng) {
        markedLocation.setValue(latLng);
    }

    public LiveData<String> getMarkedName() {
        return markedName;
    }

    public void setMarkedName(String name) {
        markedName.setValue(name);
    }

    public LiveData<Boolean> getMockRunning() {
        return mockRunning;
    }

    public void setMockRunning(boolean running) {
        mockRunning.setValue(running);
    }

    public LiveData<String> getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(String city) {
        currentCity.setValue(city);
    }
}