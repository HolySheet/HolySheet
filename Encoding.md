# Encoding

In order to store a high amount of data in google docs, there is a special encryption step required. This is currently a work-in-progress, and is designed to store the absolute most amount of data. This document is more of a reference for me at the moment, and will be cleaned up later on.

The current theorized amount of data stored per Google Docs character is 64 bytes

The data storage technique is:

| Bytes | Representation     |
| ----- | ------------------ |
| 1     | Bold Toggle        |
| 1     | Italics Toggle     |
| 1     | Underline Toggle   |
| 5     | Font (32 fonts)    |
| 6     | Character (mapped) |
| 8     | Text Size          |
| 24    | Text Color         |
| 24    | Highlight Color    |

