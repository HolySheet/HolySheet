package com.uddernetworks.drivestore;

public enum Mime {
    AUDIO("application/vnd.google-apps.audio"), //
    DOCUMENT("application/vnd.google-apps.document"), //		Google Docs
    DRAWING("application/vnd.google-apps.drawing"), //			Google Drawing
    FILE("application/vnd.google-apps.file"), //				Google Drive file
    FOLDER("application/vnd.google-apps.folder"), //			Google Drive folder
    FORM("application/vnd.google-apps.form"), //				Google Forms
    FUSIONTABLE("application/vnd.google-apps.fusiontable"), //	Google Fusion Tables
    MAP("application/vnd.google-apps.map"), //					Google My Maps
    PHOTO("application/vnd.google-apps.photo"),
    SLIDE("application/vnd.google-apps.presentation"), //		Google Slides
    SCRIPT("application/vnd.google-apps.script"), //			Google Apps Scripts
    SITE("application/vnd.google-apps.site"), //				Google Sites
    SHEET("application/vnd.google-apps.spreadsheet"), //		Google Sheets
    UNKNOWN("application/vnd.google-apps.unknown"),
    VIDEO("application/vnd.google-apps.video"),
    SHORTCUT("application/vnd.google-apps.drive-sdk"); //		3rd party shortcut

    private String mime;

    Mime(String mime) {
        this.mime = mime;
    }

    public String getMime() {
        return mime;
    }
}
