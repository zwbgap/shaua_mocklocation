package com.shaua.mocklocation.data;

import android.content.Context;

import java.util.List;

/**
 * 收藏位置仓库，封装数据库访问，提供单一数据源
 */
public class LocationRepository {

    private final FavoriteLocationDao dao;

    public LocationRepository(Context context) {
        this.dao = new FavoriteLocationDao(context.getApplicationContext());
    }

    public void save(FavoriteLocation location) {
        dao.save(location);
    }

    public List<FavoriteLocation> findAll() {
        return dao.findAll();
    }

    public void deleteById(int id) {
        dao.deleteById(id);
    }

    public void deleteAll() {
        dao.deleteAll();
    }

    public void updateName(int id, String name) {
        dao.updateName(id, name);
    }

    public void archiveExpired(long expirationDays) {
        dao.archiveExpired(expirationDays);
    }
}