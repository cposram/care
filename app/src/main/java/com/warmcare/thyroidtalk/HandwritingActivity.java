package com.warmcare.thyroidtalk;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class HandwritingActivity extends AppCompatActivity {

    private HandwritingView handwritingView;
    private EditText etFreeInput;
    private LinearLayout typedFullScreenOverlay;
    private TextView tvTypedOverlayText;
    private SoundSpeakerHelper speakerHelper;
    private String currentSpokenText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handwriting);

        speakerHelper = new SoundSpeakerHelper(this);

        handwritingView = findViewById(R.id.handwritingView);
        etFreeInput = findViewById(R.id.etFreeInput);
        typedFullScreenOverlay = findViewById(R.id.typedFullScreenOverlay);
        tvTypedOverlayText = findViewById(R.id.tvTypedOverlayText);

        Button btnBackHome = findViewById(R.id.btnBackHome);
        Button btnToggleStroke = findViewById(R.id.btnToggleStroke);
        Button btnUndoStroke = findViewById(R.id.btnUndoStroke);
        Button btnClearCanvas = findViewById(R.id.btnClearCanvas);
        Button btnClearText = findViewById(R.id.btnClearText);
        Button btnSpeakTypedText = findViewById(R.id.btnSpeakTypedText);
        Button btnTypedSpeakAgain = findViewById(R.id.btnTypedSpeakAgain);
        Button btnCloseTypedOverlay = findViewById(R.id.btnCloseTypedOverlay);

        btnBackHome.setOnClickListener(v -> finish());

        btnToggleStroke.setOnClickListener(v -> {
            boolean isThick = handwritingView.toggleThickStroke();
            btnToggleStroke.setText(isThick ? "🖊️ 特粗筆" : "🖊️ 粗筆");
        });

        btnUndoStroke.setOnClickListener(v -> handwritingView.undoLastStroke());

        btnClearCanvas.setOnClickListener(v -> handwritingView.clearCanvas());

        btnClearText.setOnClickListener(v -> etFreeInput.setText(""));

        btnSpeakTypedText.setOnClickListener(v -> {
            String input = etFreeInput.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "請先輸入想說的文字喔！", Toast.LENGTH_SHORT).show();
                return;
            }
            hideKeyboard();
            showFullScreenText(input);
        });

        btnTypedSpeakAgain.setOnClickListener(v -> {
            if (!currentSpokenText.isEmpty()) {
                speakerHelper.speakOnly(currentSpokenText);
            }
        });

        btnCloseTypedOverlay.setOnClickListener(v -> typedFullScreenOverlay.setVisibility(View.GONE));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (typedFullScreenOverlay.getVisibility() == View.VISIBLE) {
                    typedFullScreenOverlay.setVisibility(View.GONE);
                } else {
                    finish();
                }
            }
        });
    }

    private void showFullScreenText(String text) {
        currentSpokenText = text;
        tvTypedOverlayText.setText(text);

        int len = text.length();
        float sizeSp;
        if (len <= 6) {
            sizeSp = 64f;
        } else if (len <= 14) {
            sizeSp = 50f;
        } else if (len <= 26) {
            sizeSp = 40f;
        } else {
            sizeSp = 32f;
        }
        tvTypedOverlayText.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);

        typedFullScreenOverlay.setVisibility(View.VISIBLE);
        speakerHelper.announcePhrase(text, false);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speakerHelper != null) {
            speakerHelper.shutdown();
        }
    }
}