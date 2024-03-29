/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.supportv4.app;

import java.lang.reflect.Field;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;

/**
 * Demonstration of the use of a CursorLoader to load and display contacts
 * data in a fragment.
 */
public class LoaderCursorSupport extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
            CursorLoaderListFragment list = new CursorLoaderListFragment();
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
    }


    public static class CursorLoaderListFragment extends ListFragment
            implements LoaderManager.LoaderCallbacks<Cursor> {

        // This is the Adapter being used to display the list's data.
        SimpleCursorAdapter mAdapter;

        // If non-null, this is the current filter the user has provided.
        String mCurFilter;

        @Override public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Give some text to display if there is no data.  In a real
            // application this would come from a resource.
            setEmptyText("No phone numbers");

            // We have a menu item to show in action bar.
            setHasOptionsMenu(true);

            // Create an empty adapter we will use to display the loaded data.
            mAdapter = new SimpleCursorAdapter(
                           getActivity(),
                           android.R.layout.simple_list_item_2, null,
                           (
                               (Build.VERSION.SDK_INT < 5) ?
                                   new String[] { Contacts.DISPLAY_NAME } :
                                   new String[] { Contacts.DISPLAY_NAME, Contacts.CONTACT_STATUS }
                           ),
                           (
                               (Build.VERSION.SDK_INT < 5) ?
                                   new int[] { android.R.id.text1 } :
                                   new int[] { android.R.id.text1, android.R.id.text2 }
                           ), 0);
            setListAdapter(mAdapter);

            // Start out with a progress indicator.
            setListShown(false);

            // Prepare the loader.  Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(0, null, this);
        }

        @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Place an action bar item for searching.
            //MenuItem item = menu.add("Search");
            //item.setIcon(android.R.drawable.ic_menu_search);
            //item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            //SearchView sv = new SearchView(getActivity());
            //sv.setOnQueryTextListener(this);
            //item.setActionView(sv);
        }

        public boolean onQueryTextChange(String newText) {
            // Called when the action bar search text has changed.  Update
            // the search filter, and restart the loader to do a new query
            // with this filter.
            mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
            getLoaderManager().restartLoader(0, null, this);
            return true;
        }

        @Override public void onListItemClick(ListView l, View v, int position, long id) {
            // Insert desired behavior here.
            Log.i("FragmentComplexList", "Item clicked: " + id);
        }

        private static final boolean IS_API_LEVEL_4 = (Build.VERSION.SDK_INT < 5);
        // These are the Contacts rows that we will retrieve.
        static final String[] CONTACTS_SUMMARY_PROJECTION = 
            IS_API_LEVEL_4 ?
                new String[] {
                    Contacts._ID,
                    Contacts.DISPLAY_NAME,
                } :
                new String [] {
                    Contacts._ID,
                    Contacts.DISPLAY_NAME,
                    Contacts.CONTACT_STATUS,
                    Contacts.CONTACT_PRESENCE,
                    Contacts.PHOTO_ID,
                    Contacts.LOOKUP_KEY,
                };

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            // This is called when a new Loader needs to be created.  This
            // sample only has one Loader, so we don't care about the ID.
            // First, pick the base URI to use depending on whether we are
            // currently filtering.
            Uri baseUri;
            if (mCurFilter != null) {
                Uri CONTENT_FILTER_URI = null;
                if (IS_API_LEVEL_4) {
                    CONTENT_FILTER_URI = android.provider.Contacts.Phones.CONTENT_FILTER_URL;
                } else {
                    CONTENT_FILTER_URI = Uri.parse("content://com.android.contacts/phone_lookup");
                }
                
                baseUri = Uri.withAppendedPath(/*Contacts.CONTENT_FILTER_URI*/
                        CONTENT_FILTER_URI,
                        Uri.encode(mCurFilter));
            } else {
                Uri CONTENT_URI = null;
                if (IS_API_LEVEL_4) {
                    CONTENT_URI = android.provider.Contacts.Phones.CONTENT_URI;
                } else {
                    CONTENT_URI = Uri.parse("content://com.android.contacts/data/phones");
                }
                baseUri = /* Contacts.CONTENT_URI */ CONTENT_URI;
            }

            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            String select = "((" + Contacts.DISPLAY_NAME + " NOTNULL) AND ("
                     /* + Contacts.HAS_PHONE_NUMBER + "=1) AND (" */
                    + Contacts.DISPLAY_NAME + " != '' ))";
            return new CursorLoader(getActivity(), baseUri,
                    CONTACTS_SUMMARY_PROJECTION, select, null,
                    Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            // Swap the new cursor in.  (The framework will take care of closing the
            // old cursor once we return.)
            mAdapter.swapCursor(data);

            // The list should now be shown.
            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
        }

        public void onLoaderReset(Loader<Cursor> loader) {
            // This is called when the last Cursor provided to onLoadFinished()
            // above is about to be closed.  We need to make sure we are no
            // longer using it.
            mAdapter.swapCursor(null);
        }
    }

}
