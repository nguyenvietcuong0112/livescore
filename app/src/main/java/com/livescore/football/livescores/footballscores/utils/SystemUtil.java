package com.livescore.football.livescores.footballscores.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.util.Log;

import java.util.Locale;

public class SystemUtil {
    private static Locale myLocale;

    public static void saveLocale(Context context, String lang) {
        if (lang == null || lang.trim().isEmpty()) return;
        Log.d("LanguageDebug", "SystemUtil.saveLocale called with lang=" + lang);
        setPreLanguage(context, lang);
    }

    public static void setLocale(Context context) {
        String language = getPreLanguage(context);
        Log.d("LanguageDebug", "SystemUtil.setLocale called, resolved preLanguage=" + language);
        if (language == null || language.trim().isEmpty()) {
            language = "en";
        }

        Locale current = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.os.LocaleList locales = context.getResources().getConfiguration().getLocales();
            if (locales != null && !locales.isEmpty()) {
                current = locales.get(0);
            }
        }
        if (current == null) {
            current = context.getResources().getConfiguration().locale;
        }

        if (current != null && language.equalsIgnoreCase(current.getLanguage())) {
            Log.d("LanguageDebug", "SystemUtil.setLocale: already in " + language + ", skipping updateConfiguration");
            return;
        }

        Locale newLocale = new Locale(language);
        Locale.setDefault(newLocale);
        myLocale = newLocale;

        try {
            Configuration config = new Configuration(context.getResources().getConfiguration());
            config.setLocale(newLocale);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.os.LocaleList localeList = new android.os.LocaleList(newLocale);
                android.os.LocaleList.setDefault(localeList);
                config.setLocales(localeList);
            }
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        } catch (Exception e) {
            Log.e("LanguageDebug", "SystemUtil.setLocale updateConfiguration error: " + e.getMessage());
        }
    }

    public static void changeLang(String lang, Context context) {
        if (lang == null || lang.trim().isEmpty())
            return;
        Log.d("LanguageDebug", "SystemUtil.changeLang called with lang=" + lang);

        saveLocale(context, lang);
        setLocale(context);

        try {
            androidx.core.os.LocaleListCompat currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales();
            String currentTag = currentLocales.toLanguageTags();
            if (!lang.equalsIgnoreCase(currentTag)) {
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.forLanguageTags(lang)
                );
            }
        } catch (Exception e) {
            Log.e("LanguageDebug", "SystemUtil.changeLang setApplicationLocales error: " + e.getMessage());
        }
    }

    public static String getPreLanguage(Context mContext) {
        String cscLang = null;
        try {
            cscLang = new com.cscmobi.libraryads.commons.sharepreference.CSCSPF(mContext).getLanguage_code_selected();
        } catch (Exception e) {
            Log.e("LanguageDebug", "SystemUtil.getPreLanguage: CSCSPF read error=" + e.getMessage());
        }
        SharedPreferences preferences = mContext.getSharedPreferences("data", Context.MODE_PRIVATE);
        String dataLang = preferences.getString("KEY_LANGUAGE", null);
        Log.d("LanguageDebug", "SystemUtil.getPreLanguage: cscLang=" + cscLang + ", dataLang=" + dataLang);
        if (cscLang != null && !cscLang.trim().isEmpty()) {
            return cscLang;
        }
        if (dataLang != null && !dataLang.trim().isEmpty()) {
            return dataLang;
        }
        return Locale.getDefault().getLanguage();
    }

    public static void setPreLanguage(Context context, String language) {
        Log.d("LanguageDebug", "SystemUtil.setPreLanguage called with language=" + language);
        if (language == null || language.isEmpty()) {
            return;
        } else {
            try {
                new com.cscmobi.libraryads.commons.sharepreference.CSCSPF(context).setLanguage_code_selected(language);
                Log.d("LanguageDebug", "SystemUtil.setPreLanguage: saved to CSCSPF=" + language);
            } catch (Exception e) {
                Log.e("LanguageDebug", "SystemUtil.setPreLanguage: CSCSPF write error=" + e.getMessage());
            }
            SharedPreferences preferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("KEY_LANGUAGE", language);
            editor.apply();
            Log.d("LanguageDebug", "SystemUtil.setPreLanguage: saved to SharedPreferences(data)=" + language);
        }
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}