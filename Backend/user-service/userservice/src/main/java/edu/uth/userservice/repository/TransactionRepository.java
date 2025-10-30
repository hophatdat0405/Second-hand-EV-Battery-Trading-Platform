package edu.uth.userservice.repository;

import edu.uth.userservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
