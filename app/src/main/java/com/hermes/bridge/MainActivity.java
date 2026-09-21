package com.hermes.bridge;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

/**
 * 跳板：打开后自动拉起目标 Activity（默认小米备份前台），然后退出自己。
 * 目标可通过 SettingsActivity（长按图标 → 设置）修改；
 * 也可通过 adb 设置：
 *   adb shell am start -n com.hermes.bridge/.MainActivity \
 *       --es target "com.miui.backup/.local.LocalHomeActivity"
 */
public class MainActivity extends Activity {

    static final String PREFS = "bridge";
    static final String KEY_TARGET = "target";
    static final String DEFAULT_TARGET = "com.miui.backup/.local.LocalHomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) intent extra 优先（便于 adb / 自动化调用时临时指定目标）
        String target = getIntent().getStringExtra("target");
        if (target == null || target.trim().isEmpty()) {
            // 2) 其次读取保存的设置
            SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            target = sp.getString(KEY_TARGET, DEFAULT_TARGET);
        }
        if (target == null || target.trim().isEmpty()) {
            target = DEFAULT_TARGET;
        }

        ComponentName cn = ComponentName.unflattenFromString(target.trim());
        if (cn == null) {
            // 非法格式，回退默认
            cn = ComponentName.unflattenFromString(DEFAULT_TARGET);
        }

        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(cn);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            // 启动失败（未安装/不可达），打开设置界面让用户选择
            try {
                startActivity(new Intent(this, SettingsActivity.class));
            } catch (Exception ignore) {
            }
        }
        finish(); // 跳板使命完成，立即退出
    }
}
