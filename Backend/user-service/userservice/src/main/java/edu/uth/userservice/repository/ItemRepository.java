package edu.uth.userservice.repository;

import edu.uth.userservice.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Integer> {
    List<Item> findByOwnerIdOrderByCreatedAtDesc(Integer ownerId);
}
