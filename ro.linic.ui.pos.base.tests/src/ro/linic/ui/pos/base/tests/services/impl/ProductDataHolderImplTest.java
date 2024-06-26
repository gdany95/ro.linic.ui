package ro.linic.ui.pos.base.tests.services.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ca.odell.glazedlists.EventList;
import ro.linic.ui.pos.base.model.Product;
import ro.linic.ui.pos.base.services.impl.ProductDataHolderImpl;

class ProductDataHolderImplTest {
	@Test
    void givenDataChanges_whenSetData_thenEventListIsTheSameObject() {
		final ProductDataHolderImpl service = new ProductDataHolderImpl();
		final EventList<Product> dataList = service.getData();
        assertNotNull(dataList);
        
        service.setData(List.of(new Product(1L, "MERCHANDISE", "1", "1", "sku", Set.of(), "name", "uom", false, BigDecimal.ONE,
        		BigDecimal.ZERO, null, new BigDecimal("0.19"))));
        
        assertTrue(dataList == service.getData());
    }
}
