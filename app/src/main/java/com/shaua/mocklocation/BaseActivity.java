package com.shaua.mocklocation;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        // 应用主题配色；dynamic 表示跟随系统动态配色，由 Application 的 DynamicColors 处理
        String themeColor = sp.getString("setting_theme_color", "dynamic");
        if (!"dynamic".equals(themeColor)) {
            setTheme(getThemeResId(themeColor));
        }

        super.onCreate(savedInstanceState);
    }

    private int getThemeResId(String themeColor) {
        switch (themeColor) {
            case "blue":
                return R.style.AppTheme_Blue;
            case "purple":
                return R.style.AppTheme_Purple;
            case "orange":
                return R.style.AppTheme_Orange;
            case "teal":
            default:
                return R.style.AppTheme_Teal;
        }
    }
}
