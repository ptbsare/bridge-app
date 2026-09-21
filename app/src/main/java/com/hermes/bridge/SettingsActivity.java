package com.hermes.bridge;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {

    private static final String PREFS = "bridge";
    private static final String KEY_TARGET = "target";

    private EditText etSearch;
    private ListView listView;
    private ProgressBar progress;
    private TextView tvCurrent;

    private final List<String> allComponents = new ArrayList<>();  // pkg/class
    private final List<String> allLabels = new ArrayList<>();      // "应用名 → Activity"
    private final List<Integer> allTypes = new ArrayList<>();       // 0=launcher, 1=exported
    private final List<String> shownComponents = new ArrayList<>();
    private final List<String> shownLabels = new ArrayList<>();
    private final List<Integer> shownTypes = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ActivityAdapter adapter;
    private String savedTarget = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tvCurrent = findViewById(R.id.tv_current);
        etSearch = findViewById(R.id.et_search);
        listView = findViewById(R.id.list);
        progress = findViewById(R.id.progress);

        adapter = new ActivityAdapter();
        listView.setAdapter(adapter);

        // 读取当前目标并显示
        SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        savedTarget = sp.getString(KEY_TARGET, MainActivity.DEFAULT_TARGET);
        tvCurrent.setText("当前目标：" + savedTarget);

        // 点击 → 保存并启动
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String comp = shownComponents.get(position);
            saveAndLaunch(comp);
        });

        // 搜索过滤
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });

        // 后台加载 Activity 列表
        loadActivities();
    }

    private void loadActivities() {
        progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<String> comps = new ArrayList<>();
            List<String> labs = new ArrayList<>();
            List<Integer> types = new ArrayList<>();

            for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
                String pkg = app.packageName;
                try {
                    android.content.pm.PackageInfo pi = pm.getPackageInfo(pkg,
                            PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
                    if (pi.activities == null) continue;

                    for (ActivityInfo ai : pi.activities) {
                        if (ai.enabled && ai.exported) {
                            String compName = ai.name.startsWith(".")
                                    ? pkg + "/" + ai.name
                                    : pkg + "/" + ai.name;
                            String appLabel;
                            try {
                                appLabel = String.valueOf(app.loadLabel(pm));
                            } catch (Exception e) {
                                appLabel = pkg;
                            }

                            // 判断是否有 MAIN+LAUNCHER（优先级更高）
                            boolean isLauncher = false;
                            if (pi.activities != null) {
                                // 通过 intent-filter metadata 判断，简化：直接标记
                                // ActivityInfo.intentFilters 在 API 33+ 才有，这里用一个替代方案
                                isLauncher = false; // 仅靠 exported 标记
                            }

                            int type = isLauncher ? 0 : 1;
                            comps.add(compName);
                            labs.add(appLabel + " → " + ai.name.substring(ai.name.lastIndexOf('.') + 1));
                            types.add(type);
                        }
                    }
                } catch (Exception ignore) {
                }
            }

            // 按 label 排序
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < comps.size(); i++) indices.add(i);
            indices.sort((a, b) -> labs.get(a).compareToIgnoreCase(labs.get(b)));

            List<String> sortedComps = new ArrayList<>();
            List<String> sortedLabs = new ArrayList<>();
            List<Integer> sortedTypes = new ArrayList<>();
            for (int i : indices) {
                sortedComps.add(comps.get(i));
                sortedLabs.add(labs.get(i));
                sortedTypes.add(types.get(i));
            }

            mainHandler.post(() -> {
                allComponents.clear();
                allLabels.clear();
                allTypes.clear();
                allComponents.addAll(sortedComps);
                allLabels.addAll(sortedLabs);
                allTypes.addAll(sortedTypes);
                progress.setVisibility(View.GONE);
                filter(etSearch.getText().toString());
                Toast.makeText(this, "找到 " + allComponents.size() + " 个可启动 Activity", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void filter(String query) {
        shownComponents.clear();
        shownLabels.clear();
        shownTypes.clear();
        String q = query.toLowerCase().trim();
        for (int i = 0; i < allComponents.size(); i++) {
            if (q.isEmpty()
                    || allLabels.get(i).toLowerCase().contains(q)
                    || allComponents.get(i).toLowerCase().contains(q)) {
                shownComponents.add(allComponents.get(i));
                shownLabels.add(allLabels.get(i));
                shownTypes.add(allTypes.get(i));
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void saveAndLaunch(String component) {
        SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TARGET, component).apply();
        savedTarget = component;
        tvCurrent.setText("当前目标：" + component);
        Toast.makeText(this, "已保存：" + component, Toast.LENGTH_SHORT).show();

        // 测试启动
        try {
            ComponentName cn = ComponentName.unflattenFromString(component);
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(cn);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "启动失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ======================== Adapter ========================

    private class ActivityAdapter extends BaseAdapter {

        @Override
        public int getCount() { return shownComponents.size(); }

        @Override
        public Object getItem(int position) { return shownComponents.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(SettingsActivity.this)
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            TextView tv1 = convertView.findViewById(android.R.id.text1);
            TextView tv2 = convertView.findViewById(android.R.id.text2);

            String comp = shownComponents.get(position);
            String label = shownLabels.get(position);

            tv1.setText(label);
            tv2.setText(comp);

            // 已保存的目标高亮显示
            if (comp.equals(savedTarget)) {
                tv1.setTextColor(0xFF2196F3); // blue
                tv2.setTextColor(0xFF2196F3);
            } else {
                tv1.setTextColor(0xFF000000);
                tv2.setTextColor(0xFF666666);
            }
            return convertView;
        }
    }
}
