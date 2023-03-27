
# si-protected-user-list-admin-frontend

Admin frontend microservice allows the managing of allowlisted users

## API

| Path                                         | Supported Methods | Description                                                                  |
|----------------------------------------------|-------------------|------------------------------------------------------------------------------|
| `/si-protected-user-list-admin/add`                          | `GET, POST`       | Loads the 'add user' form / Adds a specified user to the allowlist           |
| `/si-protected-user-list-admin/file-upload`                  | `GET, POST`       | Loads the 'file upload' form / Processes the specified file for bulk uploads |
| `/si-protected-user-list-admin/show-find-form`               | `GET`             | Loads the 'search' form                                                      |
| `/si-protected-user-list-admin/show-all/sortByOrg/:sortByOrg`| `GET`             | Retrieves all allowlisted users with a flag to set sort order                |
| `/si-protected-user-list-admin/show-allowlisted-user`        | `POST`            | Retrieves a single allowlisted user record                                   |
| `/si-protected-user-list-admin/delete-allowlisted-user`      | `POST`            | Deletes a single allowlisted user                                            |
| `/si-protected-user-list-admin/clear`                        | `GET`             | Clears the 'add user' form                                                   |
| `/si-protected-user-list-admin/migrate-data`                 | `GET`             | Migrates  login data from One time password                            |

###GET         /si-protected-user-list-admin/add

Add to allowlist form to add a user to the  login allowlist

| Status | Message               |
|--------|-----------------------|
| `200`  | Ok                    |


###POST        /si-protected-user-list-admin/add

Submit the add to allowlist form to add a user to the  login allowlist

**Successful Request**

```json
    {
      "name":"123456789012",
      "org":"someOrgName",
      "requester_email":"some@email.com"
    }
```

| Status | Message                                    |
|--------|--------------------------------------------|
| `200`  | Ok                                         |
| `400`  | Bad Request                                |
| `409`  | Entry not added, already exists, see below |


###GET         /si-protected-user-list-admin/file-upload

File upload form to add multiple users to the  login allowlist

| Status | Message               |
|--------|-----------------------|
| `200`  | Ok                    |
| `503`  | Service Unavailable   |


###POST        /si-protected-user-list-admin/file-upload

Submit the file upload form to add a user to the  login allowlist

The submitted file must:
- be a .csv
- have a header row conforming to the case sensitive string: "UserID,OrganisationName,RequesterEmail"
- the data is ordered as per the header row
- the data is not in an invalid format
- the row limit has not been exceeded

| Status | Message                                             |
|--------|-----------------------------------------------------|
| `200`  | Ok                                                  |
| `303`  | specified depending on problem found with .csv file |


###GET         /si-protected-user-list-admin/show-find-form

Search form to find a user in the  login allowlist

| Status | Message               |
|--------|-----------------------|
| `200`  | Ok                    |


###GET         /si-protected-user-list-admin/sortByOrg/:sortByOrg

Returns all users in the  login allowlist sorted by either organisation Name or User Id

| Status | Message               |
|--------|-----------------------|
| `200`  | Ok                    |
| `404`  | Not Found             |


###POST        /si-protected-user-list-admin/show-allowlisted-user

Used by form to submit the search criteria for allowlisted user and responds with a user if in the database

**Successful Request**

```json
    {
        "username":"123456789012"
    }
```
**Successful Response**

```json
    {
        "username":"123456789012",
        "organisationName":"someOrgName",
        "requesterEmail":"someRequesterEmail@email.com"
    }
```

| Status | Message               |
|--------|-----------------------|
| `200`  | Ok                    |
| `404`  | Not Found             |

###POST        /si-protected-user-list-admin/delete-allowlisted-user

Handles the delete user form submit to delete user in the allowlist database

| Status | Message               |
|--------|-----------------------|
| `200`  | Ok                    |
| `404`  | Not Found             |


###GET         /si-protected-user-list-admin/clear

reloads the Add to allowlist form to add a user to the  login allowlist

| Status | Message               |
|--------|-----------------------|
| `200`  | Ok                    |

###GET         /si-protected-user-list-admin/migrate-data

Calls one time password to retrieve all the allowlisted users in order to migrate them into si-protected-user-list-admin.
Will be removed once migration is complete

| Status | Message                   |
|--------|---------------------------|
| `200`  | Ok                        |
| `400`  | Functionality Not Enabled |
