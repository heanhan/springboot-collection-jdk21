package com.example.elk.controller;

import com.example.common.result.ResultBody;
import com.example.elk.entity.DiscussPost;
import com.example.elk.model.vo.RemoveDiscussPostVo;
import com.example.elk.service.DiscussPostService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *
 */

@Slf4j
@RestController
@RequestMapping(value = "/discussPost")
public class DiscussPostController {

    @Resource
    private DiscussPostService discussPostServiceImpl;

    /**
     * 新增帖子
     * @param discussPost
     * @return
     */
    @PostMapping("/addDiscussPost")
    private ResultBody<List<DiscussPost>> addDiscusPost(@RequestBody DiscussPost discussPost){
        Boolean flag = discussPostServiceImpl.addDiscussPost(discussPost);
        if(flag){
            return ResultBody.success();
        }
        return ResultBody.error("添加失败!");
    }


    /**
     * 获取帖子list 不带有参数
     * @return
     */
    @PostMapping("/findAllDiscussPost")
    private ResultBody<List<DiscussPost>> findAllDiscussPost(){
        List<DiscussPost> list=  discussPostServiceImpl.findAllDiscussPost();
        return ResultBody.success(list);
    }

    @PostMapping(value = "/removeDiscussPostById")
    public ResultBody<Object> removeDiscussPostById(@RequestBody RemoveDiscussPostVo removeDiscussPostVo){
        Boolean flag= discussPostServiceImpl.removeDiscussPostById(removeDiscussPostVo.getId());
        if(flag){
            return ResultBody.success("帖子删除成功");
        }
        return ResultBody.error("帖子删除失败");


    }


}
