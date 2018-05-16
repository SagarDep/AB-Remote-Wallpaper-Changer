package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.List;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.UtilityFun;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryItem;
import in.thetechguru.walle.remote.abremotewallpaperchanger.history.HistoryRepo;
import in.thetechguru.walle.remote.abremotewallpaperchanger.preferences.Preferences;

/**
 Copyright 2017 Amit Bhandari AB
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

public class ActivityHistory extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.status_text)
    TextView statusText;

    private HistoryAdapter adapter;

    @BindView(R.id.adView) AdView mAdView;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        recyclerView.setAdapter( adapter);

        setTitle("History");

        showAd();
    }

    private void showAd(){
        if (!Preferences.isAdsRemoved(this) && UtilityFun.isConnectedToInternet()) {
            MobileAds.initialize(getApplicationContext(), getString(R.string.banner_history));
            AdRequest adRequest = new AdRequest.Builder()//.addTestDevice("F40E78AED9B7FE233362079AC4C05B61")
                    .build();
            if (mAdView != null) {
                mAdView.loadAd(adRequest);
                mAdView.setVisibility(View.VISIBLE);
            }
        } else {
            if (mAdView != null) {
                mAdView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_history_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
                finish();
                break;

            case R.id.action_clear_history:
                if(adapter!=null && adapter.getItemCount()==0){
                    Toast.makeText(this, getString(R.string.status_empty_history), Toast.LENGTH_SHORT).show();
                }else {
                    new MaterialDialog.Builder(this)
                            .title(R.string.are_you_sure)
                            .content(R.string.history_clear_warning)
                            .positiveText(R.string.clear)
                            .negativeText(getString(R.string.cancel))
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    HistoryRepo.getInstance().nukeHistory();
                                    if (adapter != null) adapter.clearHistory();
                                    progressBar.setVisibility(View.GONE);
                                    statusText.setVisibility(View.VISIBLE);
                                    statusText.setText(R.string.status_empty_history);
                                }
                            })
                            .show();
                }
                break;

            case R.id.action_info:
                Toast.makeText(this, R.string.history_info_text, Toast.LENGTH_LONG).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        private List<HistoryItem> historyItems;
        private Handler handler;

        private static final int VIEW_TYPE_HISTORY_SELF = 1;
        private static final int VIEW_TYPE_HISTORY_ELSE = 2;

        HistoryAdapter(){
            handler = new Handler(Looper.getMainLooper());
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    historyItems = HistoryRepo.getInstance().getHistory();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.INVISIBLE);
                            if(historyItems.size()>0) {
                                recyclerView.setVisibility(View.VISIBLE);
                                notifyDataSetChanged();
                            }else {
                                statusText.setVisibility(View.VISIBLE);
                                statusText.setText(getString(R.string.status_empty_history));
                            }
                        }
                    });
                }
            });
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;

            if (viewType == VIEW_TYPE_HISTORY_SELF) {
                view = LayoutInflater.from(getApplicationContext())
                        .inflate(R.layout.item_history_changd_self, parent, false);
                return new HistorySelfChanged(view);
            } else if (viewType == VIEW_TYPE_HISTORY_ELSE) {
                view = LayoutInflater.from(getApplicationContext())
                        .inflate(R.layout.item_history_else_changed, parent, false);
                return new HistoryElseChanged(view);
            }

            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int i) {
            CharSequence ago =
                    DateUtils.getRelativeTimeSpanString(historyItems.get(i).timestamp);
            Log.d("HistoryAdapter", "onBindViewHolder: " + ago);

            switch (holder.getItemViewType()) {
                case VIEW_TYPE_HISTORY_SELF:
                    ((HistorySelfChanged)holder).textView.setText(getString(R.string.you_changed, historyItems.get(i).changedOf));
                    Glide.with(getApplicationContext()).load(Uri.parse(historyItems.get(i).path))
                            .override(200,200)
                            .centerCrop()
                            .placeholder(R.drawable.ic_error_outline_black_24dp)
                            .into(((HistorySelfChanged)holder).imageView);
                    ((HistorySelfChanged)holder).timestamp.setText(ago);
                    switch (historyItems.get(i).status){
                        case HistoryItem.STATUS.SUCCESS:
                            ((HistorySelfChanged) holder).changeStatus.setImageDrawable(getResources().getDrawable(R.drawable.ic_check_black_24dp));
                            break;

                        case HistoryItem.STATUS.FAILURE:
                            ((HistorySelfChanged) holder).changeStatus.setImageDrawable(getResources().getDrawable(R.drawable.ic_error_outline_black_24dp));
                            break;
                    }
                    break;

                case VIEW_TYPE_HISTORY_ELSE:
                    ((HistoryElseChanged)holder).textView.setText(getString(R.string.changed_by, historyItems.get(i).changedBy));
                    Log.d("HistoryAdapter", "onBindViewHolder: URI " + Uri.parse(historyItems.get(i).path));
                    Glide.with(getApplicationContext()).load(Uri.parse(historyItems.get(i).path))
                            .override(200,200)
                            .centerCrop()
                            .placeholder(R.drawable.ic_error_outline_black_24dp)
                            .into(((HistoryElseChanged)holder).imageView);
                    ((HistoryElseChanged)holder).timestamp.setText(ago);
            }
        }

        @Override
        public int getItemCount() {
            return historyItems.size();
        }

        @Override
        public int getItemViewType(int position) {
            String changedBy = historyItems.get(position).changedBy;

            if (changedBy.equals("self")) {
                // Changed by self
                return VIEW_TYPE_HISTORY_SELF;
            } else {
                // Changed by else
                return VIEW_TYPE_HISTORY_ELSE;
            }
        }

        void clearHistory(){
            historyItems.clear();
            notifyDataSetChanged();
        }

        class HistorySelfChanged extends RecyclerView.ViewHolder{

            @BindView(R.id.changed_by_username)
            TextView textView;
            @BindView(R.id.changed_wallpaper)
            ImageView imageView;
            @BindView(R.id.timestamp) TextView timestamp;
            @BindView(R.id.change_status) ImageView changeStatus;

            HistorySelfChanged(View itemView) {
                super(itemView);
                ButterKnife.bind(this,itemView);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(historyItems.get(getLayoutPosition()).path), "image/*");
                        startActivity(intent);

                        Log.d("HistoryAdapter", "onBindViewHolder: Uri " + Uri.parse(historyItems.get(getLayoutPosition()).path));
                    }
                });
            }
        }

        class HistoryElseChanged extends RecyclerView.ViewHolder {

            @BindView(R.id.changed_by_username)
            TextView textView;
            @BindView(R.id.changed_wallpaper)
            ImageView imageView;
            @BindView(R.id.timestamp) TextView timestamp;

            HistoryElseChanged(View itemView) {
                super(itemView);
                ButterKnife.bind(this,itemView);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(historyItems.get(getLayoutPosition()).path), "image/*");
                        startActivity(intent);
                    }
                });
            }

        }
    }

}
