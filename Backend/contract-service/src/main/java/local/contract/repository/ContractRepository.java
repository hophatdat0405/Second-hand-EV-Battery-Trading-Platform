package local.contract.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import local.contract.entity.Contract;

public interface ContractRepository extends JpaRepository<Contract, Long> {
}
