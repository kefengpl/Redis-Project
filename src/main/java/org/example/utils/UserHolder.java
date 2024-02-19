package org.example.utils;

import org.example.dto.UserDTO;

public class UserHolder {

    public final static String USER_KEY = "user";

    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
