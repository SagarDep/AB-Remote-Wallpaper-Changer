package in.thetechguru.walle.remote.abremotewallpaperchanger.activity_fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.R;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.UtilityFun;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.Constants;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.User;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

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

public class FragmentBlockList extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    FragmentBlockList.BlockedListAdapter adapter;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.status_text)
    TextView statusText;
    int clickedPosition;

    @BindView(R.id.swipe_to_refresh)
    SwipeRefreshLayout swipeRefreshLayout;

    BroadcastReceiver refreshReceiver;

    public FragmentBlockList(){
        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(adapter!=null) adapter.refreshList();
                Log.d("FragmentFriends", "onReceive: ");
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(getContext()!=null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(refreshReceiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getContext()!=null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(refreshReceiver, new IntentFilter(Constants.ACTIONS.REFRESH_BLOCK_LIST));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layout = inflater.inflate(R.layout.fragment_block_list, container, false);
        ButterKnife.bind(this, layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        adapter = new FragmentBlockList.BlockedListAdapter(getActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return layout;
    }

    @Override
    public void onRefresh() {
        if(adapter!=null) {
            swipeRefreshLayout.setRefreshing(true);
            adapter.refreshList();
        }
    }

    class BlockedListAdapter extends RecyclerView.Adapter<FragmentBlockList.BlockedListAdapter.MyViewHolder>
            implements PopupMenu.OnMenuItemClickListener{

        private List<User> users = new ArrayList<>();
        GetBlockedList getFriendListThread;
        Activity activity;

        BlockedListAdapter(Activity activity) {
            this.activity = activity;
            getFriendListThread = new GetBlockedList();
            refreshList();
        }

        private void refreshList() {
            users.clear();
            progressBar.setVisibility(View.VISIBLE);
            statusText.setVisibility(View.INVISIBLE);
            new GetBlockedList().start();
        }

        @Override
        public FragmentBlockList.BlockedListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_blocked_contact, parent, false);
            return new FragmentBlockList.BlockedListAdapter.MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FragmentBlockList.BlockedListAdapter.MyViewHolder holder, int position) {
            //String mainText = users.get(position).display_name + " ("  + users.get(position).username +")";
            holder.textView.setText(users.get(position).display_name);

            //@todo profile photo
            Glide.with(MyApp.getContext())
                    .load(users.get(position).pic_url)
                    .placeholder(R.drawable.person_blue)
                    .override(200,200)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_unblock_user:
                    new MaterialDialog.Builder(activity)
                            .title(getString(R.string.unblock_warn, users.get(clickedPosition).display_name) )
                            .positiveText(R.string.unblock)
                            .negativeText(getString(R.string.cancel))
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    //remove token from blocked users list
                                    FirebaseUtil.getBlockedRef().child(users.get(clickedPosition).username).removeValue();

                                    Toast.makeText(MyApp.getContext(), getString(R.string.unblocked_toast, users.get(clickedPosition).display_name), Toast.LENGTH_SHORT).show();
                                    users.remove(clickedPosition);
                                    notifyItemRemoved(clickedPosition);
                                }
                            })
                            .show();
                    break;
            }
            return true;
        }

        void onClick(View view, int position) {
            clickedPosition = position;
            switch (view.getId()){
                case R.id.card_view:
                case R.id.menu_popup:
                    PopupMenu popup=new PopupMenu(getContext(),view, Gravity.RIGHT);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.menu_blocked_item, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(this);
                    break;
            }
        }

        class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

            @BindView(R.id.blocked_item_display_name)
            TextView textView;
            @BindView(R.id.blocked_item_display_picture)
            ImageView imageView;
            @BindView(R.id.menu_popup) ImageView popup;

            MyViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this,itemView);
                itemView.setOnClickListener(this);
                popup.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                FragmentBlockList.BlockedListAdapter.this.onClick(view, getLayoutPosition());
            }
        }

        class GetBlockedList extends Thread {

            @Override
            public void run() {
                getRunnable().run();
            }

            private Runnable getRunnable(){
                return new Runnable() {
                    @Override
                    public void run(){
                        if(!UtilityFun.isConnectedToInternet()){
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.INVISIBLE);
                                    statusText.setVisibility(View.VISIBLE);
                                    statusText.setText(R.string.error_no_network_swipe_down);
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            });
                            return;
                        }
                        FirebaseUtil.getBlockedRef().addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.getChildrenCount()<=0){
                                    progressBar.setVisibility(View.INVISIBLE);
                                    statusText.setVisibility(View.VISIBLE);
                                    swipeRefreshLayout.setRefreshing(false);
                                    statusText.setText(R.string.error_no_blocked_friends);
                                }
                                for(DataSnapshot snap : dataSnapshot.getChildren()){
                                    final String user_name  = snap.getKey();
                                    FirebaseUtil.getUsernamesReference().child(user_name).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            String uid = dataSnapshot.getValue(String.class);
                                            if(uid==null){
                                                return;
                                            }
                                            FirebaseUtil.getUsersReference().child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    User user = dataSnapshot.getValue(User.class);
                                                    users.add(user);
                                                    statusText.setVisibility(View.INVISIBLE);
                                                    recyclerView.setVisibility(View.VISIBLE);
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    swipeRefreshLayout.setRefreshing(false);
                                                    notifyDataSetChanged();
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    swipeRefreshLayout.setRefreshing(false);
                                                    statusText.setVisibility(View.VISIBLE);
                                                    statusText.setText(R.string.block_list_fetch_unknown_error);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            swipeRefreshLayout.setRefreshing(false);
                                            statusText.setVisibility(View.VISIBLE);
                                            statusText.setText(R.string.block_list_fetch_unknown_error);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.d("GetBlockedList", "onCancelled: Error getting friend list");
                                progressBar.setVisibility(View.INVISIBLE);
                                statusText.setVisibility(View.VISIBLE);
                                swipeRefreshLayout.setRefreshing(false);
                                statusText.setText(R.string.block_list_fetch_unknown_error);
                            }
                        });

                    }
                };
            }
        }
    }


}


/*
/*
                                    //add token back confirmed in both user directories i.e
                                    //from self --> confirmed and user_getting_unblocked --> confirmed
                                    FirebaseUtil.getConfirmedRef(users.get(clickedPosition).username)
                                            .child(MyApp.getUser().username).setValue(true);
                                    FirebaseUtil.getConfirmedRef().child(users.get(clickedPosition).username).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(getContext()!=null)
                                                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(Constants.ACTIONS.REFRESH_FRIEND_LIST));
                                        }
                                    });;
                                    */
//refreshList();
