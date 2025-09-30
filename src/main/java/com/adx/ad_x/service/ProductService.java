package com.adx.ad_x.service;

import com.adx.ad_x.model.Product;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // Existing methods...
    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndActiveTrue(category);
    }

    public List<Product> searchProducts(String query) {
        return productRepository.findByTitleContainingIgnoreCaseAndActiveTrue(query);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // Seller-specific methods (Step 5)
    public List<Product> getProductsBySeller(User seller) {
        return productRepository.findBySellerAndActiveTrue(seller);
    }

    public Long getProductCountBySeller(User seller) {
        return productRepository.countBySeller(seller);
    }

    public Optional<Product> getProductByIdAndSeller(Long productId, User seller) {
        return productRepository.findByIdAndSeller(productId, seller);
    }

    public Product createProduct(Product product, User seller) {
        product.setSeller(seller);
        product.setActive(true);
        return productRepository.save(product);
    }

    public Product updateProduct(Long productId, Product productDetails, User seller) {
        Optional<Product> productOpt = productRepository.findByIdAndSeller(productId, seller);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();

            if (productDetails.getTitle() != null) {
                product.setTitle(productDetails.getTitle());
            }
            if (productDetails.getDescription() != null) {
                product.setDescription(productDetails.getDescription());
            }
            if (productDetails.getPrice() != null) {
                product.setPrice(productDetails.getPrice());
            }
            if (productDetails.getCategory() != null) {
                product.setCategory(productDetails.getCategory());
            }
            if (productDetails.getImageUrl() != null) {
                product.setImageUrl(productDetails.getImageUrl());
            }

            return productRepository.save(product);
        }
        return null;
    }

    public boolean deleteProduct(Long productId, User seller) {
        Optional<Product> productOpt = productRepository.findByIdAndSeller(productId, seller);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setActive(false); // Soft delete
            productRepository.save(product);
            return true;
        }
        return false;
    }
}