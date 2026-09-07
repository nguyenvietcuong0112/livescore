package com.livescore.football.livescores.footballscores.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class SystemUtil {
    private static Locale myLocale;
    private static volatile String cachedLanguage = null;

    // Lưu ngôn ngữ đã cài đặt
    public static void saveLocale(Context context, String lang) {
        setPreLanguage(context, lang);
    }

    private static Locale getCurrentLocale(Context context) {
        if (context == null || context.getResources() == null) return null;
        Configuration currentConfig = context.getResources().getConfiguration();
        if (currentConfig == null) return null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (currentConfig.getLocales().size() > 0) {
                return currentConfig.getLocales().get(0);
            }
        }
        return currentConfig.locale;
    }

    public static void setLocale(Context context) {
        if (context == null) return;
        String language = getPreLanguage(context);
        if (language == null || language.isEmpty()) {
            language = "en";
        }

        Locale currentLocale = getCurrentLocale(context);
        if (currentLocale != null && language.equalsIgnoreCase(currentLocale.getLanguage())) {
            return;
        }

        changeLang(language, context);
    }

    public static void changeLang(String lang, Context context) {
        if (lang == null || lang.equalsIgnoreCase("") || context == null)
            return;
        
        Locale currentLocale = getCurrentLocale(context);
        if (currentLocale != null && lang.equalsIgnoreCase(currentLocale.getLanguage())) {
            cachedLanguage = lang;
            return;
        }

        Locale newLocale = new Locale(lang);
        myLocale = newLocale;
        saveLocale(context, lang);
        Locale.setDefault(myLocale);
        
        try {
            Configuration config = new Configuration(context.getResources().getConfiguration());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                config.setLocale(myLocale);
            } else {
                config.locale = myLocale;
            }
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getPreLanguage(Context mContext) {
        if (cachedLanguage != null) {
            return cachedLanguage;
        }
        if (mContext == null) return "en";
        SharedPreferences preferences = mContext.getSharedPreferences("data", Context.MODE_PRIVATE);
        cachedLanguage = preferences.getString("KEY_LANGUAGE", "en");
        return cachedLanguage;
    }

    public static void setPreLanguage(Context context, String language) {
        if (language == null || language.isEmpty()) {
            return;
        }
        cachedLanguage = language;
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("KEY_LANGUAGE", language);
            editor.apply();
        }
    }

    public static String getPath(Context context, Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String s = cursor.getString(column_index);
        cursor.close();
        return s;
    }

    public static File fileFromContentUri(Context context, Uri contentUri) throws IOException {
        String abc = "";
        String fileName = "";
        try {
            String fileDevice = getPath(context, contentUri);
            File file = new File(fileDevice);
            abc = file.getName();
        } catch (Exception e) {
            String fileExtension = getFileExtension(context, contentUri);
            try {
                String listUri[] = contentUri.toString().split("/");
                String fileNameEx = listUri[listUri.length - 1];
                Log.e("fileNameEx", fileNameEx);
                String urlDecodedTitle = URLDecoder.decode(fileNameEx, StandardCharsets.UTF_8.toString());
                String listUri2[] = urlDecodedTitle.split("_");
                if (listUri2.length > 1) {
                    for (int i = 0; i < listUri2.length - 1; i++) {
                        abc += listUri2[i];
                        if (i < listUri2.length - 2) {
                            abc += "_";
                        }
                    }
                } else {
                    abc = urlDecodedTitle;
                }
                abc += "." + fileExtension;
            } catch (Exception x) {
                if (fileExtension != null) {
                    abc = abc + fileExtension;
                } else {
                    abc = "";
                }
            }
        }
        fileName = abc;
        File tempFile = new File(context.getCacheDir(), fileName);
        tempFile.createNewFile();
        try {
            FileOutputStream oStream = new FileOutputStream(tempFile);
            InputStream inputStream = context.getContentResolver().openInputStream(contentUri);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                oStream.write(buf, 0, len);
            }
            oStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return tempFile;
        }
        return tempFile;
    }

    private static String getFileExtension(Context context, Uri uri) {
        String fileType = context.getContentResolver().getType(uri);
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType);
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}