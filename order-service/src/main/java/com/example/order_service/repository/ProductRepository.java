package com.example.order_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.order_service.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Product findByProductId(Long id);

    List<Product> findAllByProductIdIn(List<Long> idList);
}