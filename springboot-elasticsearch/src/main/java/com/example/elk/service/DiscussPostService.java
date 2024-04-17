package com.example.elk.service;

import com.example.elk.entity.DiscussPost;

import java.util.List;

public interface DiscussPostService {

    /**
     * 添加帖子
     * @param discussPost
     * @return
     */
    Boolean addDiscussPost(DiscussPost discussPost);

    /**
     * 查询 所有的帖子
     * @return
     */
    List<DiscussPost> findAllDiscussPost();

    /**
     * 删除帖子 根据id
     * @param id
     * @return
     */
    Boolean removeDiscussPostById(String id);
}
