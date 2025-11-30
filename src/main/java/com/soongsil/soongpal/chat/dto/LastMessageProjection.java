package com.soongsil.soongpal.chat.dto;

import java.time.LocalDateTime;

public interface LastMessageProjection {

    Long getRoomId();

    String getContent();

    LocalDateTime getCreatedAt();
}
