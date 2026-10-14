package com.example.musicplayer;


import java.io.Serializable;

public class TaskModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private String title;//任务名称

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}