package com.example.oauth.controller;

import com.example.oauth.common.result.ResultBody;
import com.example.oauth.entity.OAuthClient;
import com.example.oauth.service.OAuthClientService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth 客户端管理控制器（JDBC 存储的多客户端管理）
 */
@RestController
@RequestMapping("/admin/clients")
@Api(tags = "OAuth 客户端管理接口")
public class OAuthClientAdminController {

    private final OAuthClientService clientService;

    public OAuthClientAdminController(OAuthClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @ApiOperation("分页查询客户端")
    public ResultBody<Page<OAuthClient>> page(@RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Pageable pageable = PageRequest.of(Math.max(0, pageNum - 1), pageSize,
                Sort.by(Sort.Direction.DESC, "id"));
        return ResultBody.success(clientService.page(keyword, pageable));
    }

    @GetMapping("/{id}")
    @ApiOperation("查询客户端详情")
    public ResultBody<OAuthClient> getById(@PathVariable Long id) {
        return ResultBody.success(clientService.getById(id));
    }

    @PostMapping
    @ApiOperation("新增客户端")
    public ResultBody<OAuthClient> create(@RequestBody OAuthClient client) {
        return ResultBody.success(clientService.create(client));
    }

    @PutMapping
    @ApiOperation("更新客户端")
    public ResultBody<OAuthClient> update(@RequestBody OAuthClient client) {
        return ResultBody.success(clientService.update(client));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除客户端")
    public ResultBody<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResultBody.success();
    }
}
