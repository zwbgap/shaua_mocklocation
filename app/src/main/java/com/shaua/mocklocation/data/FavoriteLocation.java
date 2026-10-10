package com.shaua.mocklocation.data;

/**
 * 收藏位置数据模型
 */
public class FavoriteLocation {
    private int id;
    private String name;
    /** WGS84 经度 */
    private String longitudeWgs84;
    /** WGS84 纬度 */
    private String latitudeWgs84;
    private long timestamp;
    /** BD09 经度 */
    private String longitudeBd09;
    /** BD09 纬度 */
    private String latitudeBd09;

    public FavoriteLocation() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLongitudeWgs84() {
        return longitudeWgs84;
    }

    public void setLongitudeWgs84(String longitudeWgs84) {
        this.longitudeWgs84 = longitudeWgs84;
    }

    public String getLatitudeWgs84() {
        return latitudeWgs84;
    }

    public void setLatitudeWgs84(String latitudeWgs84) {
        this.latitudeWgs84 = latitudeWgs84;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLongitudeBd09() {
        return longitudeBd09;
    }

    public void setLongitudeBd09(String longitudeBd09) {
        this.longitudeBd09 = longitudeBd09;
    }

    public String getLatitudeBd09() {
        return latitudeBd09;
    }

    public void setLatitudeBd09(String latitudeBd09) {
        this.latitudeBd09 = latitudeBd09;
    }
}