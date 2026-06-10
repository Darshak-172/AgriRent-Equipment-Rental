package com.example.agrirent.activities;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.os.Handler;
import android.os.Looper;

import com.example.agrirent.R;
import com.example.agrirent.models.PaymentItem;
import com.example.agrirent.models.PaymentHistoryResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PaymentHistoryActivity
 *
 * Loads the user's payment history from the dedicated payment tracking API.
 * Displays all entries with real amounts, order IDs, and payment IDs.
 * Generates a printable PDF receipt with all payment details on device 
 * and shares it via the Android share sheet.
 */
public class PaymentHistoryActivity extends BaseActivity {

    private LinearLayout     llLoading, llEmpty, llPayments;
    private NestedScrollView scrollView;
    private TextView         tvTotalPaid;
    private LinearLayout     llTotalBadge;
    private SwipeRefreshLayout swipeRefreshLayout;

    private SessionManager session;
    private ApiService     api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        session = new SessionManager(this);
        api     = ApiClient.getClient(this).create(ApiService.class);

        bindViews();
        animateIn();
        loadPayments();
    }

    private void bindViews() {
        llLoading    = findViewById(R.id.llLoading);
        llEmpty      = findViewById(R.id.llEmpty);
        llPayments   = findViewById(R.id.llPayments);
        scrollView   = findViewById(R.id.scrollView);
        tvTotalPaid  = findViewById(R.id.tvTotalPaid);
        llTotalBadge = findViewById(R.id.llTotalBadge);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadPayments();
                new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
            });
        }

        View ivBack = findViewById(R.id.ivBack);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());
    }

    private void animateIn() {
        // Find the white sheet (second child) and slide it up
        LinearLayout root = (LinearLayout) ((android.view.ViewGroup) getWindow().getDecorView()
                .findViewById(android.R.id.content)).getChildAt(0);
        if (root != null && root.getChildCount() > 1) {
            View sheet = root.getChildAt(1);
            sheet.setTranslationY(400f);
            sheet.setAlpha(0f);
            sheet.animate().translationY(0f).alpha(1f)
                    .setDuration(450)
                    .setInterpolator(new OvershootInterpolator(0.8f))
                    .start();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // LOAD DATA — NEW API Endpoint (Payment Tracking System)
    // ──────────────────────────────────────────────────────────────────────

    private void loadPayments() {
        setState("loading");

        // Requesting up to 100 recent payments for history
        api.getPaymentHistory(100, 1).enqueue(new Callback<PaymentHistoryResponse>() {
            @Override
            public void onResponse(Call<PaymentHistoryResponse> call,
                                   Response<PaymentHistoryResponse> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess()) {
                    List<PaymentItem> list = res.body().getData();
                    if (list != null && !list.isEmpty()) {
                        buildCards(list);
                        setState("list");
                    } else {
                        setState("empty");
                    }
                } else if (res.code() == 401) {
                    Toast.makeText(PaymentHistoryActivity.this,
                            "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
                    setState("empty");
                } else {
                    Toast.makeText(PaymentHistoryActivity.this,
                            "Could not load payment history.", Toast.LENGTH_SHORT).show();
                    setState("empty");
                }
            }

            @Override
            public void onFailure(Call<PaymentHistoryResponse> call, Throwable t) {
                Toast.makeText(PaymentHistoryActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                setState("empty");
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────
    // BUILD CARDS
    // ──────────────────────────────────────────────────────────────────────

    private void buildCards(List<PaymentItem> list) {
        llPayments.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        
        double totalAmountPaid = 0;

        for (int i = 0; i < list.size(); i++) {
            PaymentItem sub = list.get(i);
            View card = inf.inflate(R.layout.item_payment_history, llPayments, false);

            // Plan name
            setText(card, R.id.tvPayPlanName,
                    sub.getPlanName() != null ? sub.getPlanName() : "Plan");

            // Payment date
            setText(card, R.id.tvPayDate, formatDate(sub.getPaymentDate()));

            // Check if payment was successful to accumulate total
            boolean paid = !"Failed".equalsIgnoreCase(sub.getStatus())
                         && !"Pending".equalsIgnoreCase(sub.getStatus());
            if (paid) {
                totalAmountPaid += sub.getAmount();
            }

            // Real accurate Amount from new API!
            DecimalFormat df = new DecimalFormat("#,##0.00");
            setText(card, R.id.tvPayAmount, "₹" + df.format(sub.getAmount()));

            // Payment ID
            String pid = sub.getRazorpayPaymentId();
            setText(card, R.id.tvPayId,
                    (pid != null && !pid.isEmpty()) ? pid : "—");

            // View button
            MaterialButton btnView = card.findViewById(R.id.btnViewReceipt);
            if (btnView != null) {
                final PaymentItem finalSub = sub;
                btnView.setOnClickListener(v -> showReceiptDialog(finalSub));
            }

            // Download (Direct) button
            MaterialButton btnDownload = card.findViewById(R.id.btnDownloadReceipt);
            if (btnDownload != null) {
                final PaymentItem finalSub = sub;
                btnDownload.setOnClickListener(v -> downloadReceiptDirect(finalSub));
            }

            // Stagger animation
            final int delay = i * 80;
            card.setAlpha(0f);
            card.setTranslationY(30f);
            card.animate().alpha(1f).translationY(0f)
                    .setStartDelay(delay).setDuration(350)
                    .setInterpolator(new OvershootInterpolator(0.8f))
                    .start();

            llPayments.addView(card);
        }

        // Show total count badge + total sum!
        final double finalTotalAmount = totalAmountPaid;
        runOnUiThread(() -> {
            if (llTotalBadge != null) {
                llTotalBadge.setVisibility(View.VISIBLE);
                if (tvTotalPaid != null) {
                    DecimalFormat df = new DecimalFormat("#,##0");
                    tvTotalPaid.setText("₹" + df.format(finalTotalAmount));
                }
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────
    // RECEIPT DIALOG & DIRECT DOWNLOAD
    // ──────────────────────────────────────────────────────────────────────

    private void showReceiptDialog(PaymentItem sub) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_receipt);

        Window objWindow = dialog.getWindow();
        if (objWindow != null) {
            objWindow.setBackgroundDrawableResource(android.R.color.transparent);
            objWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            objWindow.setGravity(Gravity.CENTER);
        }

        // Bind data
        String receiptNumber = sub.getReceiptNumber() != null ? sub.getReceiptNumber() : String.valueOf(sub.getPaymentId());
        setText(dialog.getWindow().getDecorView(), R.id.tvRcptNo, receiptNumber);
        setText(dialog.getWindow().getDecorView(), R.id.tvRcptUserName, session.getUserName());
        setText(dialog.getWindow().getDecorView(), R.id.tvRcptUserPhone, session.getUserMobile());
        setText(dialog.getWindow().getDecorView(), R.id.tvRcptPlan, sub.getPlanName() != null ? sub.getPlanName() : "—");
        setText(dialog.getWindow().getDecorView(), R.id.tvRcptDate, formatDate(sub.getPaymentDate()));
        setText(dialog.getWindow().getDecorView(), R.id.tvRcptMethod, sub.getPaymentMethod() != null ? sub.getPaymentMethod() : "Online");
        
        TextView tvStatus = dialog.findViewById(R.id.tvRcptStatus);
        if (tvStatus != null) {
            tvStatus.setText(sub.getStatus() != null ? sub.getStatus() : "—");
            if ("Failed".equalsIgnoreCase(sub.getStatus())) {
                tvStatus.setTextColor(Color.parseColor("#D32F2F"));
            } else {
                tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            }
        }

        if ("Failed".equalsIgnoreCase(sub.getStatus()) && sub.getErrorMessage() != null) {
            TextView errorText = dialog.findViewById(R.id.tvRcptError);
            if (errorText != null) {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Error: " + sub.getErrorMessage());
            }
        }

        setText(dialog.getWindow().getDecorView(), R.id.tvRcptOrderId, sub.getRazorpayOrderId() != null ? sub.getRazorpayOrderId() : "—");
        setText(dialog.getWindow().getDecorView(), R.id.tvRcptPaymentId, sub.getRazorpayPaymentId() != null ? sub.getRazorpayPaymentId() : "—");
        
        DecimalFormat df = new DecimalFormat("#,##0.00");
        String formattedAmount = (sub.getCurrency() != null ? sub.getCurrency() + " " : "₹") + df.format(sub.getAmount());
        setText(dialog.getWindow().getDecorView(), R.id.tvRcptAmount, formattedAmount);

        // Close
        View btnClose = dialog.findViewById(R.id.ivCloseReceipt);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        // Download from Dialog
        MaterialButton btnDownload = dialog.findViewById(R.id.btnRcptDownload);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                downloadReceiptDirect(sub);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void downloadReceiptDirect(PaymentItem sub) {
        Toast.makeText(this, "Downloading PDF Receipt…", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                Uri pdfUri = createPdf(sub);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Receipt saved to Downloads folder!", Toast.LENGTH_LONG).show();
                    // Optional: intent to view the PDF right away
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setDataAndType(pdfUri, "application/pdf");
                    viewIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(Intent.createChooser(viewIntent, "Open Receipt with..."));
                    } catch (Exception ignored) { }
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Could not generate receipt: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private Uri createPdf(PaymentItem sub) throws IOException {
        // A4 points: 595 x 842
        int pageWidth  = 595;
        int pageHeight = 842;

        PdfDocument pdf  = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdf.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // ── Background ────────────────────────────────────
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, pageWidth, pageHeight, bgPaint);

        // ── Header bar ────────────────────────────────────
        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#1B5E20"));
        canvas.drawRect(0, 0, pageWidth, 110, headerPaint);

        // App name
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(28f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setAntiAlias(true);
        canvas.drawText("AgriRent", 40, 55, titlePaint);

        // Receipt label
        Paint subtitlePaint = new Paint();
        subtitlePaint.setColor(Color.parseColor("#A5D6A7"));
        subtitlePaint.setTextSize(13f);
        subtitlePaint.setAntiAlias(true);
        canvas.drawText("Payment Receipt", 40, 80, subtitlePaint);

        // Receipt # on right
        Paint rightPaint = new Paint();
        rightPaint.setColor(Color.WHITE);
        rightPaint.setTextSize(12f);
        rightPaint.setTextAlign(Paint.Align.RIGHT);
        rightPaint.setAntiAlias(true);
        String receiptNumber = sub.getReceiptNumber() != null ? sub.getReceiptNumber() : String.valueOf(sub.getPaymentId());
        canvas.drawText("Receipt #" + receiptNumber, pageWidth - 40, 55, rightPaint);
        canvas.drawText(formatDate(sub.getPaymentDate()), pageWidth - 40, 75, rightPaint);

        // ── Green accent bar ──────────────────────────────
        Paint accentPaint = new Paint();
        accentPaint.setColor(Color.parseColor("#43A047"));
        canvas.drawRect(0, 110, pageWidth, 116, accentPaint);

        // ── Section: Payment Summary ──────────────────────
        float y = 150f;
        Paint sectionTitle = new Paint();
        sectionTitle.setColor(Color.parseColor("#1B5E20"));
        sectionTitle.setTextSize(14f);
        sectionTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sectionTitle.setAntiAlias(true);
        canvas.drawText("PAYMENT SUMMARY", 40, y, sectionTitle);

        y += 10;
        Paint dividerPaint = new Paint();
        dividerPaint.setColor(Color.parseColor("#E8F5E9"));
        canvas.drawRect(40, y, pageWidth - 40, y + 1.5f, dividerPaint);
        y += 25;

        // Row: Billed To
        y = drawRow(canvas, y, "Billed To", session.getUserName());
        // Row: Contact
        y = drawRow(canvas, y, "Contact", session.getUserMobile());
        // Row: Plan
        y = drawRow(canvas, y, "Plan Name", sub.getPlanName() != null ? sub.getPlanName() : "—");
        // Row: Status
        y = drawRow(canvas, y, "Transaction Status", sub.getStatus() != null ? sub.getStatus() : "—");
        // Row: Order ID
        String orderId = sub.getRazorpayOrderId();
        y = drawRow(canvas, y, "Order ID", (orderId != null && !orderId.isEmpty()) ? orderId : "—");
        // Row: Payment ID
        String payId = sub.getRazorpayPaymentId();
        y = drawRow(canvas, y, "Payment ID", (payId != null && !payId.isEmpty()) ? payId : "—");
        // Row: Payment Date
        y = drawRow(canvas, y, "Payment Date", formatDate(sub.getPaymentDate()));

        // ── Amount box ───────────────────────────────────
        y += 20;
        Paint amtBoxPaint = new Paint();
        amtBoxPaint.setColor(Color.parseColor("#E8F5E9"));
        canvas.drawRoundRect(40, y, pageWidth - 40, y + 70, 12, 12, amtBoxPaint);

        Paint amtLabelPaint = new Paint();
        amtLabelPaint.setColor(Color.parseColor("#555555"));
        amtLabelPaint.setTextSize(12f);
        amtLabelPaint.setAntiAlias(true);
        canvas.drawText("Total Amount", 60, y + 25, amtLabelPaint);

        Paint amtValuePaint = new Paint();
        amtValuePaint.setColor(Color.parseColor("#1B5E20"));
        amtValuePaint.setTextSize(26f);
        amtValuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        amtValuePaint.setAntiAlias(true);
        DecimalFormat df = new DecimalFormat("#,##0.00");
        
        String formattedAmount = (sub.getCurrency() != null ? sub.getCurrency() + " " : "₹") + df.format(sub.getAmount());
        canvas.drawText(formattedAmount, 60, y + 54, amtValuePaint);

        y += 90;

        // ── Note ─────────────────────────────────────────
        Paint notePaint = new Paint();
        notePaint.setColor(Color.parseColor("#AAAAAA"));
        notePaint.setTextSize(10f);
        notePaint.setAntiAlias(true);
        canvas.drawText("* Payment verified via Razorpay HMAC-SHA256 signature.", 40, y, notePaint);
        y += 16;
        canvas.drawText("* This is a system-generated receipt. No signature required.", 40, y, notePaint);

        // ── Footer ───────────────────────────────────────
        float footerY = pageHeight - 60;
        Paint footerLinePaint = new Paint();
        footerLinePaint.setColor(Color.parseColor("#E8F5E9"));
        canvas.drawRect(0, footerY, pageWidth, footerY + 1.5f, footerLinePaint);

        Paint footerPaint = new Paint();
        footerPaint.setColor(Color.parseColor("#AAAAAA"));
        footerPaint.setTextSize(10f);
        footerPaint.setTextAlign(Paint.Align.CENTER);
        footerPaint.setAntiAlias(true);
        canvas.drawText("AgriRent — Rent Equipment • Buy Farm Supplies", pageWidth / 2f, pageHeight - 35, footerPaint);
        canvas.drawText("For queries, contact us through the AgriRent app.", pageWidth / 2f, pageHeight - 20, footerPaint);

        pdf.finishPage(page);

        // ── Save PDF ──────────────────────────────────────
        String fileName = "AgriRent_Receipt_" + receiptNumber + ".pdf";
        Uri uri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ — use MediaStore
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            cv.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) throw new IOException("Failed to create MediaStore entry.");
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                pdf.writeTo(os);
            }
        } else {
            // Pre-Q
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                pdf.writeTo(fos);
            }
            uri = Uri.fromFile(file);
        }

        pdf.close();
        return uri;
    }

    /** Draws a label-value row. Returns updated Y. */
    private float drawRow(Canvas canvas, float y, String label, String value) {
        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#888888"));
        labelPaint.setTextSize(12f);
        labelPaint.setAntiAlias(true);

        Paint valuePaint = new Paint();
        valuePaint.setColor(Color.parseColor("#1A2E1A"));
        valuePaint.setTextSize(13f);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        valuePaint.setAntiAlias(true);
        valuePaint.setTextAlign(Paint.Align.RIGHT);

        canvas.drawText(label, 40, y, labelPaint);
        canvas.drawText(value, 555, y, valuePaint);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#F5F5F5"));
        canvas.drawRect(40, y + 5, 555, y + 6, linePaint);

        return y + 32;
    }



    // ──────────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────────

    private void setState(String s) {
        runOnUiThread(() -> {
            if ("loading".equals(s) && (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing())) {
                if (llLoading  != null) llLoading.setVisibility(View.VISIBLE);
            } else if (!"loading".equals(s)) {
                if (llLoading  != null) llLoading.setVisibility(View.GONE);
            }
            if (llEmpty    != null) llEmpty.setVisibility("empty".equals(s)     ? View.VISIBLE : View.GONE);
            if (scrollView != null) scrollView.setVisibility("list".equals(s)   ? View.VISIBLE : View.GONE);
        });
    }

    private void setText(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "—";
        try {
            String d  = iso.contains("T") ? iso.split("T")[0] : iso;
            String[] p  = d.split("-");
            String[] mo = {"","Jan","Feb","Mar","Apr","May","Jun",
                              "Jul","Aug","Sep","Oct","Nov","Dec"};
            return p[2] + " " + mo[Integer.parseInt(p[1])] + " " + p[0];
        } catch (Exception e) { return iso; }
    }
}
