/*
 * JiaoZiVideoPlayer 7.7.2.3300 (cn.jzvd).
 * Copyright (c) 2015-2021 jzvd.org
 * Licensed under the MIT License.
 */
package cn.jzvd;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class JZDataSource {
    public static final String URL_KEY_DEFAULT = "URL_KEY_DEFAULT";
    public int currentUrlIndex;
    public LinkedHashMap urlsMap = new LinkedHashMap();
    public String title = "";
    public HashMap<String, String> headerMap = new HashMap();
    public boolean looping = false;
    public Object[] objects;

    public JZDataSource(String url) {
        this.urlsMap.put(URL_KEY_DEFAULT, url);
        this.currentUrlIndex = 0;
    }

    public JZDataSource(String url, String title) {
        this.urlsMap.put(URL_KEY_DEFAULT, url);
        this.title = title;
        this.currentUrlIndex = 0;
    }

    public JZDataSource(Object url) {
        this.urlsMap.put(URL_KEY_DEFAULT, url);
        this.currentUrlIndex = 0;
    }

    public JZDataSource(LinkedHashMap urlsMap) {
        this.urlsMap.clear();
        this.urlsMap.putAll(urlsMap);
        this.currentUrlIndex = 0;
    }

    public JZDataSource(LinkedHashMap urlsMap, String title) {
        this.urlsMap.clear();
        this.urlsMap.putAll(urlsMap);
        this.title = title;
        this.currentUrlIndex = 0;
    }

    public Object getCurrentUrl() {
        return this.getValueFromLinkedMap(this.currentUrlIndex);
    }

    public Object getCurrentKey() {
        return this.getKeyFromDataSource(this.currentUrlIndex);
    }

    public String getKeyFromDataSource(int index) {
        int currentIndex = 0;
        for (Object key : this.urlsMap.keySet()) {
            if (currentIndex == index) {
                return key.toString();
            }
            ++currentIndex;
        }
        return null;
    }

    public Object getValueFromLinkedMap(int index) {
        int currentIndex = 0;
        for (Object key : this.urlsMap.keySet()) {
            if (currentIndex == index) {
                return this.urlsMap.get(key);
            }
            ++currentIndex;
        }
        return null;
    }

    public boolean containsTheUrl(Object object) {
        if (object != null) {
            return this.urlsMap.containsValue(object);
        }
        return false;
    }

    public JZDataSource cloneMe() {
        LinkedHashMap map = new LinkedHashMap();
        map.putAll(this.urlsMap);
        return new JZDataSource(map, this.title);
    }
}

