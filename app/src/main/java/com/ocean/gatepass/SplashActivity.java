package com.ocean.gatepass;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_splash);

        ImageView logo       = findViewById(R.id.splash_logo);
        TextView  appName    = findViewById(R.id.splash_app_name);
        TextView  tagline    = findViewById(R.id.splash_tagline);
        View      progress   = findViewById(R.id.splash_progress);

        // Logo pop-in
        ObjectAnimator sx = ObjectAnimator.ofFloat(logo, "scaleX", 0f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(logo, "scaleY", 0f, 1f);
        ObjectAnimator fa = ObjectAnimator.ofFloat(logo, "alpha",  0f, 1f);
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(sx, sy, fa);
        logoSet.setDuration(700);
        logoSet.setInterpolator(new AccelerateDecelerateInterpolator());
        logoSet.start();

        // Text fade-ins
        fadeIn(appName,  500,  600);
        fadeIn(tagline,  800,  600);
        fadeIn(progress, 1000, 400);

        // Go to MainActivity after 2.8 s
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 2800);
    }

    private void fadeIn(View v, long delay, long duration) {
        v.setAlpha(0f);
        v.animate().alpha(1f).setStartDelay(delay).setDuration(duration).start();
    }
}
