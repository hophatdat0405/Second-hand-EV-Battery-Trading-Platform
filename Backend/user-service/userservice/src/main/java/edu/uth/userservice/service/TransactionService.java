package edu.uth.userservice.service;

import edu.uth.userservice.model.Transaction;
import edu.uth.userservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository repo;

    public List<Transaction> findTransactionsByUser(Integer userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
