package com.example.auth.dto;

import com.example.auth.LoginType;

public record LoginResponse(String username, LoginType type, TokenResponse tokens) {}
