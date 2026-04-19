package com.washeasy.controller;

import com.washeasy.database.DatabaseManager;
import com.washeasy.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;


public class AdminPanel {

    private final DatabaseManager db = DatabaseManager.getInstance();
    private boolean isLoggedIn     = false;
    private User    currentUser    = null;
    private String  sessionToken   = null;

    public User validateLogin(String username, String password) {
        try {
            ResultSet rs = db.preparedQuery(
                    "SELECT * FROM users WHERE username=? LIMIT 1", username
            );
            if (rs.next()) {
                User u = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role")
                );
                if (u.verifyPassword(password)) {
                    isLoggedIn   = true;
                    currentUser  = u;
                    sessionToken = username + "_" + System.currentTimeMillis();
                    System.out.println("[AdminPanel] Login berhasil: " + username + " (" + u.getRole() + ")");
                    return u;
                }
            }
        } catch (SQLException e) {
            System.err.println("[AdminPanel] validateLogin error: " + e.getMessage());
        }
        return null;
    }

    public void logout() {
        isLoggedIn   = false;
        currentUser  = null;
        sessionToken = null;
        System.out.println("[AdminPanel] Session ditutup.");
    }

    public boolean isAuthenticated(){
        return isLoggedIn;
    }

    public User getCurrentUser()  {
        return currentUser;
    }

    public String getSessionToken(){
        return sessionToken;
    }
}
