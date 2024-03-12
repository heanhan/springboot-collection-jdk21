package com.example.parameter.controller;


import com.example.parameter.model.vo.UserVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.result.ResultBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequestMapping(value = "/user")
@RestController
public class UserController {

    @Resource
    private HttpServletRequest request;

    /**
     * get形式 进行 参数查询
     * @param id
     * @param userId
     * @return
     */
    @GetMapping(value = "/findUserDetail")
    public ResultBody findUserDetail(@RequestParam("id") Long id,@RequestParam(name = "userId",required = false) String userId){
        log.info("获取的参数-> Id:{},用户编号:{}",id,userId);
        return ResultBody.success();
    }

    /**
     * get形式 进行 参数查询
     * get请求以实体类型接收参数时，不能用RequestParam 注解机型标注，因为不支持这样的方式
     * 获取参数。
     * @return
     */
    @GetMapping(value = "/findUserDetailByVo")
    public ResultBody findUserDetailByVo(UserVo userVo){
        log.info("获取的参数:{}",userVo);
        return ResultBody.success(userVo);
    }

    /**
     * 通过 HttpServletRequest 接收参数
     * @return
     */

    @GetMapping(value = "/findUserDetailByRequest")
    public ResultBody findUserDetailByRequest(HttpServletRequest req){
        Long id = Long.valueOf(req.getParameter("id").trim());
        String userName = req.getParameter("userName");
        log.info("获取的参数id:{}",id);
        log.info("获取的参数userName:{}",userName);
        return ResultBody.success();

    }

    /**
     * 通过注解HttpServletRequest获取参数
     * @return
     */
    @GetMapping(value = "/findUserDetailByRequest1")
    public ResultBody findUserDetailByRequest1(){
        Long id = Long.valueOf(request.getParameter("id").trim());
        String userName = request.getParameter("userName");
        log.info("获取的参数id:{}",id);
        log.info("获取的参数userName:{}",userName);
        return ResultBody.success();

    }

    /**
     * 通过 @PathVariable 注解接收参数
     * @return
     */
    @GetMapping(value = "/findUserDetailByPathVariable/{id}/{userName}")
    public ResultBody findUserDetailByPathVariable(@PathVariable(name="id") Long id,@PathVariable(name = "userName") String userName){
        log.info("获取的参数-> Id:{},用户姓名:{}",id,userName);
        return ResultBody.success();
    }

    @GetMapping(value = "/findUseByArray")
    public ResultBody findUseByArray(@RequestParam(name = "array") String[] array){
        List<String> list = Arrays.stream(array).toList();
        list.stream().forEach(item->{
            System.out.println(item);
        });
        return ResultBody.success();
    }

    /**
     * 动态条件查询
     * @return
     */
    @PostMapping(value = "/findUserByCondition")
    public ResultBody findUserByCondition(){
        return ResultBody.success();
    }
}
