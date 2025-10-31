package local.contract.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import local.contract.model.ContractRequest;
import local.contract.model.ContractResponse;
import local.contract.service.ContractService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

  private final ContractService contractService;

  @PostMapping("/sign")
  public ResponseEntity<ContractResponse> sign(@RequestBody ContractRequest request) {
    return ResponseEntity.ok(contractService.signContract(request));
  }
}

