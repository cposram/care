package com.warmcare.thyroidtalk;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout categoryChipContainer;
    private LinearLayout phraseSectionsContainer;
    private LinearLayout fullScreenOverlay;
    private TextView tvOverlayCategory;
    private TextView tvOverlayPhrase;
    private Button btnToggleMute;
    private Button btnDeleteCustomOverlay;

    private SoundSpeakerHelper speakerHelper;
    private String selectedCategory = PhraseStorage.CAT_ALL;
    private PhraseItem activeOverlayItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speakerHelper = new SoundSpeakerHelper(this);

        categoryChipContainer = findViewById(R.id.categoryChipContainer);
        phraseSectionsContainer = findViewById(R.id.phraseSectionsContainer);
        fullScreenOverlay = findViewById(R.id.fullScreenOverlay);
        tvOverlayCategory = findViewById(R.id.tvOverlayCategory);
        tvOverlayPhrase = findViewById(R.id.tvOverlayPhrase);
        btnToggleMute = findViewById(R.id.btnToggleMute);
        btnDeleteCustomOverlay = findViewById(R.id.btnDeleteCustomOverlay);

        Button btnOpenHandwriting = findViewById(R.id.btnOpenHandwriting);
        Button btnAddCustomPhrase = findViewById(R.id.btnAddCustomPhrase);
        Button btnSpeakAgain = findViewById(R.id.btnSpeakAgain);
        Button btnCloseOverlay = findViewById(R.id.btnCloseOverlay);

        updateMuteButtonUI();

        btnToggleMute.setOnClickListener(v -> {
            boolean newMute = !PhraseStorage.isMuted(this);
            PhraseStorage.setMuted(this, newMute);
            updateMuteButtonUI();
            Toast.makeText(
                    this,
                    newMute ? "已切換為靜音模式（僅顯示特大字卡）" : "已開啟提示音與語音朗讀",
                    Toast.LENGTH_SHORT
            ).show();
        });

        btnOpenHandwriting.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HandwritingActivity.class);
            startActivity(intent);
        });

        btnAddCustomPhrase.setOnClickListener(v -> showAddCustomPhraseDialog());

        btnSpeakAgain.setOnClickListener(v -> {
            if (activeOverlayItem != null) {
                speakerHelper.speakOnly(activeOverlayItem.text);
            }
        });

        btnDeleteCustomOverlay.setOnClickListener(v -> {
            if (activeOverlayItem != null && activeOverlayItem.isCustom) {
                PhraseStorage.deleteCustomPhrase(this, activeOverlayItem.id);
                fullScreenOverlay.setVisibility(View.GONE);
                activeOverlayItem = null;
                renderPhrases();
                Toast.makeText(this, "已刪除該常用句", Toast.LENGTH_SHORT).show();
            }
        });

        btnCloseOverlay.setOnClickListener(v -> {
            fullScreenOverlay.setVisibility(View.GONE);
            activeOverlayItem = null;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (fullScreenOverlay.getVisibility() == View.VISIBLE) {
                    fullScreenOverlay.setVisibility(View.GONE);
                    activeOverlayItem = null;
                } else {
                    finish();
                }
            }
        });

        renderCategoryChips();
        renderPhrases();
    }

    private void updateMuteButtonUI() {
        boolean muted = PhraseStorage.isMuted(this);
        if (muted) {
            btnToggleMute.setText("🔇 靜音模式");
            btnToggleMute.setTextColor(Color.parseColor("#D94E4E"));
        } else {
            btnToggleMute.setText("🔊 聲音開啟");
            btnToggleMute.setTextColor(Color.parseColor("#2B2623"));
        }
    }

    private void renderCategoryChips() {
        categoryChipContainer.removeAllViews();
        List<String> categories = PhraseStorage.getCategories();

        for (String cat : categories) {
            TextView chip = new TextView(this);
            chip.setText(cat);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(16), dp(10), dp(16), dp(10));

            boolean isSelected = cat.equals(selectedCategory);
            chip.setBackgroundResource(isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chip.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#2B2623"));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                selectedCategory = cat;
                renderCategoryChips();
                renderPhrases();
            });

            categoryChipContainer.addView(chip);
        }
    }

    private void renderPhrases() {
        phraseSectionsContainer.removeAllViews();

        List<PhraseItem> allItems = new ArrayList<>();
        allItems.addAll(PhraseStorage.getCustomPhrases(this));
        allItems.addAll(PhraseStorage.getBuiltInPhrases());

        List<String> targetCategories = new ArrayList<>();
        if (PhraseStorage.CAT_ALL.equals(selectedCategory)) {
            targetCategories.addAll(PhraseStorage.getSectionCategories());
        } else {
            targetCategories.add(selectedCategory);
        }

        for (String cat : targetCategories) {
            List<PhraseItem> catItems = new ArrayList<>();
            for (PhraseItem item : allItems) {
                if (cat.equals(item.category)) {
                    catItems.add(item);
                }
            }

            if (PhraseStorage.CAT_CUSTOM.equals(cat)) {
                addSectionHeader(cat);
                if (catItems.isEmpty()) {
                    addEmptyCustomHintCard();
                } else {
                    addCardsGrid(catItems);
                }
                continue;
            }

            if (catItems.isEmpty()) {
                continue;
            }

            addSectionHeader(cat);
            addCardsGrid(catItems);
        }
    }

    private void addSectionHeader(String categoryTitle) {
        TextView header = new TextView(this);
        header.setText(categoryTitle);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        header.setTypeface(null, Typeface.BOLD);
        header.setTextColor(getCategoryAccentColor(categoryTitle));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(12);
        lp.bottomMargin = dp(8);
        lp.leftMargin = dp(4);
        header.setLayoutParams(lp);

        phraseSectionsContainer.addView(header);
    }

    private void addEmptyCustomHintCard() {
        TextView hintCard = new TextView(this);
        hintCard.setText("點上方「➕ 新增常用句」可隨時幫媽媽加入專屬需求（例如：拿眼鏡、找手機）");
        hintCard.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        hintCard.setTextColor(Color.parseColor("#6B615A"));
        hintCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        hintCard.setBackground(createCardDrawable(Color.parseColor("#F8F2FB"), Color.parseColor("#C9B2D6")));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dp(8);
        hintCard.setLayoutParams(lp);
        hintCard.setOnClickListener(v -> showAddCustomPhraseDialog());

        phraseSectionsContainer.addView(hintCard);
    }

    private void addCardsGrid(List<PhraseItem> items) {
        for (int i = 0; i < items.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowLp.bottomMargin = dp(10);
            row.setLayoutParams(rowLp);

            View leftCard = createPhraseCard(items.get(i));
            LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
            );
            leftLp.setMarginEnd(dp(5));
            leftCard.setLayoutParams(leftLp);
            row.addView(leftCard);

            if (i + 1 < items.size()) {
                View rightCard = createPhraseCard(items.get(i + 1));
                LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                );
                rightLp.setMarginStart(dp(5));
                rightCard.setLayoutParams(rightLp);
                row.addView(rightCard);
            } else {
                View spacer = new View(this);
                LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                );
                spacerLp.setMarginStart(dp(5));
                spacer.setLayoutParams(spacerLp);
                row.addView(spacer);
            }

            phraseSectionsContainer.addView(row);
        }
    }

    private View createPhraseCard(PhraseItem item) {
        TextView card = new TextView(this);
        card.setText(item.text);
        card.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f);
        card.setTypeface(null, Typeface.BOLD);
        card.setTextColor(Color.parseColor("#2B2623"));
        card.setGravity(Gravity.CENTER);
        card.setMinHeight(dp(86));
        card.setLineSpacing(0f, 1.15f);
        card.setPadding(dp(12), dp(14), dp(12), dp(14));

        int fillColor = getCategoryFillColor(item.category);
        int strokeColor = getCategoryAccentColor(item.category);
        card.setBackground(createCardDrawable(fillColor, strokeColor));

        card.setOnClickListener(v -> openFullScreenCard(item));

        return card;
    }

    private void openFullScreenCard(PhraseItem item) {
        activeOverlayItem = item;
        tvOverlayCategory.setText(item.category);
        tvOverlayCategory.setTextColor(getCategoryAccentColor(item.category));
        tvOverlayPhrase.setText(item.text);

        int len = item.text.length();
        float sizeSp;
        if (len <= 6) {
            sizeSp = 64f;
        } else if (len <= 11) {
            sizeSp = 52f;
        } else {
            sizeSp = 44f;
        }
        tvOverlayPhrase.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);

        if (item.isEmergency) {
            fullScreenOverlay.setBackgroundColor(Color.parseColor("#FFF5F5"));
        } else {
            fullScreenOverlay.setBackgroundColor(Color.WHITE);
        }

        btnDeleteCustomOverlay.setVisibility(item.isCustom ? View.VISIBLE : View.GONE);
        fullScreenOverlay.setVisibility(View.VISIBLE);

        speakerHelper.announcePhrase(item.text, item.isEmergency);
    }

    private void showAddCustomPhraseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("➕ 新增媽媽的常用句");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(12), dp(20), dp(8));

        final EditText input = new EditText(this);
        input.setHint("例如：幫我拿眼鏡、我想看手機時間");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        input.setPadding(dp(14), dp(14), dp(14), dp(14));
        input.setBackgroundResource(R.drawable.bg_btn_secondary);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("儲存按鍵", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                PhraseStorage.addCustomPhrase(this, text);
                renderPhrases();
                Toast.makeText(this, "已加入「⭐ 我的常用句」", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private GradientDrawable createCardDrawable(int fillColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setStroke(dp(2), strokeColor);
        drawable.setCornerRadius(dp(16));
        return drawable;
    }

    private int getCategoryAccentColor(String category) {
        switch (category) {
            case PhraseStorage.CAT_EMERGENCY:
                return Color.parseColor("#D94E4E");
            case PhraseStorage.CAT_THROAT:
                return Color.parseColor("#2B7A78");
            case PhraseStorage.CAT_BASIC:
                return Color.parseColor("#C86D2D");
            case PhraseStorage.CAT_ENV:
                return Color.parseColor("#4A6FA5");
            case PhraseStorage.CAT_REPLY:
                return Color.parseColor("#4F772D");
            case PhraseStorage.CAT_CUSTOM:
                return Color.parseColor("#7B4B94");
            default:
                return Color.parseColor("#3D3632");
        }
    }

    private int getCategoryFillColor(String category) {
        switch (category) {
            case PhraseStorage.CAT_EMERGENCY:
                return Color.parseColor("#FFF0F0");
            case PhraseStorage.CAT_THROAT:
                return Color.parseColor("#EEF8F7");
            case PhraseStorage.CAT_BASIC:
                return Color.parseColor("#FFF7ED");
            case PhraseStorage.CAT_ENV:
                return Color.parseColor("#F0F5FC");
            case PhraseStorage.CAT_REPLY:
                return Color.parseColor("#F2F8EE");
            case PhraseStorage.CAT_CUSTOM:
                return Color.parseColor("#F8F2FB");
            default:
                return Color.WHITE;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speakerHelper != null) {
            speakerHelper.shutdown();
        }
    }
}