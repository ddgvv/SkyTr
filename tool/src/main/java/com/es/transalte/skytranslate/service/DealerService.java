package com.es.transalte.skytranslate.service;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.resourcemanager.v3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DealerService {
    private FoldersClient folderClient;
    final Logger logger = LoggerFactory.getLogger(DealerService.class);
    private final CredentialsProvider credentialsProvider;

    public DealerService(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public Folder dealerExists(String dealerName, String orgName) throws IOException {
        FoldersSettings foldersSettings = FoldersSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();
        folderClient = FoldersClient.create(foldersSettings);
        logger.info(String.format("Running dealerExists(%s, %s) function...", dealerName, orgName));
        ListFoldersRequest request = ListFoldersRequest.newBuilder()
                .setParent(orgName)
                .build();
        FoldersClient.ListFoldersPagedResponse response = folderClient.listFolders(request);
        for (Folder folder : response.iterateAll()) {
            if (folder.getDisplayName().equals(dealerName)) {
                logger.info(String.format("dealerExists(%s, %s) function returned True", dealerName, orgName));
                return folder;
            }
        }
        logger.info(String.format("dealerExists(%s, %s) function returned False", dealerName, orgName));
        return null;
    }

    public boolean checkDealer(String dealerName) {
        logger.info("Checking Dealer/Folder name input: " + dealerName);
        if (dealerName.length() < 3 || dealerName.length() > 30) {
            logger.warn("Dealer/folder name input: " + dealerName + " must be between 3 and 30 characters.");
            logger.warn("checkDealer with name input: " + dealerName + " returned False");
            return false;
        }
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9 _-]+$");
        Matcher matcher = pattern.matcher(dealerName);
        if (!matcher.matches()) {
            logger.warn("Dealer/folder name input: " + dealerName + " can only contain letters, digits, spaces, hyphens, and underscores.");
            logger.warn("checkDealer with name input: " + dealerName + " returned False");
            return false;
        }
        if (!Character.isLetterOrDigit(dealerName.charAt(0)) || !Character.isLetterOrDigit(dealerName.charAt(dealerName.length() - 1))) {
            logger.warn("Dealer/folder name input: " + dealerName + " must start and end with a letter or digit.");
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
            if (checkDealer(dealerName)) {
                try {
                    Folder folder = Folder.newBuilder()
                            .setParent(orgName)
                            .setDisplayName(dealerName)
                            .build();
                    CreateFolderRequest request = CreateFolderRequest.newBuilder()
                            .setFolder(folder)
                            .build();

                    OperationFuture<Folder, CreateFolderMetadata> operation = folderClient.createFolderAsync(request);
                    Folder response = operation.get();
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

}