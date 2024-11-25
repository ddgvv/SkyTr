package com.es.transalte.skytranslate.handler;

import com.es.transalte.skytranslate.service.*;
import com.google.cloud.resourcemanager.v3.*;
import com.google.iam.admin.v1.ServiceAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.es.transalte.skytranslate.constants.ServiceConstants.*;
import static com.es.transalte.skytranslate.constants.ServiceConstants.SERVICE_NAME;

public class ProvisionalHandler {
    final Logger logger = LoggerFactory.getLogger(AccountProvisionService.class);

    private BillingService billingService;
    private OrgService orgService;
    private DealerService dealerService;
    private CustomerService customerService;
    private AccountProvisionService accountProvisionService;
    private UserService userService;

    public ProvisionalHandler(BillingService billingService, OrgService orgService, DealerService dealerService,
              CustomerService customerService, AccountProvisionService accountProvisionService, UserService userService) {
        this.billingService = billingService;
        this.orgService = orgService;
        this.dealerService = dealerService;
        this.customerService = customerService;
        this.accountProvisionService = accountProvisionService;
        this.userService = userService;
    }

    // there is a generated keyfile
    public String processFirstPage(String dealerName, String custName, String budgetAmount) {
        try {
            if (dealerName != null && custName != null && budgetAmount != null) {
                String billingAccount = billingService.getBillingAccount();
                String orgName = orgService.getOrgName();
                Folder folderResponse = dealerService.createDealer(dealerName, orgName);
                Project cust = customerService.createCustomer(folderResponse.getName(), folderResponse.getDisplayName(), custName);
                billingService.linkBillingAccount(cust.getProjectId(), billingAccount);
                int budgetAmountValue = switch (budgetAmount) {
                    case "Enterprise" -> ENTERPRISE_PRICE;
                    case "Business" -> BUSINESS_PRICE;
                    case "Basic" -> BASIC_PRICE;
                    default -> throw new IllegalArgumentException("Invalid budget amount");
                };
                billingService.createBudget(billingAccount, cust.getProjectId(), budgetAmountValue);
                accountProvisionService.enableService(cust.getProjectId(), SERVICE_NAME);
                String serviceAccountName = cust.getProjectId() + "-service";
                ServiceAccount account = accountProvisionService.createServiceAccount(cust.getProjectId(), serviceAccountName);
                userService.addUserRole(account.getProjectId(), account.getEmail());
                return accountProvisionService.createKey(account.getProjectId(), account.getUniqueId());
            } else {
                logger.warn("Dealer Name and/or customer name cannot be empty. Please check your inputs and try again.");
            }
        } catch (Exception e) {
            logger.warn("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
