package com.soongsil.soongpal.chat.dto;

import com.soongsil.soongpal.chat.domain.ChatRoomUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomUserResDto {

    private Long userId;
    private String userName;
    private String profileImage;

    public static ChatRoomUserResDto from(ChatRoomUser roomUser) {
        return ChatRoomUserResDto.builder()
                .userId(roomUser.getUser().getId())
                .userName(roomUser.getUser().getNickName())
                .build();
    }
}
