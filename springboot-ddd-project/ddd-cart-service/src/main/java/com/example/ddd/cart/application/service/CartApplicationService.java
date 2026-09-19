package com.example.ddd.cart.application.service;

import com.example.ddd.cart.application.port.CatalogGateway;
import com.example.ddd.cart.domain.repository.CartRepository;
import com.example.ddd.common.exception.*;
import com.example.ddd.contract.cart.CartItemDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/** 应用层：加购/改量/选中/移除和实时结算预览，不涉及支付及库存预占。 */
@Service
public class CartApplicationService {
    private final CartRepository repository; private final CatalogGateway catalog;
    public CartApplicationService(CartRepository repository,CatalogGateway catalog) { this.repository=repository; this.catalog=catalog; }
    /** 加购事务：校验可售商品后锁定用户购物车并合并数量。 */
    @Transactional
    public void add(String user,String sku,int quantity) {
        if(!"ON_SALE".equals(catalog.get(sku).status())) throw new BusinessException(ErrorCode.PRODUCT_OFF_SHELF);
        var cart=repository.load(user,true); cart.add(sku,quantity); repository.save(cart);
    }
    /** 修改事务：同时更新数量与选中状态，不产生领域事件。 */
    @Transactional
    public void change(String user,String sku,int quantity,boolean checked) {
        var cart=repository.load(user,true); cart.change(sku,quantity,checked); repository.save(cart);
    }
    /** 移除事务：重复删除幂等。 */
    @Transactional
    public void remove(String user,String sku) { var cart=repository.load(user,true); cart.remove(sku); repository.save(cart); }
    /** 清空事务：仅操作当前用户购物车。 */
    @Transactional
    public void clear(String user) { var cart=repository.load(user,true); cart.clear(); repository.save(cart); }
    /** 查询用例：刷新商品展示信息和价格，返回契约 DTO。 */
    @Transactional(readOnly=true)
    public List<CartItemDTO> list(String user) {
        return repository.load(user,false).items().stream().map(i -> {
            var s=catalog.get(i.skuId());
            return new CartItemDTO(user+":"+i.skuId(),user,i.skuId(),s.spuName(),s.skuName(),s.image(),s.price(),i.quantity(),i.checked());
        }).toList();
    }
    /** 预览用例：只计算选中且可售条目；下单仍由订单服务重新定价。 */
    @Transactional(readOnly=true)
    public Preview preview(String user) {
        var selected=repository.load(user,false).items().stream().filter(i -> i.checked()).map(i -> {
            var sku=catalog.get(i.skuId());
            if(!"ON_SALE".equals(sku.status())) throw new BusinessException(ErrorCode.PRODUCT_OFF_SHELF);
            return new CartItemDTO(user+":"+i.skuId(),user,i.skuId(),sku.spuName(),sku.skuName(),sku.image(),sku.price(),i.quantity(),true);
        }).toList();
        if(selected.isEmpty()) throw new BusinessException(ErrorCode.CART_EMPTY);
        var total=selected.stream().map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO,BigDecimal::add);
        var shipping=total.compareTo(new BigDecimal("99"))>=0?new BigDecimal("0.00"):new BigDecimal("10.00");
        return new Preview(selected,total,shipping,total.add(shipping));
    }
    /** 结算预览值对象：不是成交承诺。 */
    public record Preview(List<CartItemDTO> items,BigDecimal totalAmount,BigDecimal shippingFee,BigDecimal payAmount) {}
}
