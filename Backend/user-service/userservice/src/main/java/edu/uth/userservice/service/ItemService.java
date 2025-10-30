package edu.uth.userservice.service;

import edu.uth.userservice.model.Item;
import edu.uth.userservice.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    @Autowired
    private ItemRepository repo;

    public List<Item> findItemsByOwner(Integer ownerId) {
        return repo.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }
}
