/*
 *   Copyright (c) 2014-2015 Luis M. Gallardo D.
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the GNU Lesser General Public License v3.0
 *   which accompanies this distribution, and is available at
 *   http://www.gnu.org/licenses/lgpl.html
 *
 */
package com.lgallardo.qbittorrentclient;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lgallardo.qbittorrentclient.MainActivity.cookie;
import static com.lgallardo.qbittorrentclient.MainActivity.hostname;
import static com.lgallardo.qbittorrentclient.MainActivity.keystore_password;
import static com.lgallardo.qbittorrentclient.MainActivity.keystore_path;
import static com.lgallardo.qbittorrentclient.MainActivity.port;
import static com.lgallardo.qbittorrentclient.MainActivity.protocol;
import static com.lgallardo.qbittorrentclient.MainActivity.subfolder;


public class TorrentDetailsFragment extends Fragment {

    // TAGS
    protected static final String TAG_SAVE_PATH = "save_path";
    protected static final String TAG_CREATION_DATE = "creation_date";
    protected static final String TAG_COMMENT = "comment";
    protected static final String TAG_TOTAL_WASTED = "total_wasted";
    protected static final String TAG_TOTAL_UPLOADED = "total_uploaded";
    protected static final String TAG_TOTAL_DOWNLOADED = "total_downloaded";
    protected static final String TAG_TIME_ELAPSED = "time_elapsed";
    protected static final String TAG_NB_CONNECTIONS = "nb_connections";
    protected static final String TAG_SHARE_RATIO = "share_ratio";
    protected static final String TAG_UPLOAD_LIMIT = "up_limit";
    protected static final String TAG_DOWNLOAD_LIMIT = "dl_limit";

    // TODO: Delete trackers
    static ArrayList<GeneralInfoItem> generalInfoItems;


    // Recycler view for files and trackers
    public static ContentFilesRecyclerViewAdapter rAdapter;
    public static TrackersRecyclerViewAdapter trackerAdapter;
    public static GeneralInfoRecyclerViewAdapter generalInfoAdapter;
    protected RecyclerView mRecyclerView;
    protected RecyclerView mRecyclerViewTrackers;
    protected RecyclerView mRecyclerViewGeneralInfo;

    private RecyclerView.LayoutManager mLayoutManager;            // Declaring Layout Manager as a linear layout manager
    private RecyclerView.LayoutManager mLayoutManagerTrackers;            // Declaring Layout Manager as a linear layout manager
    private RecyclerView.LayoutManager mLayoutManagerGeneralInfo;            // Declaring Layout Manager as a linear layout manager


    // Torrent variables
    String name, info, hash, ratio, size, progressInfo = "", state, leechs, seeds, priority, savePath, creationDate, comment, totalWasted, totalUploaded,
            totalDownloaded, timeElapsed, nbConnections, shareRatio, uploadRateLimit, downloadRateLimit, downloaded, eta, downloadSpeed, uploadSpeed,
            category;

    double progress;

    long addedOn, completionOn;

    static String hashToUpdate;

    String url;
    int position;

    private Torrent torrent;
    public static SwipeRefreshLayout mSwipeRefreshLayout;
    private RefreshListener refreshListener;

    // AdView for ads
    private AdView adView;

    public static int fileContentRowPosition;

