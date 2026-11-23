package com.warmcare.thyroidtalk;

public class PhraseItem {
    public final String id;
    public final String text;
    public final String category;
    public final boolean isEmergency;
    public final boolean isCustom;

    public PhraseItem(String id, String text, String category, boolean isEmergency, boolean isCustom) {
        this.id = id;
        this.text = text;
        this.category = category;
        this.isEmergency = isEmergency;
        this.isCustom = isCustom;
    }
}