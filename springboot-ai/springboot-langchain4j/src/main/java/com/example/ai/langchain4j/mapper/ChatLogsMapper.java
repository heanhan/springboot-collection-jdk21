package com.example.ai.langchain4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ai.langchain4j.dto.ChatLogs;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: zhaojh
 * @Date: 2025-03-04 22:08
 * @Description:
 */

@Mapper
public interface ChatLogsMapper extends BaseMapper<ChatLogs> {

}
