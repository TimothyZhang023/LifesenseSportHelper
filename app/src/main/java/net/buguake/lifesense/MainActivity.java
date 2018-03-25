package net.buguake.lifesense;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;

import net.buguake.lifesense.model.AppInfo;
import net.buguake.lifesense.ui.AboutActivity;
import net.buguake.lifesense.ui.SettingsActivity;
import net.buguake.lifesense.util.CommonUtil;
import net.buguake.lifesense.util.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "buguake-main";

    private NormalRecyclerViewAdapter _NormalRecyclerViewAdapter = null;
    private SwipeRefreshLayout _SwipeRefreshLayout = null;

    private ArrayList<ApplicationInfo> _DisplayApplicationInfo = new ArrayList<>();
    private List<ApplicationInfo> applicationInfos = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Hi ~", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        RecyclerView _RecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        _RecyclerView.setLayoutManager(new LinearLayoutManager(this));
        _RecyclerView.setAdapter(_NormalRecyclerViewAdapter = new NormalRecyclerViewAdapter());

        _SwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.SwipeRefreshLayout);
        _SwipeRefreshLayout.setOnRefreshListener(this);


        MainActivity.this.onRefresh();

        if (!MainApplication.getInstance().isXposedWork()) {
            Snackbar.make(_SwipeRefreshLayout, R.string.xposed_not_working, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, v1 -> {
                    })
                    .show();
        } else {
            Snackbar.make(_SwipeRefreshLayout, R.string.guide_text, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, v1 -> {
                    })
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setIconified(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                _NormalRecyclerViewAdapter.getFilter().filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                _NormalRecyclerViewAdapter.getFilter().filter(newText);
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_refresh) {
            MainActivity.this.onRefresh();
        } else if (id == R.id.nav_donate) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://mobilecodec.alipay.com/client_download.htm?qrcode=a6x04638rp427lbqmdourfe")));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private Map<String, AppInfo> infoCache = new ConcurrentHashMap<>();

    @Override
    public void onRefresh() {
        AsyncTask<Object, Object, Object> asyncTask = new AsyncTask<Object, Object, Object>() {
            @Override
            protected void onPreExecute() {
                _SwipeRefreshLayout.setRefreshing(true);
                _NormalRecyclerViewAdapter.notifyItemRangeRemoved(0, _DisplayApplicationInfo.size());
                _DisplayApplicationInfo.clear();
            }

            @Override
            protected Object doInBackground(Object... params) {
                Set<String> ignorePackageName = new HashSet<>();
                ignorePackageName.add("android");
                ignorePackageName.add(BuildConfig.APPLICATION_ID);
                ignorePackageName.add("de.robv.android.xposed.installer");

                int hasConfigIndex = 0;

                boolean show_system_app = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("show_system_app", false);
                applicationInfos = MainActivity.this.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
                Collections.sort(applicationInfos, new ApplicationInfo.DisplayNameComparator(MainActivity.this.getPackageManager()));

                for (ApplicationInfo applicationInfo : applicationInfos) {
                    if (ignorePackageName.contains(applicationInfo.packageName)) {
                        continue;
                    }
                    if (show_system_app || CommonUtil.isUserApplication(applicationInfo)) {
                        AppInfo appInfo = SharedPreferencesHelper.getInstance().get(applicationInfo.packageName);
                        if (appInfo != null) {
                            infoCache.put(applicationInfo.packageName, appInfo);
                            _DisplayApplicationInfo.add(hasConfigIndex++, applicationInfo);
                        } else {
                            _DisplayApplicationInfo.add(applicationInfo);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                _NormalRecyclerViewAdapter.setmOriginalValues(_DisplayApplicationInfo);
                _NormalRecyclerViewAdapter.notifyDataSetChanged();
                _SwipeRefreshLayout.setRefreshing(false);
            }
        };
        asyncTask.execute();
    }

    private class NormalRecyclerViewAdapter extends RecyclerView.Adapter<NormalRecyclerViewAdapter.NormalTextViewHolder> implements Filterable {
        private ArrayList<ApplicationInfo> mOriginalValues = new ArrayList<>();

        @Override
        public NormalRecyclerViewAdapter.NormalTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new NormalTextViewHolder(LayoutInflater.from(MainActivity.this).inflate(R.layout.content_listview, parent, false));
        }

        @Override
        public void onBindViewHolder(final NormalRecyclerViewAdapter.NormalTextViewHolder holder, int position) {
            holder.applicationInfo = _DisplayApplicationInfo.get(position);

            holder.tvTtitle.setText(holder.applicationInfo.loadLabel(getPackageManager()));
            holder.tvPkgName.setText(holder.applicationInfo.packageName);
            holder.imageView.setImageDrawable(holder.applicationInfo.loadIcon(getPackageManager()));

//            AppInfo appInfo = SharedPreferencesHelper.getInstance().get(holder.applicationInfo.packageName);
            AppInfo appInfo = infoCache.get(holder.applicationInfo.packageName);
            holder.tvTtitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), (appInfo != null && appInfo.pushEnable) ? R.color.bootstrap_brand_success : android.R.color.primary_text_light));
        }

        @Override
        public int getItemCount() {
            return _DisplayApplicationInfo.size();
        }

        public void setmOriginalValues(ArrayList<ApplicationInfo> mOriginalValues) {
            this.mOriginalValues = mOriginalValues;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();

                    if (constraint == null || constraint.length() == 0) {
                        ArrayList<ApplicationInfo> applicationInfoArrayList = new ArrayList<>(mOriginalValues);
                        filterResults.values = applicationInfoArrayList;
                        filterResults.count = applicationInfoArrayList.size();
                        return filterResults;
                    }

                    String prefixString = constraint.toString().toLowerCase();
                    final ArrayList<ApplicationInfo> values = mOriginalValues;
                    final int count = mOriginalValues.size();

                    final ArrayList<ApplicationInfo> newValues = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        final ApplicationInfo applicationInfo = values.get(i);
                        if (applicationInfo.packageName.toLowerCase().contains(prefixString) || applicationInfo.loadLabel(getPackageManager()).toString().toLowerCase().contains(prefixString))
                            newValues.add(applicationInfo);
                    }

                    filterResults.values = newValues;
                    filterResults.count = newValues.size();

                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    _DisplayApplicationInfo = (ArrayList<ApplicationInfo>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        class NormalTextViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private TextView tvTtitle;
            private TextView tvPkgName;
            private ImageView imageView;
            private ApplicationInfo applicationInfo;

            NormalTextViewHolder(View itemView) {
                super(itemView);
                tvTtitle = (TextView) itemView.findViewById(R.id.text1);
                tvPkgName = (TextView) itemView.findViewById(R.id.text2);
                imageView = (ImageView) itemView.findViewById(R.id.image_view);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {

                AppInfo appInfo = SharedPreferencesHelper.getInstance().get(tvPkgName.getText().toString());
                if (appInfo == null) {
                    appInfo = new AppInfo();
                    appInfo.pushEnable = true;
                } else {
                    appInfo.pushEnable = !appInfo.pushEnable;
                }

                SharedPreferencesHelper.getInstance().set(tvPkgName.getText().toString(), appInfo);
                tvTtitle.setTextColor(ContextCompat.getColor(v.getContext(), (appInfo.pushEnable) ? R.color.bootstrap_brand_success : android.R.color.primary_text_light));

                infoCache.put(applicationInfo.packageName, appInfo);

                Snackbar.make(v, R.string.pls_restart_ls_sport, Snackbar.LENGTH_LONG)
                        .setAction(R.string.ok, v1 -> {
                        })
                        .show();


            }
        }
    }

    protected Activity getActivity() {
        return this;
    }


}
