package com.es.transalte.skytranslate.service;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.resourcemanager.v3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        } catch (Exception e ) {
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


}
