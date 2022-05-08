package com.mobil.torrent.model;

public class TorrentItem {
    private String id;
    private String name;
    private String info;
    private String size;
    private float ratedInfo;
    private int imageResource;
    private int downloadCount;

    public TorrentItem(String name, String info, String size, float ratedInfo, int imageResource, int downloadCount) {
        this.name = name;
        this.info = info;
        this.size = size;
        this.ratedInfo = ratedInfo;
        this.imageResource = imageResource;
        this.downloadCount = downloadCount;
    }

    public TorrentItem() {
    }

    public String getName() {
        return name;
    }
    public String getInfo() {
        return info;
    }
    public String getSize() {
        return size;
    }
    public float getRatedInfo() {
        return ratedInfo;
    }
    public int getImageResource() {
        return imageResource;
    }
    public int getDownloadCount() {
        return downloadCount;
    }

    public String _getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}
