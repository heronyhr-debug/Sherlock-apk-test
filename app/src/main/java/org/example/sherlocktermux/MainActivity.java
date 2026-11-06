
package org.example.sherlocktermux;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final String PERMISSION =
            "com.termux.permission.RUN_COMMAND";
    private static final int PERMISSION_REQUEST = 1001;
    private static final String HOME =
            "/data/data/com.termux/files/home";

    private EditText users, workers, proxy;
    private CheckBox txt, csv, json;
    private TextView status;

    private EditText field(
            LinearLayout box, String title,
            String hint, String value) {

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextSize(16);
        box.addView(label);

        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setText(value);
        box.addView(edit);
        return edit;
    }

    private void msg(String message) {
        status.setText(message);
        Toast.makeText(
                this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(28, 30, 28, 30);
        scroll.addView(box);

        TextView title = new TextView(this);
        title.setText("Sherlock · 公开用户名检索");
        title.setTextSize(23);
        box.addView(title);

        users = field(box,
                "用户名（逗号、空格或换行分隔）",
                "alice, bob", "");

        workers = field(box,
                "并发任务数 1–10", "1", "1");
        workers.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER);

        proxy = field(box,
                "代理（可留空）",
                "http://127.0.0.1:8080", "");

        txt = new CheckBox(this);
        txt.setText("TXT");
        txt.setChecked(true);
        box.addView(txt);

        csv = new CheckBox(this);
        csv.setText("CSV");
        csv.setChecked(true);
        box.addView(csv);

        json = new CheckBox(this);
        json.setText("JSON");
        json.setChecked(true);
        box.addView(json);

        Button run = new Button(this);
        run.setText("开始查询");
        box.addView(run);
        run.setOnClickListener(v -> launch());

        Button files = new Button(this);
        files.setText("打开导出文件");
        box.addView(files);

        files.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(
                    Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(intent, 1);
            } catch (Exception e) {
                msg("无法打开文件选择器："
                        + e.getMessage());
            }
        });

        Button settings = new Button(this);
        settings.setText("打开本应用权限设置");
        box.addView(settings);

        settings.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        status = new TextView(this);
        status.setText(
                "请先配置 Termux，然后点击开始查询。");
        box.addView(status);

        setContentView(scroll);
    }

    private void launch() {
        ArrayList<String> names = new ArrayList<>();

        for (String name :
                users.getText().toString().split("[,\\s]+")) {

            if (name.isEmpty()) continue;

            if (!name.matches("[A-Za-z0-9_.-]{1,64}")) {
                msg("用户名格式无效：" + name);
                return;
            }

            if (!names.contains(name)) {
                names.add(name);
            }
        }

        if (names.isEmpty() || names.size() > 50) {
            msg("用户名数量须为 1–50");
            return;
        }

        int count;

        try {
            count = Integer.parseInt(
                    workers.getText().toString().trim());
        } catch (NumberFormatException e) {
            msg("并发数必须为整数");
            return;
        }

        if (count < 1 || count > 10) {
            msg("并发数须为 1–10");
            return;
        }

        String p = proxy.getText().toString().trim();

        if (!p.isEmpty()
                && !(p.startsWith("http://")
                || p.startsWith("https://")
                || p.startsWith("socks5://"))) {
            msg("代理协议无效");
            return;
        }

        ArrayList<String> formats = new ArrayList<>();

        if (txt.isChecked()) formats.add("txt");
        if (csv.isChecked()) formats.add("csv");
        if (json.isChecked()) formats.add("json");

        if (formats.isEmpty()) {
            msg("请选择导出格式");
            return;
        }

        if (getPackageManager()
                .getLaunchIntentForPackage("com.termux")
                == null) {
            msg("未检测到 Termux。请确认已安装官方 Termux。");
            return;
        }

        if (checkSelfPermission(PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {

            msg("尚未授予 Termux 命令执行权限。"
                    + "请在弹窗中允许，然后再次点击开始查询。");

            requestPermissions(
                    new String[]{PERMISSION},
                    PERMISSION_REQUEST);
            return;
        }

        ArrayList<String> args = new ArrayList<>();

        args.add(HOME + "/sherlock-android/runner.py");
        args.add("--users");
        args.addAll(names);
        args.add("--workers");
        args.add(String.valueOf(count));
        args.add("--formats");
        args.add(android.text.TextUtils.join(",", formats));

        if (!p.isEmpty()) {
            args.add("--proxy");
            args.add(p);
        }

        Intent intent =
                new Intent("com.termux.RUN_COMMAND");

        intent.setClassName(
                "com.termux",
                "com.termux.app.RunCommandService");

        intent.putExtra(
                "com.termux.RUN_COMMAND_PATH",
                "/data/data/com.termux/files/usr/bin/python");

        intent.putExtra(
                "com.termux.RUN_COMMAND_ARGUMENTS",
                args.toArray(new String[0]));

        intent.putExtra(
                "com.termux.RUN_COMMAND_WORKDIR",
                HOME);

        intent.putExtra(
                "com.termux.RUN_COMMAND_BACKGROUND",
                false);

        intent.putExtra(
                "com.termux.RUN_COMMAND_SESSION_ACTION",
                "0");

        try {
            ComponentName result = startService(intent);

            if (result == null) {
                msg("系统未接受 Termux 服务启动请求。");
            } else {
                msg("已发送执行请求；请在 Termux 新会话中"
                        + "检查进度。此提示不代表查询已成功。");
            }

        } catch (SecurityException e) {
            msg("权限被拒绝：" + e.getMessage());

        } catch (Exception e) {
            msg("无法启动 Termux："
                    + e.getClass().getSimpleName()
                    + "：" + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                msg("权限已授予，请再次点击开始查询。");

            } else {
                msg("权限未授予。请点击"
                        + "‘打开本应用权限设置’，"
                        + "检查额外权限。");
            }
        }
    }
}
