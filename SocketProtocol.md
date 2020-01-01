# Socket Protocol

When not running in CLI mode, HolySheet has the ability to communicate to other programs (a GUI, web interface, etc.) over a socket JSON protocol. When a client (External program) wants information from HolySheet, it sends a request. In turn, HolySheet sends a response containing necessary data. The following are the supported request and responses, with example JSON.

- [BasicPayload](#BasicPayload)
- [ErrorPayload](#ErrorPayload-0)
- [ListRequest](#ListRequest-1)
- [ListResponse](#ListResponse-2)
- [UploadRequest](#UploadRequest-3)
- [UploadStatusResponse](#UploadStatusResponse-4)
- [DownloadRequest](#DownloadRequest-5)
- [DownloadStatusResponse](#DownloadStatusResponse-6)
- [RemoveRequest](#RemoveRequest-7)
- [RemoveStatusResponse](#RemoveStatusResponse-8)
- [CodeExecutionRequest](#CodeExecutionRequest-9)
- [CodeExecutionResponse](#CodeExecutionResponse-10)
- [CodeExecutionCallbackResponse](#CodeExecutionCallbackResponse-11)
- [SettingRequest](#SettingRequest-12)
- [SettingResponse](#SettingResponse-13)

The following section is an example of how the protocol specification will be displayed:

### ProtocolName (-1)

`Client --> Server`

A description of the protocol. Above shows what direction the request may go in. The -1 in the title is the type ID, if applicable. Types may be matched with their friendly names through the [PayloadType](https://github.com/RubbaBoy/HolySheet/blob/master/src/main/java/com/uddernetworks/holysheet/socket/PayloadType.java) enum. Below will be an example of the available JSON tags.

```json
{
    "one": "value1",
    "two": 1,
    "three": "dog"
}
```

| Key   | Value       | Description                                                  |
| ----- | ----------- | ------------------------------------------------------------ |
| one   | String      | One can be any string                                        |
| two   | Integer     | Two is some random integer                                   |
| three | `(dog\|cat)` | Three can either be "dog" or "cat". Value displayed in regex-like format. |




## BasicPayload

`Client <--> Server`

All requests are JSON, and have a few shared tags among all requests in the BasicPayload, meaning this is the parent to all requests in any direction. If these values can't be found, the request should be ignored. **In future examples, these will be hidden unless stated otherwise**.

```json
{
    "code": 1,
    "type": 1,
    "message": "Success",
    "state": "0317d1f0-6053-4cce-89ba-9e896784820a"
}
```

| Key     | Value          | Description                                                  |
| ------- | -------------- | ------------------------------------------------------------ |
| code    | Integer        | The response code of the payload, 1 being successful, <1 unsuccessful. |
| type    | Integer        | The type of the response for non-dynamic languages like this one. Derived from the [PayloadType](https://github.com/RubbaBoy/HolySheet/blob/master/src/main/java/com/uddernetworks/holysheet/socket/PayloadType.java) enum. |
| message | String         | Any extra details of the request/response, used for things like errors. The state of the request should not depend on this text. |
| state   | Untrimmed UUID | A UUID state generated for a request, and reused for the request's response, weather it be a proper response or error. This is to ensure the correct pairing of otherwise unordered requests and responses. |



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

| Key        | Value  | Description                                                  |
| ---------- | ------ | ------------------------------------------------------------ |
| code       | `0`    | Code of 0, indicating an error has occurred                  |
| type       | `0`    | The [PayloadType#ERROR](https://github.com/RubbaBoy/HolySheet/blob/master/src/main/java/com/uddernetworks/holysheet/socket/PayloadType.java#L7) type |
| message    | String | Displayable error message, if known                          |
| stacktrace | String | Stacktrace of error                                          |



## ListRequest (1)

`Client --> Server`

Sent to the server to request the listing of files. Current protocol supports a query, however nothing is implemented involving this.

```json
{
    "query": "Query"
}
```

| Key   | Value  | Description                                                 |
| ----- | ------ | ----------------------------------------------------------- |
| query | String | A string payload to search files to list. This can be null. |



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

| Key    | Example Value | Description                                                  |
| ------ | ------------- | ------------------------------------------------------------ |
| items  | Item[]        | A collection of files/items retrieved. The Item object is outlined in the following 5 properties. |
| name   | String        | The name of the file                                         |
| size   | Long          | The size of the file in bytes                                |
| sheets | Integer       | The amount of sheets the file consists of                    |
| date   | Long          | The millisecond timestamp the file was created               |
| id     | String        | The sheets-generated ID of the file                          |



## UploadRequest (3)

`Client --> Server`

A request to upload a given file. Either `file` or `id` may be defined below, however only one.

```json
{
    "file": "file:///c:/file.txt",
    "upload": "multipart",
    "compression": "zip"
}
```

| Key         | Value                 | Description                                                  |
| ----------- | --------------------- | ------------------------------------------------------------ |
| file        | URL                   | The URL of the file to upload                                |
| id          | String                | The Google Drive ID of the file to download and upload       |
| upload      | `(multipart\|direct)` | Toggles multipart or direct uploading (Multipart recommended) |
| compression | `(none\|zip)`         | The compression algorithm to use, if any.                    |



## UploadStatusResponse (4)

`Client <-- Server`

A status update saying how far along an upload is.

```json
{
    "status": "UPLOADING",
    "percentage": 0.856,
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

| Key        | Value                            | Description                                                  |
| ---------- | -------------------------------- | ------------------------------------------------------------ |
| status     | `(PENDING\|UPLOADING\|COMPLETE)` | The status of the upload. If complete, the                   |
| percentage | Double                           | The 0-1 percentage of the file upload. If pending, this value should be 0. |
| items      | Item[]                           | A collection of files/items uploaded. This list is only populated if the status is `COMPLETE`. The Item object is outlined in the following 5 properties. |
| name       | String                           | The name of the file                                         |
| size       | Long                             | The size of the file in bytes                                |
| sheets     | Integer                          | The amount of sheets the file consists of                    |
| date       | Long                             | The millisecond timestamp the file was created               |
| id         | String                           | The sheets-generated ID of the file                          |


## DownloadRequest (5)

`Client --> Server`

A request to download the given remote file from Sheets to a destination.

```json
{
    "id": "1KLruEf0d8GJgf7JGaYUiNnW_Pe0Zumvq",
    "path": "E:\\file.mp4"
}
```

| Key  | Value  | Description                                     |
| ---- | ------ | ----------------------------------------------- |
| id   | String | The Sheets-generated ID of the file to download |
| path | String | The file path to save the file to               |



## DownloadStatusResponse (6)

`Client <-- Server`

A status update saying how far along a download is.

```json
{
    "status": "DOWNLOADING",
    "percentage": "0.856"
}
```

| Key        | Value                              | Description                                                  |
| ---------- | ---------------------------------- | ------------------------------------------------------------ |
| status     | `(PENDING\|DOWNLOADING\|COMPLETE)` | The status of the download                                   |
| percentage | Double                             | The 0-1 percentage of the file download. If pending, this value should be 0. |



## RemoveRequest (7)

`Client --> Server`

A request to remove the given remote Sheets file.

```json
{
    "id": "1KLruEf0d8GJgf7JGaYUiNnW_Pe0Zumvq"
}
```

| Key  | Value  | Description                                   |
| ---- | ------ | --------------------------------------------- |
| id   | String | The Sheets-generated ID of the file to remove |



## RemoveStatusResponse (8)

`Client <-- Server`

A status update saying how far along a file removal is.

```json
{
    "status": "REMOVING",
    "percentage": "0.856"
}
```

| Key        | Value                           | Description                                                  |
| ---------- | ------------------------------- | ------------------------------------------------------------ |
| status     | `(PENDING\|REMOVING\|COMPLETE)` | The status of the removal                                    |
| percentage | Double                          | The 0-1 percentage of the file removal. If pending, this value should be 0. |



## CodeExecutionRequest (9)

`Client --> Server`

Send to request the arbitrary execution of code on the Java program. This code is ran in a static class and on the same JVM, having access to specific static "access point" variables, used for smaller things it may not be worth (Or too complex) to make a whole new request for, such as file selection. This supports callbacks as well, which will be demonstrated in [CodeExecutionCallbackResponse](#CodeExecutionCallbackResponse-11). Code is invoked via JShell.

```json
{
    "code": "Map.of(\"one\", 1)",
    "returnVariables": [
        "x",
        "y"
    ]
}
```

| Key             | Value    | Description                                                  |
| --------------- | -------- | ------------------------------------------------------------ |
| code            | String   | A snippet of Java code, exactly one complete snippet of source code, that is, one expression,statement, variable declaration, method declaration, class declaration,or import. - [JShell](https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jshell/jdk/jshell/JShell.html#eval(java.lang.String)) |
| returnVariables | String[] | A list of extra variables to return in the response          |



## CodeExecutionResponse (10)

`Client <-- Server`

A response with any variables (automatically or manually) created, along with a dump of all current or previous variables. In the future, there will be an option in the request to only dump certain ones, however this is not available in the early stages of the protocol. The below JSON sample shows the result for the above code executed in [CodeExecutionRequest](#CodeExecutionRequest-9).

```json
{
  "snippetResult": [
    "$1"
  ],
  "variables": [
    {
      "name": "$1",
      "type": "java.util.ImmutableCollections.Map1",
      "object": {
        "one": 1
      }
    }
  ]
}
```

| Key           | Value      | Description                                                  |
| ------------- | ---------- | ------------------------------------------------------------ |
| snippetResult | String[]   | A list of implicit or explicitly created variables in the snippet. |
| variables     | Variable[] | A list of variables listed in the request via `returnVariables`, and in this response via `snippetResult`. The Variable object is outlined in the following 3 properties. |
| name          | String     | The name of the variable                                     |
| type          | String     | The canonical Java class name of the object                  |
| object        | Object     | The Gson-serialized Java object. This is not always small or easy to manage, so it is important to limit variables and general use of this request. |



## CodeExecutionCallbackResponse (11)

`Client <-- Server`

A response sent to the client an arbitrary amount of times, an arbitrary amount of time after a given [CodeExecutionRequest](#CodeExecutionRequest-9). These callbacks are defined in CodeExecutionRequests, and allow for very dynamic variable fetching/code execution. The following snippet is a standard JSON response, and after the property table will be the Java code sent in the CodeExecutionRequest to generate this output. With this code snippet, this callback response is sent 5 seconds after the initial code request.

```json
{
  "callbackState": "030ccb35-8e0b-4c13-a0a1-9a6347ad8849",
  "snippetResult": [
    "theTime",
    "theTimeHalved"
  ],
  "variables": [
    {
      "name": "theTime",
      "type": "java.lang.Long",
      "object": 1577394471130
    },
    {
      "name": "theTimeHalved",
      "type": "java.lang.Long",
      "object": 788697235565
    }
  ]
}
```

| Key           | Value      | Description                                                  |
| ------------- | ---------- | ------------------------------------------------------------ |
| callbackState | String     | An untrimmed UUID (Set in the code) to identify the callback. This is separate from the standard `state` property. |
| snippetResult | String[]   | A list of variable names passed to the callback, sent by the client |
| variables     | Variable[] | A list of all variables listed in the snippetResult. The Variable object is outlined in the following 3 properties. |
| name          | String     | The name of the variable                                     |
| type          | String     | The canonical Java class name of the object                  |
| object        | Object     | The Gson-serialized Java object. This is not always small or easy to manage, so it is important to limit variables and general use of this request. |

The code used in order to create callbacks like these are in the following format:

```java
// callback UUID variable1 variable2 variable3...
```

This comment will be converted into the proper code that will be executed on the same line as the comment. The following code (Sent by the [CodeExecutionRequest](#CodeExecutionRequest-9)) was used to produce the above request. Due to the `Thread.sleep`, the callback is sent after 5 seconds with the local variables `theTime` and `theTimeHalved` sent over as well.

```java
CompletableFuture.runAsync(() -> {
    try {
        Thread.sleep(5000);
    } catch (InterruptedException ignored) {}
    long theTime = System.currentTimeMillis();
    long theTimeHalved = System.currentTimeMillis() / 2;
    // callback 030ccb35-8e0b-4c13-a0a1-9a6347ad8849 theTime theTimeHalved
});
```
