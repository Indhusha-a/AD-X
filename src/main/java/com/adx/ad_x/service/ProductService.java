package com.adx.ad_x.service;

import com.adx.ad_x.model.Product;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    public List<Product> getProductsBySeller(User seller) {
        return productRepository.findBySellerAndActiveTrue(seller);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Optional<Product> getProductByIdAndSeller(Long id, User seller) {
        return productRepository.findByIdAndSeller(id, seller);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Product existingProduct = product.get();
            existingProduct.setActive(false); // Soft delete
            productRepository.save(existingProduct);
        }
    }

    public List<Product> searchProducts(String query) {
        return productRepository.findByTitleContainingIgnoreCaseAndActiveTrue(query);
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndActiveTrue(category);
    }

    public List<Product> getProductsByCategories(List<String> categories) {
        return productRepository.findByCategoryInAndActiveTrue(categories);
    }

    public List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        return productRepository.findByPriceRange(minPrice, maxPrice);
    }

    public Long getProductCountBySeller(User seller) {
        return productRepository.countBySeller(seller);
    }

    public List<Product> getFeaturedProducts() {
        // For now, return first 6 active products
        List<Product> allProducts = productRepository.findByActiveTrue();
        return allProducts.stream().limit(6).toList();
    }
}