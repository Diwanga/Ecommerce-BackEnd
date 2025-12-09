   package com.diwangaamasith.ProductService.service;

//import com.diwangaamasith.ProductService.model.ProductRequest;
//import com.diwangaamasith.ProductService.model.ProductResponse;

   import com.diwangaamasith.ProductService.model.ProductRequest;
   import com.diwangaamasith.ProductService.model.ProductResponse;

   public interface ProductService {


    long addProduct(ProductRequest productRequest);
//
    ProductResponse getProductById(long productId);
//
    void reduceQuantity(long productId, long quantity);
}
