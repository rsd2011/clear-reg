package com.example.auth.ad;

public interface ActiveDirectoryClient {

  boolean authenticate(String username, String password);
}
