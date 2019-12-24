# Socket Protocol

When not running in CLI mode, HolySheet has the ability to communicate to other programs (a GUI, web interface, etc.) over a socket JSON protocol. When a client (External program) wants information from HolySheet, it sends a request. In turn, HolySheet sends a response containing necessary data. The following are the supported request and responses, with example JSON.

- [BasicPayload](#BasicPayload)
- [ErrorPayload](#ErrorPayload-0)
- [ListRequest](#ListRequest-1)
- [ListResponse](#ListResponse-2)
- [UploadRequest](#UploadRequest-3)
- [UploadStatusResponse](#UploadStatusResponse-4)
- [UploadCompleteResponse](#UploadCompleteResponse-5)
- [DownloadRequest](#DownloadRequest-6)
- [DownloadStatusResponse](#DownloadStatusResponse-7)
- [DownloadCompleteResponse](#DownloadCompleteResponse-8)
- [RemoveRequest](#RemoveRequest-9)
- [RemoveStatusResponse](#RemoveStatusResponse-10)
- [RemoveCompleteResponse](#RemoveCompleteResponse-11)

The following section is an example of how the protocol specification will be displayed:

### ProtocolName (-1)

`Client --> Server`

A description of the protocol. Above shows what direction the request may go in. The -1 in the title is the type ID, if applicable. Types may be matched with their friendly names through the [PayloadType](https://github.com/RubbaBoy/HolySheet/blob/master/src/main/java/com/uddernetworks/holysheet/socket/PayloadType.java) enum. Below will be an example of the available JSON tags.

```json
{
    "one": "value1",
    "two": "value2"
}
```

| Key  | Example Value | Description   |
| ---- | ------------- | ------------- |
| one  | value1        | What one does |
| two  | value2        | What two does |




## BasicPayload

`Client <--> Server`

All requests are JSON, and have a few shared tags among all requests in the BasicPayload. **In future examples, these will be hidden unless stated otherwise**.

```json
{
    "code": 1,
    "type": 1,
    "message": "Success",
    "state": "0317d1f0-6053-4cce-89ba-9e896784820a"
}
```

| Key     | Example Value                          | Description                                                  |
| ------- | -------------------------------------- | ------------------------------------------------------------ |
| code    | 1                                      | The response code of the payload, 1 being successful, <1 unsuccessful. |
| type    | 1                                      | The type of the response for non-dynamic languages like this one. Derived from the [PayloadType](https://github.com/RubbaBoy/HolySheet/blob/master/src/main/java/com/uddernetworks/holysheet/socket/PayloadType.java) enum. |
| message | "Success"                              | Any extra details of the request/response, used for things like errors. The state of the request should not depend on this text. |
| state   | "0317d1f0-6053-4cce-89ba-9e896784820a" | A UUID state generated for a request, and reused for the request's response, weather it be a proper response or error. This is to ensure the correct pairing of otherwise unordered requests and responses. |



## ErrorPayload (0)

`Client <-- Server`

Sent when the server has some kind of issue generating a response. Following JSON includes BasicPayload.

```json
{
    "code": 0,
    "type": 0,
    "message": "An error has occurred",
    "stacktrace": "...stacktrace..."
}
```

| Key        | Example Value           | Description                                                  |
| ---------- | ----------------------- | ------------------------------------------------------------ |
| code       | 0                       | Code of 0, indicating an error has occurred                  |
| type       | 0                       | The [PayloadType#ERROR](https://github.com/RubbaBoy/HolySheet/blob/master/src/main/java/com/uddernetworks/holysheet/socket/PayloadType.java#L7) type |
| message    | "An error has occurred" | Displayable error message, if known                          |
| stacktrace | "...stacktrace..."      | Stacktrace of error                                          |



## ListRequest (1)

`Client --> Server`

Sent to the server to request the listing of files. Current protocol supports a query, however nothing is implemented involving this.

```json
{
    "query": "Query"
}
```

| Key   | Example Value | Description                                                 |
| ----- | ------------- | ----------------------------------------------------------- |
| query | "Query"       | A string payload to search files to list. This can be null. |



## ListResponse (2)

`Client <-- Server`

Contains a list of files and their basic information.

```json
{
    "items": [
         {
           "name": "test.txt",
           "size": 54321,
           "sheets": 6,
           "date": 1577200502088,
           "id": "abcdefghijklmnopqrstuvwxyz"
         }
     ]
}
```

| Key    | Example Value                     | Description                                    |
| ------ | --------------------------------- | ---------------------------------------------- |
| items  | Array of below items in an object | A collection of files/items retrieved.         |
| name   | "test.txt"                        | The name of the file                           |
| size   | 54321                             | The size of the file in bytes                  |
| sheets | 6                                 | The amount of sheets the file consists of      |
| date   | 1577200502088                     | The millisecond timestamp the file was created |
| id     | "abcdefghijklmnopqrstuvwxyz"      | The ID of the file                             |



## UploadRequest (3)

Client --> Server

A request to upload a given file.

## UploadStatusResponse (4)

Client <-- Server

A status update saying how far along an upload is.

## UploadCompleteResponse (5)

Client <-- Server

Sent when an upload has been completed.

## DownloadRequest (6)

Client --> Server

A request to download the given remote file from Sheets to a destination.

## DownloadStatusResponse (7)

Client <-- Server

A status update saying how far along a download is.

## DownloadCompleteResponse (8)

Client <-- Server

Sent when a download has been completed.

## RemoveRequest (9)

Client --> Server

A request to remove the given remote Sheets file.

## RemoveStatusResponse (10)

Client <-- Server

A status update saying how far along a file removal is.

## RemoveCompleteResponse (11)

Client <-- Server

Sent when a removal has been completed.
