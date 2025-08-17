package com.sarthak.BizNex.repository;

import com.sarthak.BizNex.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillItemRepository extends JpaRepository<BillItem, Long> {
    // Add custom query methods if needed
}

