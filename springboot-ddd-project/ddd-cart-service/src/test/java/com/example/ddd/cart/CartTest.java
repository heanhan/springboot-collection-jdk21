package com.example.ddd.cart;

import com.example.ddd.cart.application.port.CatalogGateway;
import com.example.ddd.cart.application.service.CartApplicationService;
import com.example.ddd.cart.domain.model.aggregate.Cart;
import com.example.ddd.cart.domain.repository.CartRepository;
import com.example.ddd.common.exception.BusinessException;
import com.example.ddd.contract.product.SkuDTO;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 购物车回归：聚合不变式、数量合并、实时价格及选中条目的结算边界。 */
class CartTest {
    @Test void mergeChangeAndRemove() {
        var cart = new Cart("user", List.of());
        cart.add("sku", 2);
        cart.add("sku", 3);
        assertThat(cart.items()).containsExactly(new Cart.Item("sku", 5, true));
        cart.change("sku", 4, false);
        assertThat(cart.items()).containsExactly(new Cart.Item("sku", 4, false));
        assertThatThrownBy(() -> cart.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        cart.remove("sku"); cart.remove("sku");
        assertThat(cart.items()).isEmpty();
        assertThatThrownBy(() -> cart.change("sku", 1, true)).isInstanceOf(BusinessException.class);
    }

    @Test void limitsDoNotCorruptExistingItems() {
        var cart = new Cart("user", List.of());
        cart.add("sku", 999);
        assertThatThrownBy(() -> cart.add("sku", 1)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> cart.change("sku", 0, true)).isInstanceOf(BusinessException.class);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(999);
        for (int i = 1; i < 100; i++) cart.add("sku" + i, 1);
        assertThatThrownBy(() -> cart.add("overflow", 1)).isInstanceOf(BusinessException.class);
        assertThat(cart.items()).hasSize(100);
        cart.clear();
        assertThat(cart.items()).isEmpty();
    }

    @Test void previewUsesFreshPricesAndIgnoresUncheckedUnavailableSku() {
        var repository = mock(CartRepository.class);
        var catalog = mock(CatalogGateway.class);
        when(repository.load("user", false)).thenReturn(new Cart("user", List.of(
                new Cart.Item("sku", 2, true), new Cart.Item("deleted", 1, false))));
        when(catalog.get("sku")).thenReturn(sku("40.00", "ON_SALE"), sku("50.00", "ON_SALE"));
        var service = new CartApplicationService(repository, catalog);
        var first = service.preview("user");
        assertThat(first.totalAmount()).isEqualByComparingTo("80.00");
        assertThat(first.payAmount()).isEqualByComparingTo("90.00");
        var second = service.preview("user");
        assertThat(second.payAmount()).isEqualByComparingTo("100.00");
        assertThat(second.shippingFee()).isEqualByComparingTo("0.00");
        verify(catalog, times(2)).get("sku");
        verifyNoMoreInteractions(catalog);
        verify(repository, never()).save(any());
    }

    @Test void previewRejectsEmptyOrOffShelfSelection() {
        var repository = mock(CartRepository.class);
        var catalog = mock(CatalogGateway.class);
        var service = new CartApplicationService(repository, catalog);
        when(repository.load("user", false)).thenReturn(new Cart("user", List.of()));
        assertThatThrownBy(() -> service.preview("user")).isInstanceOf(BusinessException.class);
        when(repository.load("user", false)).thenReturn(new Cart("user", List.of(new Cart.Item("sku", 1, true))));
        when(catalog.get("sku")).thenReturn(sku("40.00", "OFF_SHELF"));
        assertThatThrownBy(() -> service.preview("user")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.add("user", "sku", 1)).isInstanceOf(BusinessException.class);
        verify(repository, never()).load("user", true);
    }

    private SkuDTO sku(String price, String status) {
        return new SkuDTO("sku", "spu", "商品", "规格", "{}", null, new BigDecimal(price), status);
    }
}
