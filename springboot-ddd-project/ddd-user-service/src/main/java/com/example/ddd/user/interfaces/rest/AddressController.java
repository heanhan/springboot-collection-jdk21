package com.example.ddd.user.interfaces.rest;

import com.example.ddd.common.result.Result;
import com.example.ddd.user.application.command.SaveAddressCommand;
import com.example.ddd.user.application.service.AddressApplicationService;
import com.example.ddd.user.domain.model.entity.AddressEntry;
import com.example.ddd.user.interfaces.dto.SaveAddressRequest;
import com.example.ddd.user.interfaces.dto.UserDetailResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST 控制器：收货地址簿管理。
 */
@RestController
@RequestMapping("/users/{userId}/addresses")
public class AddressController {

    private final AddressApplicationService addressApplicationService;

    public AddressController(AddressApplicationService addressApplicationService) {
        this.addressApplicationService = addressApplicationService;
    }

    @GetMapping
    public Result<List<UserDetailResponse.AddressItem>> list(@PathVariable String userId) {
        List<AddressEntry> entries = addressApplicationService.listAddresses(userId);
        return Result.ok(entries.stream().map(UserDetailResponse.AddressItem::from).toList());
    }

    @PostMapping
    public Result<String> add(@PathVariable String userId, @Valid @RequestBody SaveAddressRequest req) {
        String addressId = addressApplicationService.saveAddress(toCommand(userId, null, req));
        return Result.ok(addressId);
    }

    @PutMapping("/{addressId}")
    public Result<Void> update(@PathVariable String userId,
                               @PathVariable String addressId,
                               @Valid @RequestBody SaveAddressRequest req) {
        addressApplicationService.saveAddress(toCommand(userId, addressId, req));
        return Result.ok();
    }

    @DeleteMapping("/{addressId}")
    public Result<Void> remove(@PathVariable String userId, @PathVariable String addressId) {
        addressApplicationService.removeAddress(userId, addressId);
        return Result.ok();
    }

    @PostMapping("/{addressId}/default")
    public Result<Void> markDefault(@PathVariable String userId, @PathVariable String addressId) {
        addressApplicationService.markDefault(userId, addressId);
        return Result.ok();
    }

    private SaveAddressCommand toCommand(String userId, String addressId, SaveAddressRequest req) {
        return new SaveAddressCommand(userId, addressId, req.receiver(), req.mobile(),
                req.province(), req.city(), req.district(), req.detail(), req.zipCode(),
                req.tag(), req.setAsDefault());
    }
}