    public TorrentDetailsFragment() {
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Tell the host activity that your fragment has menu options that it
        // wants to add/replace/delete using the onCreateOptionsMenu method.
        setHasOptionsMenu(true);

        View rootView;

        rootView = inflater.inflate(R.layout.torrent_details, container, false);
//        // TODO: delete old layout
//        rootView = inflater.inflate(R.layout.torrent_details_old, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.RecyclerViewContentFiles); // Assigning the RecyclerView Object to the xml View
        rAdapter = new ContentFilesRecyclerViewAdapter((MainActivity) getActivity(), getActivity(), new ArrayList<TorrentDetailsItem>());
        rAdapter.notifyDataSetChanged();

        mRecyclerViewTrackers = (RecyclerView) rootView.findViewById(R.id.RecyclerViewTrackers); // Assigning the RecyclerView Object to the xml View
        trackerAdapter = new TrackersRecyclerViewAdapter((MainActivity) getActivity(), getActivity(), new ArrayList<TorrentDetailsItem>());
        trackerAdapter.notifyDataSetChanged();

        mRecyclerViewGeneralInfo = (RecyclerView) rootView.findViewById(R.id.RecyclerViewGeneralInfo); // Assigning the RecyclerView Object to the xml View
        generalInfoAdapter = new GeneralInfoRecyclerViewAdapter((MainActivity) getActivity(), getActivity(), new ArrayList<GeneralInfoItem>());
        generalInfoAdapter.notifyDataSetChanged();

        if (mRecyclerView == null) {
            Log.d("Debug", "mRecyclerView is null");
        }

        if (rAdapter == null) {
            Log.d("Debug", "rAdapter is null");
        }

        try {
            mRecyclerView.setAdapter(rAdapter);
            mRecyclerViewTrackers.setAdapter(trackerAdapter);
            mRecyclerViewGeneralInfo.setAdapter(generalInfoAdapter);

            // Creating a layout Managers
            mLayoutManager = new LinearLayoutManager(rootView.getContext());
            mLayoutManagerTrackers = new LinearLayoutManager(rootView.getContext());
            mLayoutManagerGeneralInfo = new LinearLayoutManager(rootView.getContext());

            // Setting Layout Managers
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerViewTrackers.setLayoutManager(mLayoutManagerTrackers);
            mRecyclerViewGeneralInfo.setLayoutManager(mLayoutManagerGeneralInfo);

            // This is needed to set the ContextMenu
            registerForContextMenu(mRecyclerView);

        } catch (Exception e) {
            Log.e("Debug", e.toString());
        }

        // Get Refresh Listener
        refreshListener = (RefreshListener) getActivity();
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.details_refresh_layout);

        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refreshListener.swipeRefresh();
                }
            });
        }

        // Hide herderInfo and title in phone's view
        if (getActivity().findViewById(R.id.one_frame) != null && MainActivity.headerInfo != null) {
            MainActivity.headerInfo.setVisibility(View.GONE);

            ((MainActivity) getActivity()).setTitle("");
        }

        savePath = "";
        creationDate = "";
        comment = "";
        uploadRateLimit = "";
        downloadRateLimit = "";
        totalWasted = "";
        totalUploaded = "";
        totalDownloaded = "";
        timeElapsed = "";
        nbConnections = "";
        shareRatio = "";

        try {

            if (savedInstanceState != null) {

                // Get saved values
                name = savedInstanceState.getString("torrentDetailName", "");
                size = savedInstanceState.getString("torrentDetailSize", "");
                hash = savedInstanceState.getString("torrentDetailHash", "");
                ratio = savedInstanceState.getString("torrentDetailRatio", "");
                state = savedInstanceState.getString("torrentDetailState", "");
                leechs = savedInstanceState.getString("torrentDetailLeechs", "");
                seeds = savedInstanceState.getString("torrentDetailSeeds", "");
                progress = savedInstanceState.getDouble("torrentDetailProgress",0);
                priority = savedInstanceState.getString("torrentDetailPriority", "");
                eta = savedInstanceState.getString("torrentDetailEta", "");
                uploadSpeed = savedInstanceState.getString("torrentDetailUploadSpeed", "");
                downloadSpeed = savedInstanceState.getString("torrentDetailDownloadSpeed", "");
                downloaded = savedInstanceState.getString("torrentDetailDownloaded", "");
                addedOn = savedInstanceState.getLong("torrentDetailsAddedOn", 0);
                completionOn = savedInstanceState.getLong("torrentDetailsCompletionOn", 0);
                category = savedInstanceState.getString("torrentDetailsCategory", "");
                hashToUpdate = hash;

            } else {

                // Get values from current activity
                name = this.torrent.getName();
                size = Common.calculateSize(this.torrent.getSize());
                hash = this.torrent.getHash();
                ratio = Common.RatioForUi(this.torrent.getRatio());
                state = this.torrent.getState();
                leechs = "" + torrent.getNum_leechs();
                seeds = "" + torrent.getNum_seeds();
                progress = this.torrent.getProgress();
                progressInfo = Common.ProgressForUiTruncated(progress) + "%";
                priority = "" + this.torrent.getPriority();
                eta = Common.secondsToEta(this.torrent.getEta());
                uploadSpeed = Common.calculateSize(this.torrent.getUpspeed()) + "/s";
                downloadSpeed = Common.calculateSize(this.torrent.getDlspeed()) + "/s";
                downloaded = Common.calculateSize(this.torrent.getSize() - this.torrent.getAmount_left());
                addedOn = this.torrent.getAdded_on();
                completionOn = this.torrent.getCompletion_on();
                category = this.torrent.getCategory();

//                Log.d("Debug", "[TorrentDetailsFragment] addedOn: " + addedOn);
//                Log.d("Debug", "[TorrentDetailsFragment] addedOn: " + Common.unixTimestampToDate(addedOn));
//                Log.d("Debug", "[TorrentDetailsFragment] addedOn: " + Common.timestampToDate(addedOn));
//
//                Log.d("Debug", "[TorrentDetailsFragment] completionOn: " + completionOn);
//                Log.d("Debug", "[TorrentDetailsFragment] completionOn: " + Common.unixTimestampToDate(completionOn));
//                Log.d("Debug", "[TorrentDetailsFragment] completionOn: " + Common.timestampToDate(completionOn));


                hashToUpdate = hash;

            }

            TextView nameTextView = (TextView) rootView.findViewById(R.id.torrentName);
            TextView sizeTextView = (TextView) rootView.findViewById(R.id.torrentSize);
            TextView ratioTextView = (TextView) rootView.findViewById(R.id.torrentRatio);
            TextView progressTextView = (TextView) rootView.findViewById(R.id.torrentProgress);
            TextView stateTextView = (TextView) rootView.findViewById(R.id.torrentState);
            TextView priorityTextView = (TextView) rootView.findViewById(R.id.torrentPriority);
            TextView leechsTextView = (TextView) rootView.findViewById(R.id.torrentLeechs);
            TextView seedsTextView = (TextView) rootView.findViewById(R.id.torrentSeeds);
            TextView hashTextView = (TextView) rootView.findViewById(R.id.torrentHash);
            TextView etaTextView = (TextView) rootView.findViewById(R.id.torrentEta);
            TextView uploadSpeedTextView = (TextView) rootView.findViewById(R.id.torrentUploadSpeed);
            TextView downloadSpeedTextView = (TextView) rootView.findViewById(R.id.torrentDownloadSpeed);

            CheckBox sequentialDownloadCheckBox;
            CheckBox firstLAstPiecePrioCheckBox;

            nameTextView.setText(name);
            ratioTextView.setText(ratio);
            stateTextView.setText(state);
            leechsTextView.setText(leechs);
            seedsTextView.setText(seeds);
            progressTextView.setText(progressInfo);
            hashTextView.setText(hash);
            etaTextView.setText(eta);
            priorityTextView.setText(priority);

            sequentialDownloadCheckBox = (CheckBox) rootView.findViewById(R.id.torrentSequentialDownload);
            firstLAstPiecePrioCheckBox = (CheckBox) rootView.findViewById(R.id.torrentFirstLastPiecePrio);

            sequentialDownloadCheckBox.setChecked(this.torrent.isSeq_dl());
            firstLAstPiecePrioCheckBox.setChecked(this.torrent.isF_l_piece_prio());

            TextView addedOnTextView = (TextView) rootView.findViewById(R.id.torrentAddedOn);
            TextView completionOnTextView = (TextView) rootView.findViewById(R.id.torrentCompletionOn);
            TextView categoryTextView = (TextView) rootView.findViewById(R.id.torrentCategory);

            if (addedOn != 10800) {
                // Not fully added yet
                addedOnTextView.setText(Common.timestampToDate(addedOn));
            } else {
                addedOnTextView.setText("");
            }

            if (completionOn != 10800) {
                // Not completed yet
                completionOnTextView.setText(Common.timestampToDate(completionOn));
            } else {
                completionOnTextView.setText("");
            }

            if (category != null && !(category.equals("null"))) {
                categoryTextView.setText(category);
            } else {
                categoryTextView.setText("");
            }


            // Set Downloaded vs Total size
            sizeTextView.setText(downloaded + " / " + size);

            // Only for Pro version
            if (MainActivity.packageName.equals("com.lgallardo.qbittorrentclientpro")) {
                downloadSpeedTextView.setText(Character.toString('\u2193') + " " + downloadSpeed);
                uploadSpeedTextView.setText(Character.toString('\u2191') + " " + uploadSpeed);

                // Set progress bar
                ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar1);
                TextView percentageTV = (TextView) rootView.findViewById(R.id.percentage);

                progressBar.setProgress((int) (progress * 100));
                percentageTV.setText(progressInfo);
            } else {
                downloadSpeedTextView.setText(downloadSpeed);
                uploadSpeedTextView.setText(uploadSpeed);
            }

            nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_recheck, 0, 0, 0);

            // Set status icon
            if ("pausedUP".equals(state) || "pausedDL".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.paused, 0, 0, 0);
            }

            if ("stalledUP".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stalledup, 0, 0, 0);
            }

            if ("stalledDL".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stalleddl, 0, 0, 0);
            }

            if ("downloading".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.downloading, 0, 0, 0);
            }

            if ("uploading".equals(state) || "forcedUP".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.uploading, 0, 0, 0);
            }

            if ("queuedDL".equals(state) || "queuedUP".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.queued, 0, 0, 0);
            }

            if ("checkingDL".equals(state) || "checkingUP".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_recheck, 0, 0, 0);
            }

            if ("error".equals(state) || "missingFiles".equals(state) || "unknown".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.error, 0, 0, 0);
            }


            // Get Content files
            getTorrentContents();

            // Get trackers
            getTorrentTrackers();

            // Get General info labels
            generalInfoItems = new ArrayList<GeneralInfoItem>();

            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_save_path), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_created_date), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_comment), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_total_wasted), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_total_uploaded), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_total_downloaded), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_time_elapsed), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_num_connections), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_share_ratio), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_upload_rate_limit), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_download_rate_limit), null, GeneralInfoItem.GENERALINFO, "generalInfo"));

            // Get general info in background
            getTorrentGeneralInfo();


        } catch (Exception e) {
            Log.e("Debug", "[TorrentDetailsFragment] onCreateView: " + e.toString());
        }

        if (MainActivity.packageName.equals("com.lgallardo.qbittorrentclient")) {
            // Load banner
            loadBanner();
        }

        return rootView;
    }

    // Volley singletons
    protected void addVolleyRequest(JsonObjectRequest jsArrayRequest) {
        VolleySingleton.getInstance(getActivity().getApplication()).addToRequestQueueHttps(jsArrayRequest, keystore_path, keystore_password);
    }

    protected void addVolleyRequest(JsonArrayRequest jsArrayRequest) {
        VolleySingleton.getInstance(getActivity().getApplication()).addToRequestQueueHttps(jsArrayRequest, keystore_path, keystore_password);
    }

    // Volley methods
    private List getTorrentContents(final ContentsListCallback callback) {

        final List<ContentFile> contentFiles = new ArrayList<>();

        String url = MainActivity.buildURL();

        // Command

        url = url + "/api/v2/torrents/files?hash=" + hash;

        JsonArrayRequest jsArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        // Get list type to parse it
                        Type listType = new TypeToken<List<ContentFile>>() {
                        }.getType();

                        // Parse Lists using Gson
                        contentFiles.addAll((List<ContentFile>) new Gson().fromJson(response.toString(), listType));

                        // Return value
                        callback.onSuccess(contentFiles);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        // Log status code
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null) {
                            Log.d("Debug", "getTorrentContents - statusCode: " + networkResponse.statusCode);
                        }

                        // Log error
                        Log.d("Debug", "getTorrentContents - Error in JSON response: " + error.getMessage());

                    }
                }

        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("User-Agent", "qBittorrent for Android");
                //params.put("Host", hostname + ":" + port);
                params.put("Referer", protocol + "://" + hostname + (port != -1 ? ":" + port: ""));
                params.put("Content-Type", "application/x-www-form-urlencoded");
                params.put("Cookie", cookie);
                return params;
            }
        };

        // Add request to te queue
        addVolleyRequest(jsArrayRequest);

        // Return the lists
        return contentFiles;
    }

    private List getTorrentTrackers(final TrackersListCallback callback) {

        final List<Tracker> trackers = new ArrayList<>();

        String url = MainActivity.buildURL();

        // Command
        url = url + "/api/v2/torrents/trackers?hash=" + hash;


        JsonArrayRequest jsArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        // Get list type to parse itgetTorrentTrackers
                        Type listType = new TypeToken<List<Tracker>>() {
                        }.getType();

                        // Parse Lists using Gson
                        trackers.addAll((List<Tracker>) new Gson().fromJson(response.toString(), listType));

                        // Return value
                        callback.onSuccess(trackers);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        // Log status code
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null) {
                            Log.d("Debug", "getTorrentTrackers - statusCode: " + networkResponse.statusCode);
                        }

                        // Log error
                        Log.d("Debug", "getTorrentTrackers - Error in JSON response: " + error.getMessage());
                    }
                }

        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("User-Agent", "qBittorrent for Android");
                //params.put("Host", hostname + ":" + port);
                params.put("Referer", protocol + "://" + hostname + ( port != -1 ? ":" + port : "" ));
                params.put("Content-Type", "application/x-www-form-urlencoded");
                params.put("Cookie", cookie);
                return params;
            }
        };

        // Add request to te queue
        addVolleyRequest(jsArrayRequest);

        // Return the lists
        return trackers;
    }

    private void getTorrentGeneralInfo(final GeneralInfoCallback callback) {

        final List<GeneralInfoItem> generalInfo = new ArrayList<>();

        String url = MainActivity.buildURL();

        // Command
        url = url + "/api/v2/torrents/properties?hash=" + hash;

        JsonObjectRequest jsArrayRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Return value
                        callback.onSuccess((GeneralInfo) new Gson().fromJson(response.toString(), GeneralInfo.class));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Log status code
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null) {
                            Log.d("Debug", "getTorrentGeneralInfo - statusCode: " + networkResponse.statusCode);
                        }
                        // Log error
                        Log.d("Debug", "getTorrentGeneralInfo - Error in JSON response: " + error.getMessage());
                    }
                }

        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("User-Agent", "qBittorrent for Android");
                //params.put("Host", hostname + ":" + port);
                params.put("Referer", protocol + "://" + hostname + (port != -1 ? ":" + port : ""));
                params.put("Content-Type", "application/x-www-form-urlencoded");
                params.put("Cookie", cookie);
                return params;
            }
        };

        // Add request to te queue
        addVolleyRequest(jsArrayRequest);

    }

    // Volley wrappers
    public void getTorrentContents() {

        getTorrentContents(new ContentsListCallback() {
            @Override
            public void onSuccess(List<ContentFile> contents) {

                ArrayList<TorrentDetailsItem> contentFiles = new ArrayList<TorrentDetailsItem>();

                for (int i = 0; i < contents.size(); i++) {

                    ContentFile item = contents.get(i);

                    // Add ContentFiles
                    contentFiles.add(new TorrentDetailsItem(item.getName(), item.getSize(), item.getProgress(), item.getPriority(), null, TorrentDetailsItem.FILE, "setFilePriority"));
                }

                TorrentDetailsFragment.rAdapter.refreshContentFiles(contentFiles);
            }

        });

    }

    public void getTorrentTrackers() {

        getTorrentTrackers(new TrackersListCallback() {
            @Override
            public void onSuccess(List<Tracker> trackers) {

                ArrayList<TorrentDetailsItem> trackersInfo = new ArrayList<TorrentDetailsItem>();

                for (int i = 0; i < trackers.size(); i++) {

                    Tracker item = trackers.get(i);

                    // Add trackers
                    trackersInfo.add(new TorrentDetailsItem(null, null, null, -1, item.getUrl(), TorrentDetailsItem.TRACKER, "addTracker"));

                }

                TorrentDetailsFragment.trackerAdapter.refreshTrackers(trackersInfo);

            }

        });

    }

    public void getTorrentGeneralInfo() {

        getTorrentGeneralInfo(new GeneralInfoCallback() {
            @Override
            public void onSuccess(GeneralInfo generalInfo) {

                GeneralInfoItem item;

                Log.d("Debug", "[getTorrentGeneralInfo] onSuccess >");

                Log.d("Debug", "[getTorrentGeneralInfo] generalInfo: " + generalInfo.getPeers());

                // Save path
                item = generalInfoItems.get(0);
                item.setValue(generalInfo.getSave_path());
                generalInfoItems.set(0, item);

                Log.d("Debug", "[getTorrentGeneralInfo] Save_path: " + generalInfo.getSave_path());

                // Creation date
                item = generalInfoItems.get(1);
                item.setValue(Common.unixTimestampToDate(Long.toString(generalInfo.getCreation_date())));
                generalInfoItems.set(1, item);

                // Comment
                item = generalInfoItems.get(2);
                item.setValue(generalInfo.getComment());
                generalInfoItems.set(2, item);

                // Total wasted
                item = generalInfoItems.get(3);
                item.setValue(Common.calculateSize(Long.toString(generalInfo.getTotal_wasted())).replace(",", "."));
                generalInfoItems.set(3, item);

                // Total uploaded
                item = generalInfoItems.get(4);
                item.setValue(Common.calculateSize(Long.toString(generalInfo.getTotal_uploaded())).replace(",", "."));
                generalInfoItems.set(4, item);

                // Total downloaded
                item = generalInfoItems.get(5);
                item.setValue(Common.calculateSize(Long.toString(generalInfo.getTotal_downloaded())).replace(",", "."));
                generalInfoItems.set(5, item);

                // Time elapsed
                item = generalInfoItems.get(6);
                item.setValue(Common.secondsToEta(generalInfo.getTime_elapsed()).replace(",", "."));
                generalInfoItems.set(6, item);

                // Number of connections
                item = generalInfoItems.get(7);
                item.setValue(Long.toString(generalInfo.getNb_connections()));
                generalInfoItems.set(7, item);

                // Share ratio
                item = generalInfoItems.get(8);
                // Format ratio
                try {
                    item.setValue(String.format("%.2f", generalInfo.getShare_ratio()).replace(",", "."));
                } catch (Exception e) {
                }
                generalInfoItems.set(8, item);

                // Upload limit
                item = generalInfoItems.get(9);
                item.setValue(Long.toString(generalInfo.getUp_limit()));
                generalInfoItems.set(9, item);

                // Download limit
                item = generalInfoItems.get(10);
                item.setValue(Long.toString(generalInfo.getDl_limit()));
                generalInfoItems.set(10, item);

                // Format Upload and Download limit

                // Upload limit
                item = generalInfoItems.get(9);

                if (!item.getValue().equals("-1")) {
                    item.setValue(Common.calculateSize(item.getValue()).replace(",", ".") + "/s");
                } else {
                    item.setValue("∞");
                }

                generalInfoItems.set(9, item);

                // Download limit
                item = generalInfoItems.get(10);

                if (!item.getValue().equals("-1")) {
                    item.setValue(Common.calculateSize(item.getValue()).replace(",", ".") + "/s");
                } else {
                    item.setValue("∞");
                }

                generalInfoItems.set(10, item);

                // Refresh adapater
                generalInfoAdapter.refreshGeneralInfo(generalInfoItems);

            }

        });
    }
    // end of wraps

    public void updateDetails(Torrent torrent) {

        try {

            // Hide herderInfo in phone's view
            if (getActivity().findViewById(R.id.one_frame) != null) {
                MainActivity.headerInfo.setVisibility(View.GONE);
            }

            // Get values from current activity
            name = torrent.getName();
            size = Common.calculateSize(torrent.getSize());
            hash = torrent.getHash();
            ratio = Common.RatioForUi(this.torrent.getRatio());
            state = torrent.getState();
            leechs = "" + torrent.getNum_leechs();
            seeds = "" + torrent.getNum_seeds();
            progress = torrent.getProgress();
            progressInfo = Common.ProgressForUiTruncated(progress) + "%";
            priority = "" + torrent.getPriority();
            eta = Common.secondsToEta(torrent.getEta());
            uploadSpeed = Common.calculateSize(torrent.getUpspeed()) + "/s";
            downloadSpeed = Common.calculateSize(torrent.getDlspeed()) + "/s";
            downloaded = Common.calculateSize(torrent.getSize() - torrent.getAmount_left());
            addedOn = torrent.getAdded_on();
            completionOn = torrent.getCompletion_on();
            category = torrent.getCategory();


//            Log.d("Debug", "[TorrentDetailsFragment] update - progress: " + progress);
//

            FragmentManager fragmentManager = getFragmentManager();

            TorrentDetailsFragment detailsFragment = null;

            if (getActivity().findViewById(R.id.one_frame) != null) {
                detailsFragment = (TorrentDetailsFragment) fragmentManager.findFragmentByTag("firstFragment");
            } else {
                detailsFragment = (TorrentDetailsFragment) fragmentManager.findFragmentByTag("secondFragment");
            }

            View rootView = detailsFragment.getView();

            TextView nameTextView = (TextView) rootView.findViewById(R.id.torrentName);
            TextView sizeTextView = (TextView) rootView.findViewById(R.id.torrentSize);
            TextView ratioTextView = (TextView) rootView.findViewById(R.id.torrentRatio);
            TextView priorityTextView = (TextView) rootView.findViewById(R.id.torrentPriority);
            TextView stateTextView = (TextView) rootView.findViewById(R.id.torrentState);
            TextView leechsTextView = (TextView) rootView.findViewById(R.id.torrentLeechs);
            TextView seedsTextView = (TextView) rootView.findViewById(R.id.torrentSeeds);
            TextView progressTextView = (TextView) rootView.findViewById(R.id.torrentProgress);
            TextView hashTextView = (TextView) rootView.findViewById(R.id.torrentHash);

            TextView etaTextView = (TextView) rootView.findViewById(R.id.torrentEta);
            TextView uploadSpeedTextView = (TextView) rootView.findViewById(R.id.torrentUploadSpeed);
            TextView downloadSpeedTextView = (TextView) rootView.findViewById(R.id.torrentDownloadSpeed);

            CheckBox sequentialDownloadCheckBox;
            CheckBox firstLAstPiecePrioCheckBox;

            nameTextView.setText(name);
            ratioTextView.setText(ratio);
            stateTextView.setText(state);
            leechsTextView.setText(leechs);
            seedsTextView.setText(seeds);
            progressTextView.setText(progressInfo);
            hashTextView.setText(hash);
            priorityTextView.setText(priority);
            etaTextView.setText(eta);


            sequentialDownloadCheckBox = (CheckBox) rootView.findViewById(R.id.torrentSequentialDownload);
            firstLAstPiecePrioCheckBox = (CheckBox) rootView.findViewById(R.id.torrentFirstLastPiecePrio);

            sequentialDownloadCheckBox.setChecked(this.torrent.isSeq_dl());
            firstLAstPiecePrioCheckBox.setChecked(this.torrent.isF_l_piece_prio());

            TextView addedOnTextView = (TextView) rootView.findViewById(R.id.torrentAddedOn);
            TextView completionOnTextView = (TextView) rootView.findViewById(R.id.torrentCompletionOn);

            TextView categoryTextView = (TextView) rootView.findViewById(R.id.torrentCategory);


            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

            if (addedOn != 10800) {
                // Not fully added yet
                addedOnTextView.setText(Common.timestampToDate(addedOn));
            } else {
                addedOnTextView.setText("");
            }

            if (completionOn != 10800) {
                // Not completed yet
                completionOnTextView.setText(Common.timestampToDate(completionOn));
            } else {
                completionOnTextView.setText("");
            }

            if (category != null && !(category.equals("null"))) {
                categoryTextView.setText(category);
            } else {
                categoryTextView.setText("");
            }



            // Set Downloaded vs Total size
            sizeTextView.setText(downloaded + " / " + size);

            // Only for Pro version
            if (MainActivity.packageName.equals("com.lgallardo.qbittorrentclientpro")) {
                downloadSpeedTextView.setText(Character.toString('\u2193') + " " + downloadSpeed);
                uploadSpeedTextView.setText(Character.toString('\u2191') + " " + uploadSpeed);

                // Set progress bar
                ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar1);
                TextView percentageTV = (TextView) rootView.findViewById(R.id.percentage);

                progressBar.setProgress((int) (progress*100));
                percentageTV.setText(progressInfo);

            } else {
                downloadSpeedTextView.setText(downloadSpeed);
                uploadSpeedTextView.setText(uploadSpeed);
            }


            nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_recheck, 0, 0, 0);

            if ("pausedUP".equals(state) || "pausedDL".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.paused, 0, 0, 0);
            }

            if ("stalledUP".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stalledup, 0, 0, 0);
            }

            if ("stalledDL".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stalleddl, 0, 0, 0);
            }

            if ("downloading".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.downloading, 0, 0, 0);
            }

            if ("uploading".equals(state) || "forcedUP".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.uploading, 0, 0, 0);
            }

            if ("queuedDL".equals(state) || "queuedUP".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.queued, 0, 0, 0);
            }

            if ("checkingDL".equals(state) || "checkingUP".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_recheck, 0, 0, 0);
            }

            if ("error".equals(state) || "missingFiles".equals(state) || "unknown".equals(state)) {
                nameTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.error, 0, 0, 0);
            }


            // Get Content files
            getTorrentContents();

            // Get trackers
            getTorrentTrackers();

            // Get General info labels
            generalInfoItems = new ArrayList<GeneralInfoItem>();

            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_save_path), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_created_date), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_comment), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_total_wasted), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_total_uploaded), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_total_downloaded), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_time_elapsed), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_num_connections), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_share_ratio), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_upload_rate_limit), null, GeneralInfoItem.GENERALINFO, "generalInfo"));
            generalInfoItems.add(new GeneralInfoItem(getString(R.string.torrent_details_download_rate_limit), null, GeneralInfoItem.GENERALINFO, "generalInfo"));

            // Get General info in background;
            getTorrentGeneralInfo();

        } catch (Exception e) {

            Log.e("Debug", "TorrentDetailsFragment - onCreateView: " + e.toString());
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("torrentDetailName", name);
        outState.putString("torrentDetailSize", size);
        outState.putString("torrentDetailHash", hash);
        outState.putString("torrentDetailRatio", ratio);
        outState.putString("torrentDetailState", state);
        outState.putString("torrentDetailLeechs", leechs);
        outState.putString("torrentDetailSeeds", seeds);
        outState.putDouble("torrentDetailProgress", progress);
        outState.putString("torrentDetailPriority", priority);
        outState.putString("torrentDetailEta", eta);
        outState.putString("torrentDetailUploadSpeed", uploadSpeed);
        outState.putString("torrentDetailDownloadSpeed", downloadSpeed);
        outState.putString("torrentDetailDownloaded", downloaded);
        outState.putLong("torrentDetailsAddedOn", addedOn);
        outState.putLong("torrentDetailsCompletionOn", completionOn);

    }

    // @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {

    }

    // @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menu != null) {

            // Disable RSS support
            menu.findItem(R.id.action_rss).setVisible(false);

            menu.findItem(R.id.action_resume_all).setVisible(false);
            menu.findItem(R.id.action_pause_all).setVisible(false);
            menu.findItem(R.id.action_add).setVisible(false);

            if (getActivity().findViewById(R.id.one_frame) != null) {
                menu.findItem(R.id.action_refresh).setVisible(false);
            } else {
                menu.findItem(R.id.action_refresh).setVisible(true);
            }

            if (getActivity().findViewById(R.id.one_frame) != null) {
                menu.findItem(R.id.action_sort_menu).setVisible(false);
            } else {
                menu.findItem(R.id.action_sort_menu).setVisible(true);
            }

            menu.findItem(R.id.action_resume).setVisible(true);
            menu.findItem(R.id.action_pause).setVisible(true);
            menu.findItem(R.id.action_priority_menu).setVisible(true);
            menu.findItem(R.id.action_increase_prio).setVisible(true);
            menu.findItem(R.id.action_decrease_prio).setVisible(true);
            menu.findItem(R.id.action_max_prio).setVisible(true);
            menu.findItem(R.id.action_min_prio).setVisible(true);
            menu.findItem(R.id.action_delete).setVisible(true);
            menu.findItem(R.id.action_delete_drive).setVisible(true);
            menu.findItem(R.id.action_download_rate_limit).setVisible(true);
            menu.findItem(R.id.action_upload_rate_limit).setVisible(true);
            menu.findItem(R.id.action_recheck).setVisible(true);

            if (getActivity().findViewById(R.id.one_frame) != null) {
                menu.findItem(R.id.action_search).setVisible(false);
            } else {
                menu.findItem(R.id.action_search).setVisible(true);
            }

            menu.findItem(R.id.action_first_last_piece_prio).setVisible(true);
            menu.findItem(R.id.action_sequential_download).setVisible(true);
            menu.findItem(R.id.action_toggle_alternative_rate).setVisible(true);
            // TODO: Change add_tracker to true
