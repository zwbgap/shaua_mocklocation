package com.shaua.mocklocation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.shaua.mocklocation.utils.GoUtils;

import java.io.File;

/**
 * "我的"页面：设置 + 日志开关 + 反馈 + 更新 + 联系作者
 */
public class MineFragment extends PreferenceFragmentCompat {

    private OnMineActionListener listener;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    public interface OnMineActionListener {
        void onCheckUpdate();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnMineActionListener) {
            listener = (OnMineActionListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 在父类的首选项列表上方叠加一个 Toolbar，与收藏页保持一致
        View superView = super.onCreateView(inflater, container, savedInstanceState);
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        Toolbar toolbar = new Toolbar(requireContext());
        toolbar.setBackgroundColor(GoUtils.getThemePrimaryColor(requireContext()));
        toolbar.setTitle("设置");
        toolbar.setTitleTextColor(GoUtils.getThemeOnPrimaryColor(requireContext()));
        int actionBarSize = 0;
        int[] attrs = new int[]{androidx.appcompat.R.attr.actionBarSize};
        android.content.res.TypedArray ta = requireContext().getTheme().obtainStyledAttributes(attrs);
        actionBarSize = ta.getDimensionPixelSize(0, 0);
        ta.recycle();
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, actionBarSize);
        layout.addView(toolbar, tbLp);

        if (superView != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layout.addView(superView, lp);
        }
        return layout;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_mine, rootKey);

        // 监听主题变更，重启 Activity 生效
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        themeListener = (sharedPrefs, key) -> {
            if ("setting_theme_color".equals(key)) {
                requireActivity().recreate();
            } else if ("setting_app_icon".equals(key)) {
                String iconValue = sharedPrefs.getString(key, "default");
                GoUtils.switchAppIcon(requireContext(), iconValue);
                GoUtils.DisplayToast(requireContext(), "图标已切换，桌面图标可能需要稍后刷新");
            }
        };
        sp.registerOnSharedPreferenceChangeListener(themeListener);

        // 回显当前图标（兼容从旧版本升级的情况）
        ListPreference iconPref = findPreference("setting_app_icon");
        if (iconPref != null) {
            String current = GoUtils.getCurrentAppIcon(requireContext());
            if (!current.equals(iconPref.getValue())) {
                iconPref.setValue(current);
            }
        }

        // 检测更新：摘要显示当前版本号
        Preference updatePref = findPreference("pref_update");
        if (updatePref != null) {
            updatePref.setSummary("当前版本 " + GoUtils.getVersionName(requireContext()));
        }

        // 查看日志
        Preference viewLogPref = findPreference("pref_view_log");
        if (viewLogPref != null) {
            viewLogPref.setOnPreferenceClickListener(pref -> {
                startActivity(new android.content.Intent(requireContext(), LogViewerActivity.class));
                return true;
            });
        }

        // 清理缓存：显示当前缓存大小，确认后执行清理
        Preference clearCachePref = findPreference("pref_clear_cache");
        if (clearCachePref != null) {
            updateCacheSummary(clearCachePref);
            clearCachePref.setOnPreferenceClickListener(pref -> {
                long cacheSize = GoUtils.getCacheSize(requireContext());
                String sizeStr = GoUtils.formatFileSize(cacheSize);
                androidx.appcompat.app.AlertDialog dlg = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("清理缓存")
                        .setMessage("当前可清理缓存约 " + sizeStr + "\n\n将清理：日志文件、搜索历史、地图瓦片缓存、残留安装包\n\n确定要清理吗？")
                        .setPositiveButton("清理", (dialog, which) -> {
                            long freed = GoUtils.clearCache(requireContext());
                            String freedStr = GoUtils.formatFileSize(freed);
                            GoUtils.DisplayToast(requireContext(), "已清理 " + freedStr);
                            updateCacheSummary(clearCachePref);
                        })
                        .setNegativeButton("取消", null)
                        .create();
                dlg.show();
                dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(GoUtils.getThemeAccentColor(requireContext()));
                dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(GoUtils.getThemeOnSurfaceVariantColor(requireContext()));
                return true;
            });
        }

        // 问题反馈：弹框提醒通过邮箱发送日志报告
        Preference feedbackPref = findPreference("pref_feedback");
        if (feedbackPref != null) {
            feedbackPref.setOnPreferenceClickListener(pref -> {
                final File logFile = new File(requireContext().getExternalFilesDir("Logs"),
                        com.shaua.mocklocation.GoApplication.LOG_FILE_NAME);
                androidx.appcompat.app.AlertDialog fbDlg = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("问题反馈")
                        .setMessage("请通过邮箱 3556248756@qq.com 发送日志报告。\n\n点击「发送」将打开邮件应用并附上日志文件。")
                        .setPositiveButton("发送", (dialog, which) -> {
                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                            emailIntent.setData(Uri.parse("mailto:3556248756@qq.com"));
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"3556248756@qq.com"});
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "迁境 问题反馈");
                            if (logFile.exists()) {
                                emailIntent.putExtra(Intent.EXTRA_STREAM,
                                        androidx.core.content.FileProvider.getUriForFile(requireContext(),
                                                requireContext().getPackageName() + ".fileProvider", logFile));
                                emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            }
                            try {
                                startActivity(emailIntent);
                            } catch (Exception e) {
                                GoUtils.DisplayToast(requireContext(),
                                        "未找到邮件应用，请手动将日志发送至 3556248756@qq.com");
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create();
                fbDlg.show();
                fbDlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(GoUtils.getThemeAccentColor(requireContext()));
                fbDlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(GoUtils.getThemeOnSurfaceVariantColor(requireContext()));
                return true;
            });
        }

        // 检测更新
        if (updatePref != null) {
            updatePref.setOnPreferenceClickListener(pref -> {
                if (listener != null) {
                    listener.onCheckUpdate();
                }
                return true;
            });
        }

        // 联系作者
        Preference contactPref = findPreference("pref_contact");
        if (contactPref != null) {
            contactPref.setOnPreferenceClickListener(pref -> {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/zwbgap/shaua_mocklocation/issues")));
                return true;
            });
        }

        // 开发人员选项
        Preference devPref = findPreference("pref_dev");
        if (devPref != null) {
            devPref.setOnPreferenceClickListener(pref -> {
                if (!GoUtils.isDeveloperOptionsEnabled(requireContext())) {
                    GoUtils.DisplayToast(requireContext(), "请先开启开发者选项");
                } else {
                    try {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                    } catch (Exception e) {
                        GoUtils.DisplayToast(requireContext(), "无法打开开发者选项");
                    }
                }
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Preference clearCachePref = findPreference("pref_clear_cache");
        if (clearCachePref != null) {
            updateCacheSummary(clearCachePref);
        }
    }

    private void updateCacheSummary(Preference pref) {
        long cacheSize = GoUtils.getCacheSize(requireContext());
        String sizeStr = GoUtils.formatFileSize(cacheSize);
        pref.setSummary("当前缓存 " + sizeStr + "（日志、搜索历史、地图缓存、安装包）");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (themeListener != null) {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }
}
