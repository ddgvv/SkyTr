package com.es.transalte.skytranslate.service;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.serviceusage.v1.*;
import com.google.cloud.billing.budgets.v1.BudgetServiceClient;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.billing.budgets.v1.*;
import com.google.cloud.billing.v1.CloudBillingClient;
import com.google.cloud.billing.v1.CloudBillingSettings;
import com.google.cloud.billing.v1.ProjectBillingInfo;
import com.google.cloud.billing.v1.UpdateProjectBillingInfoRequest;
import com.google.cloud.iam.admin.v1.IAMClient;
import com.google.cloud.iam.admin.v1.IAMSettings;
import com.google.cloud.resourcemanager.v3.*;
import com.google.iam.admin.v1.CreateServiceAccountKeyRequest;
import com.google.iam.admin.v1.CreateServiceAccountRequest;
import com.google.iam.admin.v1.ServiceAccount;
import com.google.iam.admin.v1.ServiceAccountKey;
import com.google.iam.v1.Binding;
import com.google.iam.v1.Policy;
import com.google.protobuf.ByteString;
import com.google.type.Money;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.es.transalte.skytranslate.constants.ServiceConstants.*;

@Service
public class GoogleResourceManagerService {
    private final Random random = new Random();
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String ALPHANUMERIC_AND_DASH = LOWERCASE + "0123456789-";
    private FoldersClient folderClient;
    final Logger logger = LoggerFactory.getLogger(GoogleResourceManagerService.class);
    private ProjectsClient client;
    private final CredentialsProvider credentialsProvider;

