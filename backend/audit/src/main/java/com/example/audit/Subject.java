package com.example.audit;

import lombok.Builder;
import lombok.Value;

/** 감사 대상(정보주체/자산) 식별자. */
@Value
@Builder(toBuilder = true)
public class Subject {
    String type;
    String key;
}
