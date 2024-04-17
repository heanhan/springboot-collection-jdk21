package com.example.elk.service.impl;

import com.example.common.utils.Snowflake;
import com.example.elk.entity.DiscussPost;
import com.example.elk.repository.DiscussPostRepository;
import com.example.elk.service.DiscussPostService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class DiscussPostServiceImpl implements DiscussPostService {

    @Resource
    private DiscussPostRepository discussPostRepository;


    /**
     * 添加帖子
     *
     * @param discussPost
     * @return
     */
    @Override
    public Boolean addDiscussPost(DiscussPost discussPost) {
        Snowflake snowflake = new Snowflake();
        discussPost.setId(String.valueOf(snowflake.getWorkerId()));
        DiscussPost save = discussPostRepository.save(discussPost);
        if(ObjectUtils.isEmpty(save)){
            return false;
        }
        return true;
    }

    /**
     * 查询 所有的帖子
     *
     * @return
     */
    @Override
    public List<DiscussPost> findAllDiscussPost() {
        Iterable<DiscussPost> all = discussPostRepository.findAll();
        List<DiscussPost> discussPosts = new ArrayList<>();
        all.forEach(discussPosts::add);
        return discussPosts;
    }

    /**
     * 删除帖子 根据id
     *
     * @param id
     * @return
     */
    @Override
    public Boolean removeDiscussPostById(String id) {
        discussPostRepository.deleteById(id);
        return true;
    }
}
