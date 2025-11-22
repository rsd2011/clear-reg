package com.example.audit;

import lombok.Builder;
import lombok.Value;

/** 감사 이벤트의 주체(행위자) 정보를 표현한다. */
@Value
@Builder(toBuilder = true)
public class Actor {
    String id;
    ActorType type;
    String role;
    String dept;
}