    public GoogleResourceManagerService(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        try {
            ProjectsSettings foldersSettings = ProjectsSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .build();

            this.client = ProjectsClient.create(foldersSettings);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getOrgName() throws IOException {
        logger.info("Running getOrgName() function...");

        // Create the OrganizationsClient with the provided credentials
        try (OrganizationsClient client = OrganizationsClient.create(
                OrganizationsSettings.newBuilder().setCredentialsProvider(credentialsProvider).build())) {

            // Initialize the request
            SearchOrganizationsRequest request = SearchOrganizationsRequest.newBuilder().build();

            // Make the request
            OrganizationsClient.SearchOrganizationsPagedResponse response = client.searchOrganizations(request);

            // Process the response
            logger.info("getOrgName() response: ");
            for (Organization organization : response.iterateAll()) {
                logger.info(organization.toString());
                return organization.getName(); // Return the name of the first organization
            }
        } catch (IOException e) {
            logger.error("Error occurred while fetching organization name", e);
            throw e;
        }
        return null; // Return null if no organizations are found
    }

    public boolean customerExists(String dealerName, String customerName) throws IOException {
        logger.info("Running customerExists({}, {}) function...", dealerName, customerName);

        // Load credentials from the JSON file

        ProjectsSettings projectsSettings = ProjectsSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();

        // Initialize ProjectsClient
        try (ProjectsClient client = ProjectsClient.create(projectsSettings)) {

            // Create the request
            ListProjectsRequest request = ListProjectsRequest.newBuilder().setParent(customerName)
                    .build();

            // Fetch and check the projects
            for (Project response : client.listProjects(request).iterateAll()) {
                if (response.getDisplayName().equals(dealerName)) {
                    logger.info("customerExists() function returned True");
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public Folder dealerExists(String dealerName, String orgName) throws IOException {
        // Configure FoldersClient with the credentials
        FoldersSettings foldersSettings = FoldersSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();
        folderClient = FoldersClient.create(foldersSettings);
        // Initialize the client
        logger.info(String.format("Running dealerExists(%s, %s) function...", dealerName, orgName));

        // Set up the request to list folders under the given organization
        ListFoldersRequest request = ListFoldersRequest.newBuilder()
                .setParent(orgName)
                .build();

        // Make the request
        FoldersClient.ListFoldersPagedResponse response = folderClient.listFolders(request);

        // Iterate through folders and check if one matches the dealerName
        for (Folder folder : response.iterateAll()) {
            if (folder.getDisplayName().equals(dealerName)) {
                logger.info(String.format("dealerExists(%s, %s) function returned True", dealerName, orgName));
                return folder;  // Return the matching folder if found
            }
        }
        logger.info(String.format("dealerExists(%s, %s) function returned False", dealerName, orgName));
        return null;  // Return null if no matching folder is found
    }

    public boolean checkDealer(String dealerName) {
        logger.info("Checking Dealer/Folder name input: " + dealerName);

        // Check length
        if (dealerName.length() < 3 || dealerName.length() > 30) {
            logger.warn("Dealer/folder name input: " + dealerName + " must be between 3 and 30 characters.");
            System.out.println("Dealer name must be between 3 and 30 characters.");
            logger.warn("checkDealer with name input: " + dealerName + " returned False");
            return false;
        }

        // Check allowed characters
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9 _-]+$");
        Matcher matcher = pattern.matcher(dealerName);
        if (!matcher.matches()) {
            logger.warn("Dealer/folder name input: " + dealerName + " can only contain letters, digits, spaces, hyphens, and underscores.");
            System.out.println("Dealer name can only contain letters, digits, spaces, hyphens, and underscores.");
            logger.warn("checkDealer with name input: " + dealerName + " returned False");
            return false;
        }

        // Check starting and ending characters
        if (!Character.isLetterOrDigit(dealerName.charAt(0)) || !Character.isLetterOrDigit(dealerName.charAt(dealerName.length() - 1))) {
            logger.warn("Dealer/folder name input: " + dealerName + " must start and end with a letter or digit.");
            System.out.println("Dealer name must start and end with a letter or digit.");
            logger.warn("checkDealer with name input: " + dealerName + " returned False");
            return false;
        }

        logger.info("checkDealer with name input: " + dealerName + " returned True");
        return true;
    }

    public Folder createDealer(String dealerName, String orgName) throws IOException {
        logger.info("Running createDealer, creating dealer: " + dealerName + " in Organization: " + orgName);

        Folder dealer = dealerExists(dealerName, orgName);
        if (dealer != null) {
            logger.info("Dealer name already exists in organization.");
            return dealer;
        } else {
            if (checkDealer(dealerName)) {  // Checks if the dealerName is valid
                try {
                    Folder folder = Folder.newBuilder()
                            .setParent("organizations/" + orgName)
                            .setDisplayName(dealerName)
                            .build();
                    CreateFolderRequest request = CreateFolderRequest.newBuilder()
                            .setFolder(folder)
                            .build();

                    OperationFuture<Folder, CreateFolderMetadata> operation = folderClient.createFolderAsync(request);
                    Folder response = operation.get();  // Blocks until the operation completes
                    logger.info("createDealer returned: " + response);
                    return response;
                } catch (ApiException | InterruptedException e) {
                    logger.warn("Error creating dealer folder: " + e.getMessage());
                    return null;
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            } else {
                logger.info("checkDealer test failed.");
                return null;
            }
        }
    }


    public Project createCustomer(String parent, String dealerName, String customerName) {
        logger.info("Creating customer: " + customerName + " in parent: " + dealerName);

        try {
            if (customerExists(parent, customerName)) {
                logger.warn("Customer " + customerName + " already exists under Dealer: " + dealerName);
                return null;
            } else {
                String projectId = generateProjectId(customerName);
                Project project = Project.newBuilder()
                        .setParent(parent)
                        .setProjectId(projectId)
                        .setDisplayName(customerName)
                        .putLabels("dealer", dealerName)
                        .build();

                CreateProjectRequest request = CreateProjectRequest.newBuilder()
                        .setProject(project)
                        .build();

                try {
                    OperationFuture<Project, CreateProjectMetadata> operation = client.createProjectAsync(request);
                    Project response = operation.get();
                    logger.info("createCustomer " + customerName + " in parent " + dealerName + " returned: " + response);
                    return response;
                } catch (ApiException | InterruptedException e) {
                    logger.warn("Error creating customer project: " + e.getMessage());
                    return null;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    private String generateProjectId(String customerName) {
        int length = 6 + random.nextInt(5);

        StringBuilder projectId = new StringBuilder(customerName.toLowerCase().replaceAll(" ", "-").substring(0, Math.min(customerName.length(), 20)) + "-");

        for (int i = 0; i < length - 2; i++) {
            projectId.append(randomChar());
        }

        projectId.append(randomChar(true));  // Append final character (letter or digit)

        return projectId.toString();
    }

    private char randomChar() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789-";
        return chars.charAt(random.nextInt(chars.length()));
    }

    private char randomChar(boolean lettersOnly) {
        String chars = lettersOnly ? "abcdefghijklmnopqrstuvwxyz0123456789" : "abcdefghijklmnopqrstuvwxyz0123456789-";
        return chars.charAt(random.nextInt(chars.length()));
    }

    public String generateServiceAccountId() {
        System.out.println("Running generateServiceAccountId...");

        // Length of the service account ID (between 6 and 30 characters)
        int length = random.nextInt(25) + 6; // Random number between 6 and 30

        // First character: random lowercase letter
        char firstChar = LOWERCASE.charAt(random.nextInt(LOWERCASE.length()));

        // Middle characters: random alphanumeric + dash, length - 2
        StringBuilder middleChars = new StringBuilder();
        for (int i = 0; i < length - 2; i++) {
            middleChars.append(ALPHANUMERIC_AND_DASH.charAt(random.nextInt(ALPHANUMERIC_AND_DASH.length())));
        }

        // Last character: random alphanumeric
        char lastChar = LOWERCASE.charAt(random.nextInt(LOWERCASE.length() + 10)); // 26 letters + 10 digits

        // Constructing the service account ID
        String serviceAccountId = firstChar + middleChars.toString() + lastChar;

        System.out.println("generateServiceAccountId returned: " + serviceAccountId);
        return serviceAccountId;
    }

    public void linkBillingAccount(String projectId, String billingAccount) throws IOException {
        logger.info("Running linkBillingAccount...");
        logger.info("Trying to link project_id: " + projectId + " to billing_account: " + billingAccount);

        CloudBillingSettings billingSettings = CloudBillingSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();
        // Initialize CloudBillingClient
        try (CloudBillingClient client = CloudBillingClient.create(billingSettings)) {
            // Set up ProjectBillingInfo with the billing account name
            ProjectBillingInfo projectBillingInfo = ProjectBillingInfo.newBuilder()
                    .setBillingAccountName(billingAccount)
                    .build();

            // Create request with project name and billing info
            String name = "projects/" + projectId;
            UpdateProjectBillingInfoRequest request = UpdateProjectBillingInfoRequest.newBuilder()
                    .setName(name)
                    .setProjectBillingInfo(projectBillingInfo)
                    .build();

            // Execute request and retrieve response
            ProjectBillingInfo response = client.updateProjectBillingInfo(request);
            logger.info("linkBillingAccount response: " + response);
        } catch (IOException e) {
            logger.warn("Failed to link billing account: " + e.getMessage());
            throw e;
        }
    }


    public Budget createBudget(String billingAccountId, String projectId, long budgetAmount) {
        logger.info("Running createBudget...");
        try {
            BudgetServiceSettings budgetServiceSettings = BudgetServiceSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .build();
            try (BudgetServiceClient budgetClient = BudgetServiceClient.create(budgetServiceSettings)) {

                // Set the parent resource (billing account ID)
                String parent = String.format(billingAccountId);

                // Define budget details
                Budget budget = Budget.newBuilder()
                        .setDisplayName("Budget for " + projectId)
                        .setBudgetFilter(Filter.newBuilder()
                                .addProjects("projects/" + projectId)
                                .build())
                        .setAmount(BudgetAmount.newBuilder()
                                .setSpecifiedAmount(Money.newBuilder()
                                        .setCurrencyCode("USD")
                                        .setUnits(budgetAmount)
                                        .build())
                                .build())
                        .addThresholdRules(ThresholdRule.newBuilder()
                                .setThresholdPercent(0.5)
                                .setSpendBasis(ThresholdRule.Basis.CURRENT_SPEND)
                                .build())
                        .addThresholdRules(ThresholdRule.newBuilder()
                                .setThresholdPercent(0.9)
                                .setSpendBasis(ThresholdRule.Basis.CURRENT_SPEND)
                                .build())
                        .addThresholdRules(ThresholdRule.newBuilder()
                                .setThresholdPercent(0.95)
                                .setSpendBasis(ThresholdRule.Basis.CURRENT_SPEND)
                                .build())
                        .build();

                // Create the budget
                CreateBudgetRequest request = CreateBudgetRequest.newBuilder()
                        .setParent(parent)
                        .setBudget(budget)
                        .build();
                Budget response = budgetClient.createBudget(request);

                logger.info("Budget created successfully: " + response.getName());
                return response;
            }
        } catch (IOException e) {
            logger.warn("Failed to create BudgetServiceClient: " + e.getMessage());
        }
        return null;
    }

    public ServiceAccount createServiceAccount(String projectId, String displayName) {
        try {
            logger.info("Running createServiceAccount with projectId: " + projectId + " and displayName: " + displayName);

            // Initialize the IAM client
            IAMSettings iamSettings = IAMSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .build();
            try (IAMClient iamClient = IAMClient.create(iamSettings)) {

                // Generate a unique account ID
                String accountId = generateServiceAccountId();

                // Prepare the request to create the service account
                CreateServiceAccountRequest request = CreateServiceAccountRequest.newBuilder()
                        .setName(ProjectName.of(projectId).toString())
                        .setAccountId(accountId)
                        .setServiceAccount(ServiceAccount.newBuilder()
                                .setDisplayName(displayName)
                                .build())
                        .build();

                // Create the service account
                ServiceAccount serviceAccount = iamClient.createServiceAccount(request);
                logger.info("createServiceAccount response: " + serviceAccount);
                return serviceAccount;

            }
        } catch (NotFoundException e) {
            logger.warn("Project not found: " + e.getMessage());
        } catch (IOException e) {
            logger.warn("Error initializing IAM client or creating service account: " + e.getMessage());
        }
        return null;
    }

    public void enableService(String projectId, String service) {
        logger.info("Running enableService...");
        try {
            ServiceUsageSettings settings = ServiceUsageSettings.newBuilder()
                    .setCredentialsProvider(credentialsProvider)
                    .build();
            try (ServiceUsageClient client = ServiceUsageClient.create(settings)) {

                // Prepare the EnableServiceRequest
                String serviceName = String.format("projects/%s/%s", projectId, service);
                EnableServiceRequest request = EnableServiceRequest.newBuilder()
                        .setName(serviceName)
                        .build();

                // Enable the service and wait for the operation to complete
                OperationFuture<EnableServiceResponse, OperationMetadata> future = client.enableServiceAsync(request);
                EnableServiceResponse response = future.get();  // This blocks until the operation is complete
                logger.info("Enable service response: " + response);

            } catch (ExecutionException | InterruptedException e) {
                logger.warn("Error enabling service: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        } catch (IOException e) {
            logger.warn("Failed to create ServiceUsageClient: " + e.getMessage());
        }
    }


    public String createKey(String projectId, String serviceAccountEmail) throws IOException {
        IAMSettings iamSettings = IAMSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();
        try (IAMClient iamClient = IAMClient.create(iamSettings)) {
            // Construct the request for creating a service account key
            String resourceName = String.format("projects/%s/serviceAccounts/%s", projectId, serviceAccountEmail);

            // Prepare the request to create the service account
            CreateServiceAccountKeyRequest request = CreateServiceAccountKeyRequest.newBuilder()
                    .setName(resourceName)
                    .build();

            ServiceAccountKey serviceAccountKey = iamClient.createServiceAccountKey(request);
            ByteString privateKeyData = serviceAccountKey.getPrivateKeyData();
            return privateKeyData.toString();

        }
    }


//    public void addUserRole(String projectId, String serviceAccountEmail) throws IOException {
//        System.out.println("Running addUserRole...");
//        System.out.println("Project ID: " + projectId + ", Service Account Email: " + serviceAccountEmail);
//
//
//        // Define resource and role
//        String resource = "projects/" + projectId;
//        String role = "roles/cloudtranslate.user";
//        String member = "serviceAccount:" + serviceAccountEmail;
//
//        // Get IAM Policy
//        Policy policy = client.getIamPolicy(resource);
//
//        // Check for the binding and update policy
//        boolean bindingFound = false;
//        List<Binding> bindings = policy.getBindingsList();
//
//        for (Binding binding : bindings) {
//            if (role.equals(binding.getRole())) {
//                if (!binding.contains(member)) {
//                    binding.getMembers().add(member);
//                }
//                bindingFound = true;
//                break;
//            }
//        }
//
//        if (!bindingFound) {
//            Binding newBinding = new Binding()
//                    .setRole(role)
//                    .setMembers(Collections.singletonList(member));
//            bindings.add(newBinding);
//        }
//
//        // Set the updated IAM Policy
//        client.setIamPolicy(resource, new com.google.api.services.cloudresourcemanager.v3.model.SetIamPolicyRequest()
//                .setPolicy(policy)).execute();
//
//        System.out.println("Successfully updated IAM Policy for resource: " + resource);
//    }


    public String processFirstPage() {
        // Input data
        String dealerName = "tbs-west";
        String custName = "test21";
        String budgetAmount = "Basic"; // Options: Enterprise, Business, Basic

        String basicPage = (int) Math.ceil((double) BASIC_PRICE / BASIC_PAGE_PRICE) + " pages";
        String businessPage = (int) Math.ceil((double) BUSINESS_PRICE / BUSINESS_PAGE_PRICE) + " pages";
        String enterprisePage = (int) Math.ceil((double) ENTERPRISE_PRICE / ENTERPRISE_PAGE_PRICE) + " pages";

        try {
            if (dealerName != null && custName != null && budgetAmount != null) {
                String billingAccount = new BillingService().getBillingAccount("");
                String orgName = getOrgName();
                Folder folderResponse = createDealer(dealerName, orgName);
                Project cust = createCustomer(folderResponse.getName(), folderResponse.getDisplayName(), custName);
                linkBillingAccount(cust.getProjectId(), billingAccount);
                int budgetAmountValue = switch (budgetAmount) {
                    case "Enterprise" -> ENTERPRISE_PRICE;
                    case "Business" -> BUSINESS_PRICE;
                    case "Basic" -> BASIC_PRICE;
                    default -> throw new IllegalArgumentException("Invalid budget amount");
                };
                createBudget(billingAccount, cust.getProjectId(), budgetAmountValue);
                enableService(cust.getProjectId(), SERVICE_NAME);
                String serviceAccountName = cust.getProjectId() + "-service";
                ServiceAccount account = createServiceAccount(cust.getProjectId(), serviceAccountName);
                String keyFile = createKey(account.getProjectId(), account.getUniqueId());
                //addUserRole(account.getProjectId(), account.getEmail());
                return keyFile;
            } else {
                System.err.println("Dealer Name, Customer Name, or Budget Amount cannot be empty. Please check your inputs.");
                logger.warn("Dealer Name and/or customer name cannot be empty. Please check your inputs and try again.");
            }
        } catch (Exception e) {
            logger.warn("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}

//def add_user_role(project_id: str, service_account_email:str) -> None:

//TODO Test
//def create_key(project_id: str, service_account: str) -> str:
//def enable_service(project_id: str, service: str) -> None:
//def create_service_account(project_id: str, displayName: str) -> types.ServiceAccount:
//def create_budget(billing_account_id: str, project_id: str, budget_amount: int):
