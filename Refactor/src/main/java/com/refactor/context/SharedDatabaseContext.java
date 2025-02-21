package com.refactor.context;

import lombok.Data;

import java.util.*;

@Data
public class SharedDatabaseContext {


    public Map<String, ArrayList<String>> sharedDatabaseMap;
    public Map<String, ArrayList<String>> serviceIntimacyMap;
    public Map<String, ArrayList<String>> servicecDatabasesMap;


    public SharedDatabaseContext() {
        this.sharedDatabaseMap = new HashMap<>();
        this.serviceIntimacyMap = new HashMap<>();
        this.servicecDatabasesMap = new HashMap<>();
    }

    public void addSharedDatabase(String databaseName, ArrayList<String> list){
        this.sharedDatabaseMap.put(databaseName,list);
    }

    public void addServiceIntimacy(String databaseName,ArrayList<String> serviceIntimacySet){
        this.serviceIntimacyMap.put(databaseName,serviceIntimacySet);
    }

    public void addServicecDatabasesMap(Map<String, ArrayList<String>> servicecDatabasesMap){
        this.servicecDatabasesMap = servicecDatabasesMap;
    }


}