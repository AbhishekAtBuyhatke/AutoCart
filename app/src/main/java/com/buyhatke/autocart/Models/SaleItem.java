package com.buyhatke.autocart.Models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by Abhishek on 26-Aug-17.
 */

public class SaleItem {

    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("code")
    @Expose
    private String code;
    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("time")
    @Expose
    private Integer time;
    @SerializedName("imp")
    @Expose
    private String imp;
    @SerializedName("all_variants")
    @Expose
    private String all_variants;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public String getImp() {
        return imp;
    }

    public void setImp(String imp) {
        this.imp = imp;
    }

    public String getAll_variants() {
        return all_variants;
    }
}


