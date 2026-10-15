package com.picture_backend.service.impl;

import com.picture_backend.exception.BusinessException;
import com.picture_backend.mapper.AiConversationMapper;
import com.picture_backend.mapper.AiMessageMapper;
import com.picture_backend.model.entity.AiConversation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {
    @InjectMocks private AiChatServiceImpl service;
    @Mock private AiConversationMapper conversationMapper;
    @Mock private AiMessageMapper messageMapper;

    @Test
    void deletedConversationCannotReadMessages() {
        AiConversation conversation = new AiConversation();
        conversation.setId(7L);
        conversation.setUserId(1L);
        conversation.setIsDelete(1);
        when(conversationMapper.selectById(7L)).thenReturn(conversation);

        assertThrows(BusinessException.class, () -> service.listMessages(1L, 7L, 1, 50));
        verifyNoInteractions(messageMapper);
    }

    @Test
    void deleteConversationUsesLogicalDelete() {
        AiConversation conversation = new AiConversation();
        conversation.setId(7L);
        conversation.setUserId(1L);
        when(conversationMapper.selectById(7L)).thenReturn(conversation);
        when(conversationMapper.deleteById(7L)).thenReturn(1);

        assertTrue(service.deleteConversation(1L, 7L));
        verify(conversationMapper).deleteById(7L);
    }
}
