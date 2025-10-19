package local.Second_hand_EV_Battery_Trading_Platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import local.Second_hand_EV_Battery_Trading_Platform.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
