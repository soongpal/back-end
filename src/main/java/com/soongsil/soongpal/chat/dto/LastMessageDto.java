package com.soongsil.soongpal.chat.dto;

import java.time.LocalDateTime;

public record LastMessageDto(
        Long roomId,
        String content,
        LocalDateTime createdAt
) {}
