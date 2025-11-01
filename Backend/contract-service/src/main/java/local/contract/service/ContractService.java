package local.contract.service;


import local.contract.model.ContractRequest;
import local.contract.model.ContractResponse;

public interface ContractService {
  ContractResponse signContract(ContractRequest request);
  ContractResponse createContract(ContractRequest request);
}



