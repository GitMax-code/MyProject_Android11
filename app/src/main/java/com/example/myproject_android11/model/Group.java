package com.example.myproject_android11.model;

import java.util.ArrayList;
import java.util.List;

public class Group {
    String id;
    String name;
    String creator;
    String date;
    String time;


    public Group() {
        // Constructeur par défaut requis pour Firestore
    }

    public Group(String id, String name, String creator, String date, String time) {
        this.id = id;
        this.name = name;
        this.creator = creator;
        this.date = date;
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

    public void setDate(String date){
        this.date = date;
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

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }


}
