/*
 * Copyright (c) 2014 Washington State Department of Transportation
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package gov.wa.wsdot.android.wsdot.ui;

import gov.wa.wsdot.android.wsdot.R;
import gov.wa.wsdot.android.wsdot.util.AnalyticsUtils;

import java.util.ArrayList;

import aje.android.sdk.AdError;
import aje.android.sdk.AdJugglerAdView;
import aje.android.sdk.AdListener;
import aje.android.sdk.AdRequest;
import aje.android.sdk.IncorrectAdRequestException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FerriesFragment extends ListFragment {
    
    private static final String TAG = FerriesFragment.class.getSimpleName();
    private ArrayList<ListViewItem> listViewItems;
    private ListViewArrayAdapter mAdapter;
    private View mLoadingSpinner;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        // Tell the framework to try to keep this fragment around
        // during a configuration change.
        setRetainInstance(true);
       
        AnalyticsUtils.getInstance(getActivity()).trackPageView("/Ferries");
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_list_with_spinner, null);

        // For some reason, if we omit this, NoSaveStateFrameLayout thinks we are
        // FILL_PARENT / WRAP_CONTENT, making the progress bar stick to the top of the activity.
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Since we are building a simple navigation, just hide the spinner.
        mLoadingSpinner = root.findViewById(R.id.loading_spinner);
        mLoadingSpinner.setVisibility(View.GONE);

        final AdJugglerAdView mAdJugglerAdView = (AdJugglerAdView) root.findViewById(R.id.ajAdView);
        mAdJugglerAdView.setListener(new AdListener() {

            public boolean onClickAd(String arg0) {
                return false;
            }

            public void onExpand() {
            }

            public void onExpandClose() {
            }

            public void onFailedToClickAd(String arg0, String arg1) {
            }

            public void onFailedToFetchAd(AdError arg0, String arg1) {
            }

            public void onFetchAdFinished() {
            }

            public void onFetchAdStarted() {
            }

            public void onResize() {
            }

            public void onResizeClose() {
            }
        });
        
        try {
            AdRequest adRequest = new AdRequest();
            adRequest.setServer(getString(R.string.adRequest_server));
            adRequest.setZone(getString(R.string.adRequest_zone));
            adRequest.setAdSpot(getString(R.string.adRequest_adspot));
            mAdJugglerAdView.showAd(adRequest);
        } catch (IncorrectAdRequestException e) {
            Log.e(TAG, "Error showing banner ad", e);
        }
        
        return root;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        listViewItems = new ArrayList<ListViewItem>();
        listViewItems.add(new ListViewItem("Route Schedules", FerriesRouteSchedulesActivity.class));
        listViewItems.add(new ListViewItem("Vehicle Reservations", "http://www.wsdot.wa.gov/ferries/reservations"));
        listViewItems.add(new ListViewItem("Vessel Watch", VesselWatchMapActivity.class));
        
        mAdapter = new ListViewArrayAdapter(getActivity(), R.layout.list_item, listViewItems);
        setListAdapter(mAdapter);
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent intent = new Intent();
        
        if (listViewItems.get(position).getClz() instanceof Class<?>) {
            intent.setClass(getActivity(), (Class<?>) listViewItems.get(position).getClz());
        } else {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(listViewItems.get(position).getUrl()));
        }
        
        startActivity(intent);
    }
    
    private class ListViewArrayAdapter extends ArrayAdapter<ListViewItem> {
        private final LayoutInflater mInflater;
        private ArrayList<ListViewItem> items;
        private Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Regular.ttf");
        
        public ListViewArrayAdapter(Context context, int detailsItem, ArrayList<ListViewItem> items) {
            super(context, detailsItem, items);
            this.items = items;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item, null);
            }
            
            ListViewItem item = items.get(position);
            
            if (item != null) {
                TextView tt = (TextView) convertView.findViewById(R.id.title);
                tt.setTypeface(tf);
                
                if (tt != null) {
                    tt.setText(item.getTitle());
                }
            }
            
            return convertView;
        }
    }
    
    public static class ViewHolder {
        public TextView tt;
    }       
    
    public class ListViewItem {
        private String title;
        private Object clz;
        private String url;
        
        public ListViewItem(String title, Class<?> clz) {
            this.title = title;
            this.clz = clz;
        }
        public ListViewItem(String title, String url) {
            this.title = title;
            this.url = url;
        }
        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
        public Object getClz() {
            return clz;
        }
        public void setClz(Object clz) {
            this.clz = clz;
        }
        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
    }

}