//                menu.findItem(R.id.action_add_tracker).setVisible(false);

            // TODO: set category/category visible after implement it
            menu.findItem(R.id.action_label_menu).setVisible(false);
            menu.findItem(R.id.action_set_category).setVisible(false);
            menu.findItem(R.id.action_delete_category).setVisible(false);

            // TODO: Check when this changed (qb_api X.Y.Z )
            if (MainActivity.qb_api < 8) {
                menu.findItem(R.id.action_delete_category).setVisible(false);
            }

            // Set Alternate Speed limit state
            if (MainActivity.alternative_speeds) {
                menu.findItem(R.id.action_toggle_alternative_rate).setChecked(true);
            } else {
                menu.findItem(R.id.action_toggle_alternative_rate).setChecked(false);
            }

        }
    }

    // Load Banner method
    public void loadBanner() {

        // Get the adView.
        adView = (AdView) getActivity().findViewById(R.id.adView);

        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad in the background.
        adView.loadAd(adRequest);

    }

    public void setTorrent(Torrent torrent) {
        this.torrent = torrent;
    }


    /**
     * *
     * Method for Setting the Height of the ListView dynamically. Hack to fix
     * the issue of not showing all the items of the ListView when placed inside
     * a ScrollView
     * **
     */
    public static void setListViewHeightBasedOnChildren(ListView listView) {

        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;

        for (int i = 0; i < listAdapter.getCount(); i++) {

            long numOfLines = 1;
            view = listAdapter.getView(i, view, listView);

            if (i == 0) {
                view.setLayoutParams(new LayoutParams(desiredWidth, LayoutParams.WRAP_CONTENT));
            }

            view.measure(desiredWidth, MeasureSpec.UNSPECIFIED);

            TextView file = (TextView) view.findViewById(R.id.name);
            TextView percentage = (TextView) view.findViewById(R.id.percentage);
            ProgressBar progressBar1 = (ProgressBar) view.findViewById(R.id.progressBar1);

            if (view.getMeasuredWidth() > desiredWidth) {

                double viewWidthLong = Double.valueOf(view.getMeasuredWidth());
                double desiredWidthLong = Double.valueOf(desiredWidth);

                numOfLines = Math.round(viewWidthLong / desiredWidthLong) + 1;

                totalHeight += (file.getMeasuredHeight() * numOfLines) + percentage.getMeasuredHeight() + progressBar1.getMeasuredHeight();

            } else {
                totalHeight += view.getMeasuredHeight();
            }

        }

        LayoutParams params = listView.getLayoutParams();

        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));

        listView.setLayoutParams(params);
        listView.requestLayout();

    }

}
