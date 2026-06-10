package com.example.agrirent.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.agrirent.R;
import com.example.agrirent.models.SubscriptionOrder;
import com.example.agrirent.models.SubscriptionPlan;
import com.example.agrirent.models.SubscriptionPlansResponse;
import com.example.agrirent.models.VerifyPaymentRequest;
import com.example.agrirent.models.VerifyPaymentResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.razorpay.Checkout;
import com.razorpay.PaymentData;
import com.razorpay.PaymentResultWithDataListener;

import org.json.JSONObject;

import java.util.List;

import com.google.firebase.messaging.FirebaseMessaging;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Premium subscription plans screen.
 *
 * Flow:
 *  1. GET  /api/subscription-plans         (public)
 *  2. Custom BottomSheet confirm dialog
 *  3. POST /api/subscription/create-order/{planId}  (auth)
 *  4. Step-tracker loading dialog
 *  5. Razorpay Checkout
 *  6. POST /api/subscription/verify-payment         (auth)
 *  7. Success celebration dialog
 */
public class SubscriptionActivity extends BaseActivity
        implements PaymentResultWithDataListener {

    // ── state ──────────────────────────────────────────────────────────────
    private int    selectedPlanId    = -1;
    private double selectedPlanPrice = 0;
    private String selectedPlanName  = "";
    private String pendingOrderId    = null;

    // ── views ──────────────────────────────────────────────────────────────
    private LinearLayout llLoading, llPlans, llEmpty, llError;
    private TextView     tvErrorMsg;
    private MaterialButton btnRetry;

    // ── dialogs ────────────────────────────────────────────────────────────
    private Dialog      loadingDialog;
    private TextView    tvLoadingTitle, tvLoadingSubtitle;
    private View        tvStep2Num, tvStep3Num, connector1, connector2;

    // ── deps ───────────────────────────────────────────────────────────────
    private SessionManager session;
    private ApiService     api;

    // ══════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        session = new SessionManager(this);
        api     = ApiClient.getClient(this).create(ApiService.class);

        Checkout.preload(getApplicationContext());

        bindViews();
        animateSheetIn();
        loadPlans();
    }

    // ══════════════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════════════

    private void bindViews() {
        llLoading  = findViewById(R.id.llLoading);
        llPlans    = findViewById(R.id.llPlans);
        llEmpty    = findViewById(R.id.llEmpty);
        llError    = findViewById(R.id.llError);
        tvErrorMsg = findViewById(R.id.tvError);
        btnRetry   = findViewById(R.id.btnRetry);

        View ivBack = findViewById(R.id.ivBack);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        TextView tvMySubs = findViewById(R.id.tvMySubscriptions);
        if (tvMySubs != null) {
            tvMySubs.setOnClickListener(v ->
                    startActivity(new Intent(this, MySubscriptionsActivity.class)));
        }

        if (btnRetry != null) btnRetry.setOnClickListener(v -> loadPlans());
    }

    /** Slide the white bottom sheet up from below when screen opens */
    private void animateSheetIn() {
        View sheet = findViewById(R.id.llSheet);
        if (sheet == null) return;
        sheet.setTranslationY(600f);
        sheet.setAlpha(0f);
        sheet.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .start();
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 1 — Load plans
    // ══════════════════════════════════════════════════════════════════════

    private void loadPlans() {
        setState("loading");

        api.getSubscriptionPlans().enqueue(new Callback<SubscriptionPlansResponse>() {
            @Override
            public void onResponse(Call<SubscriptionPlansResponse> call,
                                   Response<SubscriptionPlansResponse> res) {
                if (res.isSuccessful() && res.body() != null
                        && res.body().getData() != null
                        && !res.body().getData().isEmpty()) {
                    buildPlanCards(res.body().getData());
                    setState("plans");
                } else {
                    setState("empty");
                }
            }

            @Override
            public void onFailure(Call<SubscriptionPlansResponse> call, Throwable t) {
                if (tvErrorMsg != null)
                    tvErrorMsg.setText("Network error. Please try again.");
                setState("error");
            }
        });
    }

    private void setState(String s) {
        runOnUiThread(() -> {
            llLoading.setVisibility("loading".equals(s) ? View.VISIBLE : View.GONE);
            llPlans.setVisibility("plans".equals(s)     ? View.VISIBLE : View.GONE);
            llEmpty.setVisibility("empty".equals(s)     ? View.VISIBLE : View.GONE);
            llError.setVisibility("error".equals(s)     ? View.VISIBLE : View.GONE);
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // BUILD PLAN CARDS
    // ══════════════════════════════════════════════════════════════════════

    private void buildPlanCards(List<SubscriptionPlan> plans) {
        llPlans.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);

        for (int i = 0; i < plans.size(); i++) {
            View card = inf.inflate(R.layout.item_subscription_plan, llPlans, false);
            bindCard(card, plans.get(i), i == 1);
            // Animate cards in with a stagger
            final int delay = i * 100;
            card.setAlpha(0f);
            card.setTranslationY(40f);
            card.animate().alpha(1f).translationY(0f)
                    .setStartDelay(delay).setDuration(350).start();
            llPlans.addView(card);
        }
    }

    private void bindCard(View card, SubscriptionPlan plan, boolean popular) {
        // ── Popular card: dark green background, white text ──
        MaterialCardView mcv = card.findViewById(R.id.planCard);
        if (mcv != null && popular) {
            mcv.setCardBackgroundColor(Color.parseColor("#0D2A14"));
            mcv.setCardElevation(dp(12));
        }

        int textDark    = popular ? Color.WHITE              : Color.parseColor("#1A2E1A");
        int textSub     = popular ? Color.parseColor("#AAFFCC") : Color.parseColor("#888888");
        int priceColor  = popular ? Color.parseColor("#69F0AE") : Color.parseColor("#2E7D32");
        int priceSub    = popular ? Color.parseColor("#66FFFFFF") : Color.parseColor("#AAAAAA");

        // Popular badge
        TextView badge = card.findViewById(R.id.tvBestBadge);
        if (badge != null && popular) {
            badge.setVisibility(View.VISIBLE);
            badge.setText("⭐ BEST VALUE");
            badge.setTextColor(popular ? Color.parseColor("#FFD700")
                    : Color.parseColor("#1B5E20"));
        }

        // Plan name
        setTvColor(card, R.id.tvPlanName, plan.getPlanName(), textDark);

        // Duration
        setTvColor(card, R.id.tvDuration, "📅  " + plan.getDurationDays() + " days validity", textSub);

        // ── Feature icon circles: use white bg for popular, green for normal ──
        updateFeatureCircles(card, popular);

        // Equipment row
        View rowEq = card.findViewById(R.id.rowEquipment);
        if (rowEq != null && plan.getMaxEquipment() > 0) {
            rowEq.setVisibility(View.VISIBLE);
            setTvColor(card, R.id.tvEquipmentFeature,
                    "List up to " + plan.getMaxEquipment() + " equipment", textDark);
        }

        // Products row
        View rowProd = card.findViewById(R.id.rowProducts);
        if (rowProd != null && plan.getMaxProducts() > 0) {
            rowProd.setVisibility(View.VISIBLE);
            setTvColor(card, R.id.tvProductsFeature,
                    "Sell up to " + plan.getMaxProducts() + " products", textDark);
        }

        // Price
        setTvColor(card, R.id.tvPrice, "₹" + String.format("%.0f", plan.getPrice()), priceColor);
        setTvColor(card, R.id.tvPriceLabel,
                "one-time for " + plan.getDurationDays() + " days", priceSub);

        // Subscribe button
        MaterialButton btn = card.findViewById(R.id.btnSubscribe);
        if (btn != null) {
            if (popular) {
                btn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#69F0AE")));
                btn.setTextColor(Color.parseColor("#0D2A14"));
            }
            final int pid   = plan.getPlanId();
            final double amt = plan.getPrice();
            final String nm  = plan.getPlanName();
            final int dur    = plan.getDurationDays();
            btn.setOnClickListener(v -> onSubscribeClicked(pid, amt, nm, dur));
        }
    }

    private void updateFeatureCircles(View card, boolean popular) {
        // For popular (dark) cards, the green circles should be semi-transparent white
        int[] circleIds = {
                // FrameLayouts containing the emoji
        };
        // We set it via alpha on the FrameLayout backgrounds
        if (popular) {
            // Rows are already set with bg_success_circle which is green.
            // For dark card, overlay them with 30% white alpha effect via code is complex.
            // We'll just keep the green circles — they look fine on dark.
        }
    }

    private void setTvColor(View root, int id, String text, int color) {
        TextView tv = root.findViewById(id);
        if (tv != null) {
            tv.setText(text);
            tv.setTextColor(color);
        }
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 2 — Custom confirm bottom-sheet dialog
    // ══════════════════════════════════════════════════════════════════════

    private void onSubscribeClicked(int planId, double price, String planName, int days) {
        if (session.getToken() == null || session.getToken().isEmpty()) {
            Toast.makeText(this, "Please login first to subscribe.", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedPlanId    = planId;
        selectedPlanPrice = price;
        selectedPlanName  = planName;

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_confirm_subscription, null);

        // Fill in details
        setText(view, R.id.tvConfirmPlanName, planName);
        setText(view, R.id.tvConfirmAmount, "₹" + String.format("%.0f", price));
        setText(view, R.id.tvConfirmDuration, days + " days");

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().getAttributes().windowAnimations =
                android.R.style.Animation_InputMethod; // slide up from bottom
        dialog.setCancelable(true);

        view.findViewById(R.id.btnConfirmCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirmPay).setOnClickListener(v -> {
            dialog.dismiss();
            createOrder(planId);
        });

        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 3 — Create order
    // ══════════════════════════════════════════════════════════════════════

    private void createOrder(int planId) {
        showLoadingDialog(1); // Step 1: Creating order

        api.createSubscriptionOrder(planId).enqueue(new Callback<SubscriptionOrder>() {
            @Override
            public void onResponse(Call<SubscriptionOrder> call,
                                   Response<SubscriptionOrder> res) {
                if (res.isSuccessful() && res.body() != null) {
                    pendingOrderId = res.body().getOrderId();
                    // Step 2 lights up — opening Razorpay
                    advanceLoadingStep(2, "Opening Payment", "Launching Razorpay secure checkout…");
                    // Small delay so user sees step 2 before Razorpay opens
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        hideLoadingDialog();
                        openCheckout(res.body());
                    }, 500);
                } else {
                    hideLoadingDialog();
                    String msg = res.code() == 401 ? "Session expired. Please login again."
                               : res.code() == 404 ? "Plan not found."
                               : "Failed to create order (HTTP " + res.code() + ").";
                    Toast.makeText(SubscriptionActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SubscriptionOrder> call, Throwable t) {
                hideLoadingDialog();
                Toast.makeText(SubscriptionActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 4 — Open Razorpay
    // ══════════════════════════════════════════════════════════════════════

    private void openCheckout(SubscriptionOrder order) {
        try {
            Checkout checkout = new Checkout();
            String key = (order.getKey() != null && !order.getKey().isEmpty())
                    ? order.getKey()
                    : getString(R.string.razorpay_key_id);
            checkout.setKeyID(key);

            JSONObject opts = new JSONObject();
            opts.put("name",        "AgriRent");
            opts.put("description", selectedPlanName);
            opts.put("order_id",    order.getOrderId());
            opts.put("currency",    "INR");
            opts.put("amount",      (long)(order.getAmount() * 100));

            JSONObject prefill = new JSONObject();
            prefill.put("name", session.getUserName() != null ? session.getUserName() : "");
            opts.put("prefill", prefill);

            JSONObject theme = new JSONObject();
            theme.put("color", "#1B5E20");
            opts.put("theme", theme);

            checkout.open(this, opts);
        } catch (Exception e) {
            Toast.makeText(this, "Payment init error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 5a — Razorpay SUCCESS
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void onPaymentSuccess(String paymentId, PaymentData paymentData) {
        String signature = "";
        String orderId   = pendingOrderId != null ? pendingOrderId : "";

        if (paymentData != null) {
            String sig = paymentData.getSignature();
            if (sig != null) signature = sig;
            String oid = paymentData.getOrderId();
            if (oid != null && !oid.isEmpty()) orderId = oid;
        }

        if (orderId.isEmpty()) {
            Toast.makeText(this,
                    "Order lost. Contact support with payment ID: " + paymentId,
                    Toast.LENGTH_LONG).show();
            return;
        }

        showLoadingDialog(3); // Step 3: Activating
        verifyPayment(paymentId, orderId, signature);
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 5b — Razorpay FAILURE
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void onPaymentError(int code, String response, PaymentData paymentData) {
        hideLoadingDialog();
        String msg = code == Checkout.NETWORK_ERROR
                ? "Network error during payment. Please try again."
                : "Payment cancelled or failed.";
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // ══════════════════════════════════════════════════════════════════════
    // STEP 6 — Verify payment
    // ══════════════════════════════════════════════════════════════════════

    private void verifyPayment(String paymentId, String orderId, String signature) {
        VerifyPaymentRequest body = new VerifyPaymentRequest(
                selectedPlanId, paymentId, orderId, signature);

        api.verifyPayment(body).enqueue(new Callback<VerifyPaymentResponse>() {
            @Override
            public void onResponse(Call<VerifyPaymentResponse> call,
                                   Response<VerifyPaymentResponse> res) {
                hideLoadingDialog();

                if (res.isSuccessful() && res.body() != null && res.body().isSuccess()) {
                    VerifyPaymentResponse result = res.body();
                    if (result.getToken() != null && !result.getToken().isEmpty()) {
                        session.saveAuth(result.getToken(), session.getRefreshToken());
                    }
                    if (result.getRoles() != null && !result.getRoles().isEmpty()) {
                        session.saveUserRoles(result.getRoles());
                    }

                    // Register FCM token now (in case user didn't re-login)
                    String authToken = session.getToken();
                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String fcmToken = task.getResult();
                            String language = session.getLanguage();
                            String deviceId = android.provider.Settings.Secure.getString(
                                    getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                            api.updateFcmToken(new com.example.agrirent.models.FcmTokenRequest(fcmToken, language, deviceId)).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    Log.d("FCM", "Token registered from subscription. Code: " + response.code());
                                }
                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Log.e("FCM", "Token upload failed: " + t.getMessage());
                                }
                            });
                        }
                    });

                    showSuccessDialog(result);
                } else {
                    String msg = res.code() == 400
                            ? "Verification failed. Contact support with: " + paymentId
                            : res.code() == 401 ? "Session expired."
                            : "Verification error (HTTP " + res.code() + ").";
                    Toast.makeText(SubscriptionActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<VerifyPaymentResponse> call, Throwable t) {
                hideLoadingDialog();
                Toast.makeText(SubscriptionActivity.this,
                        "Network error during verification. Contact support: " + paymentId,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // SUCCESS DIALOG — animated checkmark + dates
    // ══════════════════════════════════════════════════════════════════════

    private void showSuccessDialog(VerifyPaymentResponse result) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_success_subscription, null);

        setText(view, R.id.tvSuccessMessage,
                "\"" + selectedPlanName + "\" is now active on your account.");
        setText(view, R.id.tvSuccessFrom, formatDate(result.getValidFrom()));
        setText(view, R.id.tvSuccessTill, formatDate(result.getValidTill()));

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().getAttributes().windowAnimations =
                android.R.style.Animation_InputMethod;
        dialog.setCancelable(false);

        // Animate checkmark circle with bounce
        FrameLayout circle = view.findViewById(R.id.circleSuccess);
        if (circle != null) {
            circle.setScaleX(0f);
            circle.setScaleY(0f);
            circle.animate().scaleX(1f).scaleY(1f)
                    .setDuration(500)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .start();
        }

        view.findViewById(R.id.btnSuccessView).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, MySubscriptionsActivity.class));
            finish();
        });
        view.findViewById(R.id.tvSuccessLater).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    // ══════════════════════════════════════════════════════════════════════
    // LOADING DIALOG (step tracker)
    // ══════════════════════════════════════════════════════════════════════

    private void showLoadingDialog(int step) {
        runOnUiThread(() -> {
            if (loadingDialog == null || !loadingDialog.isShowing()) {
                View view = LayoutInflater.from(this)
                        .inflate(R.layout.dialog_loading_subscription, null);
                loadingDialog = new Dialog(this);
                loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                loadingDialog.setContentView(view);
                loadingDialog.getWindow().setBackgroundDrawable(
                        new ColorDrawable(Color.TRANSPARENT));
                loadingDialog.getWindow().setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
                loadingDialog.getWindow().setGravity(Gravity.CENTER);
                loadingDialog.setCancelable(false);

                tvLoadingTitle    = view.findViewById(R.id.tvLoadingTitle);
                tvLoadingSubtitle = view.findViewById(R.id.tvLoadingSubtitle);
                tvStep2Num        = view.findViewById(R.id.tvStep2Num);
                tvStep3Num        = view.findViewById(R.id.tvStep3Num);
                connector1        = view.findViewById(R.id.connector1);
                connector2        = view.findViewById(R.id.connector2);

                loadingDialog.show();
            }
            advanceLoadingStep(step, null, null);
        });
    }

    private void advanceLoadingStep(int step, String title, String subtitle) {
        runOnUiThread(() -> {
            if (tvLoadingTitle == null) return;
            if (step == 1) {
                tvLoadingTitle.setText("Creating Order");
                tvLoadingSubtitle.setText("Please wait, do not press back…");
            } else if (step == 2) {
                tvLoadingTitle.setText(title != null ? title : "Opening Payment");
                tvLoadingSubtitle.setText(subtitle != null ? subtitle : "Launching Razorpay…");
                activateStep(tvStep2Num, connector1);
            } else if (step == 3) {
                tvLoadingTitle.setText("Activating Subscription");
                tvLoadingSubtitle.setText("Verifying payment & updating your roles…");
                activateStep(tvStep2Num, connector1);
                activateStep(tvStep3Num, connector2);
            }
        });
    }

    private void activateStep(View stepView, View connector) {
        if (stepView != null) {
            stepView.animate().alpha(1f).setDuration(300).start();
        }
        if (connector != null) {
            connector.setBackgroundColor(Color.parseColor("#4CAF50"));
        }
    }

    private void hideLoadingDialog() {
        runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
                loadingDialog    = null;
                tvLoadingTitle   = null;
                tvLoadingSubtitle = null;
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private void setText(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "—";
        try {
            String d = iso.contains("T") ? iso.split("T")[0] : iso;
            String[] p  = d.split("-");
            String[] mo = {"","Jan","Feb","Mar","Apr","May","Jun",
                              "Jul","Aug","Sep","Oct","Nov","Dec"};
            return p[2] + " " + mo[Integer.parseInt(p[1])] + " " + p[0];
        } catch (Exception e) { return iso; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoadingDialog();
    }
}
