package com.washeasy.model;


public class User {

    private int    id;
    private String username;
    private String passwordHash;
    private String role;

    public User(){

    }

    public User(int id, String username, String passwordHash, String role) {
        this.id           = id;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.role         = role;
    }

    public int getId(){
        return id;
    }

    public void setId(int id){

        this.id = id;
    }

    public String getUsername(){

        return username;
    }

    public void setUsername(String u){

        this.username = u;
    }


    public String getPasswordHash(){

        return passwordHash;
    }

    public void setPasswordHash(String p){

        this.passwordHash = p;
    }

    public String getRole(){

        return role;
    }
    public void setRole(String r){

        this.role = r;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }


    public boolean verifyPassword(String input) {
        return passwordHash.equals(input);
    }
}
