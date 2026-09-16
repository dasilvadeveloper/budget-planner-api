package com.lelisdev.budget_planner_api.utils;

public enum Role {
    SYS_ADMIN("SYS ADMIN"),
    AGENCY_ADMIN("AGENCY ADMIN"),
    AGENT("AGENT");

    private String name;

    Role(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }
}
