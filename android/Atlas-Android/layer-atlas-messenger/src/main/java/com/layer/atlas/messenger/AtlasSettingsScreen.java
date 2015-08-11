/*
 * Copyright (c) 2015 Layer. All rights reserved.
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
package com.layer.atlas.messenger;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

public class AtlasSettingsScreen extends AppCompatActivity {

    public static String PARSE_VERSION = "1.9.2";
    public static String LAYER_VERSION = "0.14.0";
    public static String ATLAS_VERSION = "0.1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyPreferenceFragment preferenceFragment = new MyPreferenceFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, preferenceFragment).commit();


        Preference layerVersionPreference = preferenceFragment.findFragmentPreference("layerVersion");
        layerVersionPreference.setSummary(LAYER_VERSION);

        Preference atlasVersionPreference = preferenceFragment.findFragmentPreference("atlasVersion");
        atlasVersionPreference.setSummary(ATLAS_VERSION);

        Preference parseVersionPreference = preferenceFragment.findFragmentPreference("parseVersion");
        parseVersionPreference.setSummary(PARSE_VERSION);

        Preference appVersionPreference = preferenceFragment.findFragmentPreference("appVersion");
        String versionName = BuildConfig.VERSION_NAME;
        appVersionPreference.setSummary(versionName);
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        public Preference findFragmentPreference(CharSequence key) {
            getFragmentManager().executePendingTransactions();
            return findPreference(key);
        }
    }
}
