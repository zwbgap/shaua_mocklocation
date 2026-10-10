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
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.shaua.mocklocation.utils.GoUtils;
import com.shaua.mocklocation.utils.ShareUtils;

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
        toolbar.setTitleTextColor(0xFFFFFFFF);
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
            }
        };
        sp.registerOnSharedPreferenceChangeListener(themeListener);

        // 版本号
        Preference versionPref = findPreference("pref_version");
        if (versionPref != null) {
            versionPref.setSummary(GoUtils.getVersionName(requireContext()));
        }

        // 查看日志
        Preference viewLogPref = findPreference("pref_view_log");
        if (viewLogPref != null) {
            viewLogPref.setOnPreferenceClickListener(pref -> {
                startActivity(new android.content.Intent(requireContext(), LogViewerActivity.class));
                return true;
            });
        }

        // 问题反馈
        Preference feedbackPref = findPreference("pref_feedback");
        if (feedbackPref != null) {
            feedbackPref.setOnPreferenceClickListener(pref -> {
                File file = new File(requireContext().getExternalFilesDir("Logs"),
                        com.shaua.mocklocation.GoApplication.LOG_FILE_NAME);
                ShareUtils.shareFile(requireContext(), file, "问题反馈");
                return true;
            });
        }

        // 反馈邮箱
        Preference emailPref = findPreference("pref_email");
        if (emailPref != null) {
            emailPref.setOnPreferenceClickListener(pref -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:3556248756@qq.com"));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"3556248756@qq.com"});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "迁境 问题反馈");
                try {
                    startActivity(emailIntent);
                } catch (Exception e) {
                    GoUtils.DisplayToast(requireContext(), "未找到邮件应用，请手动发送至 3556248756@qq.com");
                }
                return true;
            });
        }

        // 检测更新
        Preference updatePref = findPreference("pref_update");
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
    public void onDestroyView() {
        super.onDestroyView();
        if (themeListener != null) {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }
}
