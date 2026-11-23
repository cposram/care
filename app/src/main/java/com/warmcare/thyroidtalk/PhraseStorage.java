package com.warmcare.thyroidtalk;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PhraseStorage {
    private static final String PREF_NAME = "warmcare_phrases_pref";
    private static final String KEY_CUSTOM_PHRASES = "custom_phrases_json";
    private static final String KEY_MUTED = "is_muted";

    public static final String CAT_ALL = "全部";
    public static final String CAT_EMERGENCY = "🚨 緊急醫護";
    public static final String CAT_THROAT = "💧 喉嚨與口腔";
    public static final String CAT_BASIC = "🛏️ 生理與用藥";
    public static final String CAT_ENV = "🔌 環境與物品";
    public static final String CAT_REPLY = "💬 常用回覆";
    public static final String CAT_CUSTOM = "⭐ 我的常用句";

    public static List<String> getCategories() {
        return Arrays.asList(
                CAT_ALL,
                CAT_EMERGENCY,
                CAT_THROAT,
                CAT_BASIC,
                CAT_ENV,
                CAT_REPLY,
                CAT_CUSTOM
        );
    }

    public static List<String> getSectionCategories() {
        return Arrays.asList(
                CAT_EMERGENCY,
                CAT_THROAT,
                CAT_BASIC,
                CAT_ENV,
                CAT_REPLY,
                CAT_CUSTOM
        );
    }

    public static List<PhraseItem> getBuiltInPhrases() {
        List<PhraseItem> list = new ArrayList<>();

        list.add(new PhraseItem("e1", "請幫我叫護理師", CAT_EMERGENCY, true, false));
        list.add(new PhraseItem("e2", "脖子覺得緊緊的／腫脹", CAT_EMERGENCY, true, false));
        list.add(new PhraseItem("e3", "呼吸有點喘／吸不到氣", CAT_EMERGENCY, true, false));
        list.add(new PhraseItem("e4", "手腳麻麻的／嘴角發麻", CAT_EMERGENCY, true, false));
        list.add(new PhraseItem("e5", "傷口很痛／需要止痛", CAT_EMERGENCY, false, false));
        list.add(new PhraseItem("e6", "引流管不舒服／拉扯到了", CAT_EMERGENCY, false, false));

        list.add(new PhraseItem("t1", "喉嚨很乾／想喝一點水", CAT_THROAT, false, false));
        list.add(new PhraseItem("t2", "喉嚨有痰／想清喉嚨", CAT_THROAT, false, false));
        list.add(new PhraseItem("t3", "想冰敷脖子", CAT_THROAT, false, false));
        list.add(new PhraseItem("t4", "頭暈／有點噁心想吐", CAT_THROAT, false, false));

        list.add(new PhraseItem("b1", "我想尿尿", CAT_BASIC, false, false));
        list.add(new PhraseItem("b2", "我想大便", CAT_BASIC, false, false));
        list.add(new PhraseItem("b3", "肚子餓了／想吃東西", CAT_BASIC, false, false));
        list.add(new PhraseItem("b4", "幫我擦臉／擦身體", CAT_BASIC, false, false));
        list.add(new PhraseItem("b5", "我想吃安眠藥", CAT_BASIC, false, false));

        list.add(new PhraseItem("v1", "手機需要充電", CAT_ENV, false, false));
        list.add(new PhraseItem("v2", "太冷了／幫我蓋被子", CAT_ENV, false, false));
        list.add(new PhraseItem("v3", "太熱了／調低冷氣", CAT_ENV, false, false));
        list.add(new PhraseItem("v4", "幫我調高床頭（半坐臥）", CAT_ENV, false, false));
        list.add(new PhraseItem("v5", "幫我調低床頭／想躺平", CAT_ENV, false, false));
        list.add(new PhraseItem("v6", "我想換個姿勢／翻身", CAT_ENV, false, false));
        list.add(new PhraseItem("v7", "我想睡覺／請關燈", CAT_ENV, false, false));

        list.add(new PhraseItem("r1", "好／可以", CAT_REPLY, false, false));
        list.add(new PhraseItem("r2", "不要／不用", CAT_REPLY, false, false));
        list.add(new PhraseItem("r3", "好很多了，謝謝", CAT_REPLY, false, false));
        list.add(new PhraseItem("r4", "等一下", CAT_REPLY, false, false));

        return list;
    }

    public static List<PhraseItem> getCustomPhrases(Context context) {
        List<PhraseItem> result = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_CUSTOM_PHRASES, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.optString("id", UUID.randomUUID().toString());
                String text = obj.optString("text", "").trim();
                if (!text.isEmpty()) {
                    result.add(new PhraseItem(id, text, CAT_CUSTOM, false, true));
                }
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public static void addCustomPhrase(Context context, String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.isEmpty()) return;

        List<PhraseItem> existing = getCustomPhrases(context);
        existing.add(0, new PhraseItem(UUID.randomUUID().toString(), cleaned, CAT_CUSTOM, false, true));
        saveCustomPhrases(context, existing);
    }

    public static void deleteCustomPhrase(Context context, String id) {
        List<PhraseItem> existing = getCustomPhrases(context);
        List<PhraseItem> updated = new ArrayList<>();
        for (PhraseItem item : existing) {
            if (!item.id.equals(id)) {
                updated.add(item);
            }
        }
        saveCustomPhrases(context, updated);
    }

    private static void saveCustomPhrases(Context context, List<PhraseItem> items) {
        JSONArray array = new JSONArray();
        for (PhraseItem item : items) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("text", item.text);
                array.put(obj);
            } catch (JSONException ignored) {
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CUSTOM_PHRASES, array.toString()).apply();
    }

    public static boolean isMuted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_MUTED, false);
    }

    public static void setMuted(Context context, boolean muted) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_MUTED, muted).apply();
    }
}