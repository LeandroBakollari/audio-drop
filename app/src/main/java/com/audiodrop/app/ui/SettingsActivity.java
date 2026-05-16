package com.audiodrop.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;

import com.audiodrop.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends BaseActivity {
    private static final int MAX_SLEEP_TIMER_MINUTES = 180;

    private TextView sleepTimerLabel;
    private Slider sleepTimerSlider;
    private TextInputEditText sleepTimerMinutesEdit;
    private MaterialButton startSleepTimerButton;
    private boolean updatingTimerControls = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupBottomNavigation(SettingsActivity.class);

        SwitchMaterial darkModeSwitch = findViewById(R.id.darkModeSwitch);
        darkModeSwitch.setChecked(viewModel.isDarkModeEnabled());
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setDarkModeEnabled(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        sleepTimerLabel = findViewById(R.id.sleepTimerLabel);
        sleepTimerSlider = findViewById(R.id.sleepTimerSlider);
        sleepTimerMinutesEdit = findViewById(R.id.sleepTimerMinutesEdit);
        startSleepTimerButton = findViewById(R.id.startSleepTimerButton);

        int currentMinutes = clampMinutes(viewModel.getSleepTimerMinutes());
        if (currentMinutes != viewModel.getSleepTimerMinutes()) {
            viewModel.setSleepTimerMinutes(currentMinutes);
        }
        updateTimerControls(currentMinutes);
        sleepTimerSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (updatingTimerControls || !fromUser) {
                return;
            }
            int minutes = Math.round(value);
            setSleepTimerMinutes(minutes);
        });

        sleepTimerMinutesEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (updatingTimerControls) {
                    return;
                }
                String value = s == null ? "" : s.toString().trim();
                try {
                    int minutes = value.isEmpty() ? 0 : clampMinutes(Integer.parseInt(value));
                    setSleepTimerMinutes(minutes);
                } catch (NumberFormatException ignored) {
                    setSleepTimerMinutes(MAX_SLEEP_TIMER_MINUTES);
                }
            }
        });

        findViewById(R.id.sleepTimerMinusButton).setOnClickListener(v ->
                setSleepTimerMinutes(viewModel.getSleepTimerMinutes() - 5)
        );
        findViewById(R.id.sleepTimerPlusButton).setOnClickListener(v ->
                setSleepTimerMinutes(viewModel.getSleepTimerMinutes() + 5)
        );
        startSleepTimerButton.setOnClickListener(v -> {
            playbackController.startSleepTimer();
            updateTimerControls(viewModel.getSleepTimerMinutes());
        });
    }

    private void updateSleepTimerLabel(int minutes) {
        if (minutes == 0) {
            sleepTimerLabel.setText("Sleep timer: Off");
        } else if (playbackController.isSleepTimerActive()) {
            sleepTimerLabel.setText("Sleep timer: running for " + minutes + " min");
        } else {
            sleepTimerLabel.setText("Sleep timer: " + minutes + " min selected");
        }
    }

    private void setSleepTimerMinutes(int minutes) {
        int clamped = clampMinutes(minutes);
        viewModel.setSleepTimerMinutes(clamped);
        playbackController.cancelSleepTimer();
        updateTimerControls(clamped);
    }

    private void updateTimerControls(int minutes) {
        updatingTimerControls = true;
        sleepTimerSlider.setValue(minutes);
        sleepTimerMinutesEdit.setText(String.valueOf(minutes));
        sleepTimerMinutesEdit.setSelection(sleepTimerMinutesEdit.getText() == null ? 0 : sleepTimerMinutesEdit.getText().length());
        updateSleepTimerLabel(minutes);
        startSleepTimerButton.setEnabled(minutes > 0);
        startSleepTimerButton.setText(playbackController.isSleepTimerActive() ? "Restart timer" : "Start timer");
        updatingTimerControls = false;
    }

    private int clampMinutes(int minutes) {
        return Math.max(0, Math.min(minutes, MAX_SLEEP_TIMER_MINUTES));
    }
}
