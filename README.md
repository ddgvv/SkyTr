# Elevate Sky Translate Account Setup Tool

### Requirements

- Ensure that Google Cloud Organization and permissions for the `Admin service account` have been setup properly.
- Change the Credentials in `key.json`
- `mvn clean compile` are correctly setup the project.

### Tips on Google Cloud Platform

- Number of projects you can create is limited.
- Number of projects you can link to a billing account is limited.
- Google Cloud removes projects 30 days after deletion, even if you manually delete the projects they will still factor into the billing/creation counts until after 30 days.
- project_ids for each project generated are permanent, even after project deletion. Google has an internal memory, be careful with potential collisions.

