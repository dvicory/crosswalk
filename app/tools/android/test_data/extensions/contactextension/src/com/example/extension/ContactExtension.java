// Copyright (c) 2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.extension;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.RawContacts.Data;
import android.util.Log;
import android.view.Menu;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.xwalk.core.extension.XWalkExternalExtension;
import org.xwalk.core.extension.XWalkExtensionContextClient;

public class ContactExtension extends XWalkExternalExtension {
    final private static String TAG = "ExternalContactExtension";
    final private XWalkExtensionContextClient mContext;

    public ContactExtension(String name, String JsApi, XWalkExtensionContextClient context) {
        super(name, JsApi, context);
        mContext = context;
    }

    @Override
    public void onMessage(int instanceID, String message){
        try {
            JSONObject m = new JSONObject(message);
            String cmd = m.getString("cmd");
            String name = m.getString("name");
            if (cmd.equals("read")) {
                String storedPhone = "";
                try {
                    storedPhone = readContact(name);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to read phone number for name=" + name);
                    Log.e(TAG, e.toString());
                    e.printStackTrace();
                }
                postMessage(instanceID, storedPhone);

            } else if (cmd.equals("write")) {
                String phone = "";
                try {
                    phone = m.getString("phone");
                    writeContact(name, phone);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to write contact, name=" + name + ", phone=" + phone);
                    Log.e(TAG, e.toString());
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "Unsupported command: " + cmd);
            }
        } catch(Exception e) {
            Log.e(TAG, "Invalid message: " + message);
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return;
        }
    }

    String readContact(String name) {
        Activity cActivity = mContext.getActivity();
        Cursor nameCursor = null;
        Cursor phoneCursor = null;
        String phoneNumber = "";
        try {
            // Find the contacts by name.
            nameCursor = cActivity.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    null, ContactsContract.Contacts.DISPLAY_NAME + "='" + name + "'", null, null);
            if (nameCursor != null && nameCursor.getCount() >= 1) {
                // Get the first phone of the specified name.
                phoneCursor = cActivity.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.Contacts.DISPLAY_NAME + "='" + name + "'", null, null);
                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
            }
        } finally {
            if (phoneCursor != null) phoneCursor.close();
            if (nameCursor != null) nameCursor.close();
        }
        return phoneNumber;
    }

    void writeContact(String name, String phone) {
        //get Android context
        Activity cActivity = mContext.getActivity();
        ContentValues values = new ContentValues();

        Uri rawContactUri = cActivity.getContentResolver().insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        values.put(Data.RAW_CONTACT_ID, rawContactId);

        values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);

        values.put(StructuredName.GIVEN_NAME, name);

        cActivity.getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
        values.clear();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);

        values.put(Phone.NUMBER, phone);

        values.put(Phone.TYPE, Phone.TYPE_MOBILE);

        cActivity.getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
    }
}
