package com.shaua.mocklocation;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.shaua.mocklocation.utils.GoUtils;
import com.shaua.mocklocation.utils.ShareUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * 日志查看页面
 */
public class LogViewerActivity extends BaseActivity {

    private TextView logText;
    private ScrollView scrollView;
    private File logFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        Toolbar toolbar = findViewById(R.id.log_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        logText = findViewById(R.id.log_text);
        scrollView = findViewById(R.id.log_scroll);
        logFile = new File(getExternalFilesDir("Logs"), GoApplication.LOG_FILE_NAME);

        loadLog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh_log) {
            loadLog();
            return true;
        } else if (id == R.id.action_clear_log) {
            clearLog();
            return true;
        } else if (id == R.id.action_share_log) {
            ShareUtils.shareFile(this, logFile, "运行日志");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadLog() {
        if (!logFile.exists()) {
            logText.setText("日志文件不存在");
            return;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception e) {
            sb.append("读取日志失败: ").append(e.getMessage());
        }
        logText.setText(sb.toString());
        // 滚动到底部
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void clearLog() {
        try (FileWriter writer = new FileWriter(logFile, false)) {
            writer.write("");
            logText.setText("日志已清空");
            GoUtils.DisplayToast(this, "日志已清空");
        } catch (Exception e) {
            GoUtils.DisplayToast(this, "清空失败: " + e.getMessage());
        }
    }
}
