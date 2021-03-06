/*
 * Copyright (c) 2015 Washington State Department of Transportation
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
import gov.wa.wsdot.android.wsdot.shared.BlogItem;
import gov.wa.wsdot.android.wsdot.util.AnalyticsUtils;
import gov.wa.wsdot.android.wsdot.util.ImageManager;
import gov.wa.wsdot.android.wsdot.util.ParserUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import aje.android.sdk.AdError;
import aje.android.sdk.AdJugglerAdView;
import aje.android.sdk.AdListener;
import aje.android.sdk.AdRequest;
import aje.android.sdk.IncorrectAdRequestException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class BlogFragment extends ListFragment implements
        LoaderCallbacks<ArrayList<BlogItem>>,
        SwipeRefreshLayout.OnRefreshListener {

	private static final String TAG = BlogFragment.class.getSimpleName();
	private static ArrayList<BlogItem> blogItems = null;
	private static BlogItemAdapter mAdapter;
	private View mEmptyView;
	private static SwipeRefreshLayout swipeRefreshLayout;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        AnalyticsUtils.getInstance(getActivity()).trackPageView("/News & Social Media/Blog");
	}	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_list_adjuggler_with_swipe_refresh, null);

        // For some reason, if we omit this, NoSaveStateFrameLayout thinks we are
        // FILL_PARENT / WRAP_CONTENT, making the progress bar stick to the top of the activity.
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_container);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorScheme(
                17170451,  // android.R.color.holo_blue_bright 
                17170452,  // android.R.color.holo_green_light 
                17170456,  // android.R.color.holo_orange_light 
                17170454); // android.R.color.holo_red_light)
        
        mEmptyView = root.findViewById( R.id.empty_list_view );
        
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
		
		// Remove the separator between items in the ListView
		getListView().setDivider(null);
		getListView().setDividerHeight(0);
		
		mAdapter = new BlogItemAdapter(getActivity());
		setListAdapter(mAdapter);
		
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.        
        getLoaderManager().initLoader(0, null, this);
	}
	
	public Loader<ArrayList<BlogItem>> onCreateLoader(int id, Bundle args) {
		// This is called when a new Loader needs to be created. There
        // is only one Loader with no arguments, so it is simple
		return new BlogItemsLoader(getActivity());
	}

	public void onLoadFinished(Loader<ArrayList<BlogItem>> loader, ArrayList<BlogItem> data) {

		if (!data.isEmpty()) {
			mAdapter.setData(data);
		} else {
		    TextView t = (TextView) mEmptyView;
			t.setText(R.string.no_connection);
			getListView().setEmptyView(mEmptyView);
		}
		
		swipeRefreshLayout.setRefreshing(false);
	}

	public void onLoaderReset(Loader<ArrayList<BlogItem>> loader) {
	    swipeRefreshLayout.setRefreshing(false);
	    mAdapter.setData(null);
	}
	
	/**
	 * A custom Loader that loads all of the posts from the WSDOT blog.
	 */	
	public static class BlogItemsLoader extends AsyncTaskLoader<ArrayList<BlogItem>> {

		private ArrayList<BlogItem> mItems = null;
		
		public BlogItemsLoader(Context context) {
			super(context);
		}

		@Override
		public ArrayList<BlogItem> loadInBackground() {
			mItems = new ArrayList<BlogItem>();
			BlogItem i = null;
			
			try {
				URL url = new URL("http://wsdotblog.blogspot.com/feeds/posts/default?alt=json&max-results=10");
				URLConnection urlConn = url.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
				String jsonFile = "";
				String line;
				
				while ((line = in.readLine()) != null)
					jsonFile += line;
				in.close();
				
				JSONObject obj = new JSONObject(jsonFile);
				JSONObject data = obj.getJSONObject("feed");			
				JSONArray entries = data.getJSONArray("entry");
				
				int numEntries = entries.length();
				for (int j=0; j < numEntries; j++) {
					JSONObject entry = entries.getJSONObject(j);
					i = new BlogItem();
					i.setTitle(entry.getJSONObject("title").getString("$t"));

	            	try {
	            		i.setPublished(ParserUtils.relativeTime(entry.getJSONObject("published").getString("$t"), "yyyy-MM-dd'T'HH:mm:ss.SSSz", true));
	            	} catch (Exception e) {
	            		i.setPublished("Unavailable");
	            		Log.e(TAG, "Error parsing date", e);
	            	}
					
	            	String content = entry.getJSONObject("content").getString("$t");
					i.setContent(content);
					
					Document doc = Jsoup.parse(content);
					Element imgTable = doc.select("table img").first();
					Element imgDiv = doc.select("div:not(.blogger-post-footer) img").first();
					Element table = doc.select("table").first();
					if (imgTable != null) {
						String imgSrc = imgTable.attr("src");
						i.setImageUrl(imgSrc);
						if (table != null) {
							try {
								String caption = table.text();
								i.setImageCaption(caption);
							} catch (NullPointerException e) {
								// TODO Auto-generated catch block
							}
						}
					} else if (imgDiv != null) {
						String imgSrc = imgDiv.attr("src");
						i.setImageUrl(imgSrc);
					}
					
					String temp = content.replaceFirst("<i>(.*)</i><br /><br />", "");
					temp = temp.replaceFirst("<table(.*?)>.*?</table>", "");
					String tempDoc = Jsoup.parse(temp).text();
					try {
						String description = tempDoc.split("\\.", 2)[0] + ".";
						i.setDescription(description);
					} catch (ArrayIndexOutOfBoundsException e) {
						i.setDescription("");
					}

					i.setLink(entry.getJSONArray("link").getJSONObject(4).getString("href"));
					
					mItems.add(i);
				}

			} catch (Exception e) {
				Log.e(TAG, "Error in network call", e);
			}

			return mItems;
		}
		
		@Override
		public void deliverResult(ArrayList<BlogItem> data) {
		    /**
		     * Called when there is new data to deliver to the client. The
		     * super class will take care of delivering it; the implementation
		     * here just adds a little more logic.
		     */
			blogItems = data;
			
			super.deliverResult(data);
		}

		@Override
		protected void onStartLoading() {
			super.onStartLoading();

			mAdapter.clear();
			swipeRefreshLayout.setRefreshing(true);
			forceLoad();
		}

		@Override
		protected void onStopLoading() {
			super.onStopLoading();
			
	        // Attempt to cancel the current load task if possible.
	        cancelLoad();
		}
		
		@Override
		public void onCanceled(ArrayList<BlogItem> data) {
			super.onCanceled(data);
		}

		@Override
		protected void onReset() {
			super.onReset();
			
	        // Ensure the loader is stopped
	        onStopLoading();
	        
	        if (mItems != null) {
	        	mItems = null;
	        }
		}		
		
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Bundle b = new Bundle();
		Intent intent = new Intent(getActivity(), BlogDetailsActivity.class);
		b.putString("title", blogItems.get(position).getTitle());
		b.putString("content", blogItems.get(position).getContent());
		b.putString("link", blogItems.get(position).getLink());
		intent.putExtras(b);
		
		startActivity(intent);
	}
	
	private class BlogItemAdapter extends ArrayAdapter<BlogItem> {
		private final LayoutInflater mInflater;
		private Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Regular.ttf");
        private Typeface tfb = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Bold.ttf");
        private ImageManager imageManager;
        
        public BlogItemAdapter(Context context) {
	        super(context, R.layout.list_item_with_image);
	        mInflater = LayoutInflater.from(context);
	        imageManager = new ImageManager(getActivity(), 0);
        }
        
        public void setData(ArrayList<BlogItem> data) {
            clear();
            if (data != null) {
                //addAll(data); // Only in API level 11
                notifyDataSetChanged();
                int size = data.size();
                for (int i=0; i < size; i++) {
                	add(data.get(i));
                }
                notifyDataSetChanged();                
            }
        }        

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
	        ViewHolder holder = null;
        	
        	if (convertView == null) {
	            convertView = mInflater.inflate(R.layout.list_item_with_image, null);
	            holder = new ViewHolder();
	            holder.image = (ImageView) convertView.findViewById(R.id.image);
	            holder.caption = (TextView) convertView.findViewById(R.id.caption);
	            holder.title = (TextView) convertView.findViewById(R.id.title);
	            holder.title.setTypeface(tfb);
	            holder.description = (TextView) convertView.findViewById(R.id.description);
	            holder.description.setTypeface(tf);
	            holder.created_at = (TextView) convertView.findViewById(R.id.created_at);
	            holder.created_at.setTypeface(tf);
	            
	            convertView.setTag(holder);
	        } else {
	        	holder = (ViewHolder) convertView.getTag();
	        }
	        
	        BlogItem item = getItem(position);
	        
	        if (item.getImageUrl() == null) {
	        	holder.image.setVisibility(View.GONE);
	        	holder.caption.setVisibility(View.GONE);
	        } else {
	        	holder.image.setVisibility(View.VISIBLE);
	        	holder.image.setTag(item.getImageUrl());
	        	imageManager.displayImage(item.getImageUrl(), getActivity(), holder.image);
	        	if (item.getImageCaption() == null) {
	        		holder.caption.setVisibility(View.GONE);
	        	} else {
	        		holder.caption.setVisibility(View.VISIBLE);
	        		holder.caption.setText(item.getImageCaption().toUpperCase());
	        	}
	        }	        
	        
           	holder.title.setText(item.getTitle());
           	holder.description.setText(item.getDescription());
       		holder.created_at.setText(item.getPublished());
	        
	        return convertView;
        }
	}
	
	public static class ViewHolder {
		public ImageView image;
		public TextView caption;
		public TextView title;
		public TextView description;
		public TextView created_at;
	}

    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        getLoaderManager().restartLoader(0, null, this);        
    }
	
}
