package com.chatroom.dto;

import lombok.Data;
import java.util.List;

@Data
public class GroupMembersUpdateRequest {
    private List<Long> userIds;
}