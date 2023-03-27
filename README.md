# SI Protected User List Admin Frontend

Admin frontend microservice to manage restricted users.

## API

All paths prefixed with `/si-protected-user-list-admin`.

| Path                   | Methods     | Description                                                                  |
|------------------------|-------------|------------------------------------------------------------------------------|
| `/add`                 | `GET, POST` | Loads the 'add user' form / Adds a specified user to the allowlist           |
| `/file-upload`         | `GET, POST` | Loads the 'file upload' form / Processes the specified file for bulk uploads |
| `/show-find-form`      | `GET`       | Loads the 'search' form                                                      |
| `/show-all/:ascending` | `GET`       | Retrieves all records with a flag to set sort order                          |
| `/show-records`        | `POST`      | Retrieves all records with the given tax ID.                                 |
| `/delete-records`      | `POST`      | Deletes all records with the given tax ID.                                   |
| `/clear`               | `GET`       | Clears the 'add user' form                                                   |
| `/migrate-data`        | `GET`       | Migrates login data from One time password                                   |

### GET /si-protected-user-list-admin/add

Show the form which adds a `(taxId, authProviderId)` pair to the list.

| Status | Message |
|--------|---------|
| `200`  | Ok      |

### POST /si-protected-user-list-admin/add

Submit the form which adds a `(taxId, authProviderId)` pair to the list.

#### Successful Request

```json
{
  "taxId": {
    "name": "NINO",
    "value": "QQ123456A"
  },
  "authProviderId": {
    "name": "GovernmentGateway",
    "value": "1234568790"
  },
  "comment": ""
}
```

| Status | Message                                    |
|--------|--------------------------------------------|
| `200`  | Ok                                         |
| `400`  | Bad Request                                |
| `409`  | Entry not added, already exists, see below |

### /si-protected-user-list-admin/file-upload

File upload form to add multiple `(taxId, authProviderId)` pairs to the list.

| Status | Message             |
|--------|---------------------|
| `200`  | Ok                  |
| `503`  | Service Unavailable |

### POST /si-protected-user-list-admin/file-upload

Submit the file upload form to add a `(taxId, authProviderId)` pair to the list.

The submitted file must:

- be a .csv
- have a header row conforming to the case-sensitive string: "TaxIdType,TaxId,AuthProvider,AuthProviderId,Comment"
- the data is ordered as per the header row
- the data is not in an invalid format
- the row limit has not been exceeded

| Status | Message                                             |
|--------|-----------------------------------------------------|
| `200`  | Ok                                                  |
| `303`  | specified depending on problem found with .csv file |

### GET /si-protected-user-list-admin/show-find-form

Search form to find all auth provider IDs locked to a given tax ID.

| Status | Message |
|--------|---------|
| `200`  | Ok      |

### GET /si-protected-user-list-admin/:ascending

Returns all users in the login allowlist sorted by tax ID and auth provider ID.

| Status | Message   |
|--------|-----------|
| `200`  | Ok        |
| `404`  | Not Found |

### POST /si-protected-user-list-admin/show-records

Used by form to search for a given taxID and responds with records of all connected auth provider IDs.

#### Successful Request

```json
{
  "taxId": {
    "name": "NINO",
    "value": "QQ123456A"
  }
}
```

#### Successful Response

```json
[
  {
    "taxId": {
      "name": "NINO",
      "value": "QQ123456A"
    },
    "authProviderId": {
      "name": "GovernmentGateway",
      "value": "1234568790"
    },
    "comment": ""
  },
  {
    "taxId": {
      "name": "NINO",
      "value": "QQ123456A"
    },
    "authProviderId": {
      "name": "OLfG",
      "value": "0987654321"
    },
    "comment": "Lorem ipsum dolor sit amet"
  }
]
```

| Status | Message   |
|--------|-----------|
| `200`  | Ok        |
| `404`  | Not Found |

### POST /si-protected-user-list-admin/delete-records

Handles the delete user form submit to remove all records for a given tax ID.

| Status | Message   |
|--------|-----------|
| `200`  | Ok        |
| `404`  | Not Found |

### GET /si-protected-user-list-admin/clear

Reloads the Add to allowlist form to add a user to the list.

| Status | Message |
|--------|---------|
| `200`  | Ok      |

### GET /si-protected-user-list-admin/migrate-data

Calls one time password to retrieve all the listed users in order to migrate them into si-protected-user-list-admin.
Will be removed once migration is complete.

| Status | Message                   |
|--------|---------------------------|
| `200`  | Ok                        |
| `400`  | Functionality Not Enabled |
