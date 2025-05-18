package com.example.myproject_android11.model;

import java.util.ArrayList;
import java.util.List;

public class Group {
    String id;
    String name;
    String creator;
    String dayOfWeek;
    String time;


    public Group() {
        // Constructeur par défaut requis pour Firestore
    }

    public Group(String id, String name, String creator, String dayOfWeek, String time) {
        this.id = id;
        this.name = name;
        this.creator = creator;
        this.dayOfWeek = dayOfWeek;
        this.time = time;

    }

    //set
    public void setId(String id){
        this.id = id;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setCreator(String creator){
        this.creator = creator;
    }

    public void setDayOfWeek(String dayOfWeek){
        this.dayOfWeek = dayOfWeek;
    }

    public void setTime(String time){
        this.time = time;
    }


    //Getter

    public String getId(){
        return id;
    }

    public String getName() {
        return name;
    }
    public String getCreator() {
        return creator;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public String getTime() {
        return time;
    }

    @Override
    public String toString() {
        return name;
    }


}
