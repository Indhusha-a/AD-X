package com.adx.ad_x.service;

import com.adx.ad_x.model.Favorite;
import com.adx.ad_x.model.Product;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    public List<Favorite> getUserFavorites(User user) {
        return favoriteRepository.findByUser(user);
    }

    public boolean isProductFavorited(User user, Product product) {
        return favoriteRepository.existsByUserAndProduct(user, product);
    }

    public Favorite addToFavorites(User user, Product product) {
        Optional<Favorite> existingFavorite = favoriteRepository.findByUserAndProduct(user, product);
        if (existingFavorite.isPresent()) {
            return existingFavorite.get();
        }

        Favorite favorite = new Favorite(user, product);
        return favoriteRepository.save(favorite);
    }

    @Transactional  // ADD THIS ANNOTATION
    public void removeFromFavorites(User user, Product product) {
        favoriteRepository.deleteByUserAndProduct(user, product);
    }

    public Long getFavoriteCount(User user) {
        return favoriteRepository.countByUser(user);
    }

    @Transactional  // ADD THIS ANNOTATION
    public void toggleFavorite(User user, Product product) {
        if (isProductFavorited(user, product)) {
            removeFromFavorites(user, product);
        } else {
            addToFavorites(user, product);
        }
    }
}