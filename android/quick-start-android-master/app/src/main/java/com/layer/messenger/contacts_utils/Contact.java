package com.layer.messenger.contacts_utils;

public class Contact {
    private String contactId;
    private String displayName;
    private String photoId;

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getContactId() {
        return contactId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getPhotoId() {
        return photoId;
    }
}