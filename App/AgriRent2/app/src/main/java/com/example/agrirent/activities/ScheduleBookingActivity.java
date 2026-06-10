package com.example.agrirent.activities;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.agrirent.R;
import com.example.agrirent.models.BlockedDateDto;
import com.example.agrirent.models.CreateBookingRequest;
import com.example.agrirent.models.CreateBookingResponse;
import com.example.agrirent.models.Equipment;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleBookingActivity extends BaseActivity {

    private Equipment equipment;
    private ApiService apiService;
    private boolean isHourlyMode = true; // The current UI mode user selected

    // UI
    private TextView tvEquipmentName, tvEquipmentType, tvPriceTag, tvPriceUnit;
    private ShapeableImageView ivEquipmentImage;
    private TextView tvMonthYear, tvDateHint;
    private RecyclerView rvCalendarGrid;
    private TextView tvTotalLabel, tvTotalPrice, tvBookingSummary;
    private MaterialButton btnConfirmBooking;

    // Hourly-only UI
    private View layoutTimeSection;
    private RadioGroup rgStartTimes;
    private MaterialButton btnDecreaseHours, btnIncreaseHours;
    private TextView tvHoursCount;

    // Daily-only UI
    private View layoutDateRange;
    private LinearLayout llStartDate, llEndDate;
    private TextView tvStartDate, tvEndDate, tvStartTime, tvEndTime;

    // Loading
    private AlertDialog loadingDialog;
    private View layoutLoadingOverlay; // full-screen overlay shown while fetching blocked dates

    // Calendar state
    private Calendar currentMonthCalendar;
    private Calendar startDateCalendar;  // selected start date (or single date for hourly)
    private Calendar endDateCalendar;    // selected end date (daily only)
    private boolean selectingStart = true; // for daily: true = selecting start, false = end

    // Hourly state
    private int selectedTimeHour = 8;
    private int selectedTimeMinute = 0;
    private int selectedHours = 2; // minimum 2

    // Daily Time State
    private int selectedStartHour = 10;
    private int selectedStartMin = 0;
    private int selectedEndHour = 10;
    private int selectedEndMin = 0;

    // Blocked dates from API
    private final List<BlockedDateDto> blockedDates = new ArrayList<>();

    private static final SimpleDateFormat DISPLAY_FMT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat ISO_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_schedule_booking);
            com.example.agrirent.network.LoadingInterceptor.setCurrentActivity(this);
            apiService = ApiClient.getClient(this).create(ApiService.class);

            if (getIntent().hasExtra("equipment")) {
                equipment = (Equipment) getIntent().getSerializableExtra("equipment");
            }
            if (equipment == null) {
                Toast.makeText(this, "Equipment not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            isHourlyMode = true;

            initViews();
            initViews();
            populateEquipmentData();
            setupCalendar();
            setupModeToggle();
            
            if (isHourlyMode) setupTimeSlots();
            
            fetchBlockedDates();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // -------------------------------------------------------------------------
    // INIT
    // -------------------------------------------------------------------------

    private void initViews() {
        tvEquipmentName = findViewById(R.id.tvEquipmentName);
        tvEquipmentType = findViewById(R.id.tvEquipmentType);
        tvPriceTag      = findViewById(R.id.tvPriceTag);
        tvPriceUnit     = findViewById(R.id.tvPriceUnit);
        ivEquipmentImage= findViewById(R.id.ivEquipmentImage);

        tvMonthYear     = findViewById(R.id.tvMonthYear);
        tvDateHint      = findViewById(R.id.tvDateHint);
        rvCalendarGrid  = findViewById(R.id.rvCalendarGrid);

        tvTotalLabel    = findViewById(R.id.tvTotalLabel);
        tvTotalPrice    = findViewById(R.id.tvTotalPrice);
        tvBookingSummary= findViewById(R.id.tvBookingSummary);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        // Loading overlay — NO LONGER shown (fetch in background)
        layoutLoadingOverlay = findViewById(R.id.layoutLoadingOverlay);
        layoutLoadingOverlay.setVisibility(View.GONE);

        layoutTimeSection = findViewById(R.id.layoutTimeSection);
        rgStartTimes      = findViewById(R.id.rgStartTimes);
        btnDecreaseHours  = findViewById(R.id.btnDecreaseHours);
        btnIncreaseHours  = findViewById(R.id.btnIncreaseHours);
        tvHoursCount      = findViewById(R.id.tvHoursCount);

        layoutDateRange = findViewById(R.id.layoutDateRange);
        llStartDate     = findViewById(R.id.llStartDate);
        llEndDate       = findViewById(R.id.llEndDate);
        tvStartDate     = findViewById(R.id.tvStartDate);
        tvEndDate       = findViewById(R.id.tvEndDate);
        tvStartTime     = findViewById(R.id.tvStartTime);
        tvEndTime       = findViewById(R.id.tvEndTime);

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Month navigation
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            updateCalendarUI();
        });
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            updateCalendarUI();
        });

        // Hours Picker (hourly only)
        btnDecreaseHours.setOnClickListener(v -> {
            if (selectedHours > 2) {
                selectedHours--;
                updateHoursDisplay();
                updatePriceDisplay();
            }
        });
        btnIncreaseHours.setOnClickListener(v -> {
            if (selectedHours < 24) {
                selectedHours++;
                updateHoursDisplay();
                updatePriceDisplay();
            }
        });

        // Book
        btnConfirmBooking.setOnClickListener(v -> submitBooking());

        // Only show date range click listeners if they haven't been set yet
        llStartDate.setOnClickListener(v -> {
            if (startDateCalendar != null) {
                showTimePicker(true);
            } else {
                Toast.makeText(this, "Please select the start date first", Toast.LENGTH_SHORT).show();
            }
        });

        llEndDate.setOnClickListener(v -> {
            if (endDateCalendar != null) {
                showTimePicker(false);
            } else {
                Toast.makeText(this, "Please select the end date first", Toast.LENGTH_SHORT).show();
            }
        });

        updateUIMode();
    }

    private void setupModeToggle() {
        com.google.android.material.button.MaterialButtonToggleGroup toggle = findViewById(R.id.toggleBookingMode);
        toggle.check(isHourlyMode ? R.id.btnModeHourly : R.id.btnModeDaily);
        
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean newMode = (checkedId == R.id.btnModeHourly);
                if (newMode != isHourlyMode) {
                    isHourlyMode = newMode;
                    // Reset selections when switching mode
                    startDateCalendar = null;
                    endDateCalendar = null;
                    selectingStart = true;
                    
                    if (isHourlyMode && rgStartTimes != null && rgStartTimes.getChildCount() == 0) {
                        setupTimeSlots();
                    }
                    
                    updateUIMode();
                    updateCalendarUI(); // re-render dates
                    updatePriceDisplay();
                }
            }
        });
    }

    private void updateUIMode() {
        if (!isHourlyMode) {
            layoutDateRange.setVisibility(View.VISIBLE);
            layoutTimeSection.setVisibility(View.GONE);
            tvDateHint.setText("Tap to select START date, then END date");
            // Restore base price in header
            tvPriceTag.setText("₹" + String.format(Locale.getDefault(), "%.0f", equipment.getDailyPrice()));
            tvPriceUnit.setText("/day");
        } else {
            layoutDateRange.setVisibility(View.GONE);
            layoutTimeSection.setVisibility(View.VISIBLE);
            tvDateHint.setText("Tap a date to book");
            // Show derived hourly rate in header if base is daily
            tvPriceTag.setText("₹" + String.format(Locale.getDefault(), "%.0f", equipment.getHourlyPrice()));
            tvPriceUnit.setText("/hour");
        }
        updateSmartBookingHints();
    }

    private void populateEquipmentData() {
        tvEquipmentName.setText(equipment.getEquipmentName());
        tvEquipmentType.setText(equipment.getCategoryName() != null ? equipment.getCategoryName() : "Agricultural Equipment");

        tvPriceTag.setText("₹" + String.format(Locale.getDefault(), "%.0f", equipment.getHourlyPrice()));
        tvPriceUnit.setText("/hour");

        String imageUrl = null;
        if (equipment.getImages() != null && !equipment.getImages().isEmpty()) {
            imageUrl = equipment.getImages().get(0).getImageUrl();
        } else if (equipment.getThumbnailUrl() != null) {
            imageUrl = equipment.getThumbnailUrl();
        }
        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(ivEquipmentImage);
        }
    }

    // -------------------------------------------------------------------------
    // CALENDAR
    // -------------------------------------------------------------------------

    private void setupCalendar() {
        currentMonthCalendar = Calendar.getInstance();
        currentMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
        updateCalendarUI();
    }

    private void updateCalendarUI() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonthCalendar.getTime()));

        List<Date> cells = new ArrayList<>();
        Calendar cal = (Calendar) currentMonthCalendar.clone();
        int start = cal.get(Calendar.DAY_OF_WEEK) - 1;
        cal.add(Calendar.DAY_OF_MONTH, -start);
        while (cells.size() < 42) {
            cells.add(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        rvCalendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
        rvCalendarGrid.setAdapter(new CalendarAdapter(cells));
    }

    private void onDateTapped(Calendar tapped) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);

        if (tapped.getTimeInMillis() < today.getTimeInMillis()) {
            Toast.makeText(this, "Cannot select past dates", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isDateBooked(tapped) && !isHourlyMode) {
            Toast.makeText(this, "This date is already booked", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isDateBlocked(tapped)) {
            Toast.makeText(this, "This date is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isHourlyMode) {
            startDateCalendar = (Calendar) tapped.clone();
            endDateCalendar = null;
            updateCalendarUI();
            refreshTimeSlotsForDate(startDateCalendar); // disable booked time slots
            updatePriceDisplay();
            updateSmartBookingHints();
        } else {
            // Daily: two-tap date range selection
            if (selectingStart || endDateCalendar != null) {
                startDateCalendar = (Calendar) tapped.clone();
                endDateCalendar = null;
                selectingStart = false;
                tvDateHint.setText("Now tap END date");
                tvStartDate.setText(DISPLAY_FMT.format(tapped.getTime()));
                tvEndDate.setText("Not selected");
            } else {
                if (tapped.getTimeInMillis() <= startDateCalendar.getTimeInMillis()) {
                    Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                    return;
                }
                endDateCalendar = (Calendar) tapped.clone();
                selectingStart = true;
                tvDateHint.setText("Tap START date to change selection");
                tvEndDate.setText(DISPLAY_FMT.format(tapped.getTime()));
            }
            updateCalendarUI();
            updatePriceDisplay();
            updateSmartBookingHints();
        }
    }

// -------------------------------------------------------------------------
    // TIME SLOTS (HOURLY)
    // -------------------------------------------------------------------------

    private static final String[] TIME_LABELS = {"6 AM","7 AM","8 AM","9 AM","10 AM","11 AM","12 PM","1 PM","2 PM","3 PM","4 PM","5 PM"};
    private static final int[]    TIME_HOURS  = {  6,     7,     8,     9,     10,     11,     12,     13,    14,    15,    16,    17};

    private void setupTimeSlots() {
        rgStartTimes.removeAllViews();

        for (int i = 0; i < TIME_HOURS.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(TIME_LABELS[i]);
            rb.setTag(TIME_HOURS[i]);
            rb.setId(View.generateViewId());
            rb.setButtonDrawable(null);
            rb.setBackground(getResources().getDrawable(R.drawable.selector_time_slot, null));
            rb.setTextColor(getResources().getColorStateList(R.color.selector_time_text, null));
            rb.setTextSize(13f);
            rb.setPadding(32, 16, 32, 16);

            RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.WRAP_CONTENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            rb.setLayoutParams(lp);

            rgStartTimes.addView(rb);

            // Select 8 AM by default
            if (TIME_HOURS[i] == 8) {
                rb.setChecked(true);
                selectedTimeHour = 8;
            }
        }

        rgStartTimes.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb2 = group.findViewById(checkedId);
            if (rb2 != null && rb2.getTag() != null && rb2.isEnabled()) {
                selectedTimeHour = (int) rb2.getTag();
                updatePriceDisplay();
            }
        });

        updateHoursDisplay();
    }

    /**
     * Called after a date is selected in hourly mode.
     * Disables radio buttons whose hour overlaps an existing booking on that date.
     */
    private void refreshTimeSlotsForDate(Calendar selectedDate) {
        if (rgStartTimes == null || selectedDate == null) return;

        // Check if the selected date is today
        Calendar now = Calendar.getInstance();
        boolean isToday = selectedDate.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && selectedDate.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                && selectedDate.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH);
        int currentHour = now.get(Calendar.HOUR_OF_DAY);

        int firstAvailableHour = -1;

        for (int i = 0; i < rgStartTimes.getChildCount(); i++) {
            View child = rgStartTimes.getChildAt(i);
            if (!(child instanceof RadioButton)) continue;
            RadioButton rb = (RadioButton) child;
            int slotHour = (int) rb.getTag();

            boolean slotBooked = isHourBooked(selectedDate, slotHour, selectedHours);
            // Disable past hours when today is selected
            boolean slotPast   = isToday && slotHour <= currentHour;

            if (slotBooked) {
                rb.setEnabled(false);
                rb.setChecked(false);
                rb.setAlpha(0.35f);
                // Solid red tint to show it's booked
                rb.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                rb.setTextColor(getResources().getColor(android.R.color.white));
            } else if (slotPast) {
                // Grey out past slots — user cannot select them
                rb.setEnabled(false);
                rb.setChecked(false);
                rb.setAlpha(0.35f);
                rb.setBackground(getResources().getDrawable(R.drawable.selector_time_slot, null));
                rb.setTextColor(getResources().getColorStateList(R.color.selector_time_text, null));
            } else {
                rb.setEnabled(true);
                rb.setAlpha(1f);
                rb.setBackground(getResources().getDrawable(R.drawable.selector_time_slot, null));
                rb.setTextColor(getResources().getColorStateList(R.color.selector_time_text, null));
                if (firstAvailableHour == -1) firstAvailableHour = slotHour;
            }
        }

        // Auto-select the first available time if the current selection is booked or past
        boolean currentInvalid = isHourBooked(selectedDate, selectedTimeHour, selectedHours)
                || (isToday && selectedTimeHour <= currentHour);
        if (currentInvalid && firstAvailableHour != -1) {
            for (int i = 0; i < rgStartTimes.getChildCount(); i++) {
                if (rgStartTimes.getChildAt(i) instanceof RadioButton) {
                    RadioButton rb = (RadioButton) rgStartTimes.getChildAt(i);
                    if ((int) rb.getTag() == firstAvailableHour) {
                        rb.setChecked(true);
                        selectedTimeHour = firstAvailableHour;
                        break;
                    }
                }
            }
            tvDateHint.setText("Smart pick: " + formatHour(firstAvailableHour) + " looks open.");
        } else {
            updateSmartBookingHints();
        }
    }

    /**
     * Returns true if the time slot starting at {@code startHour} for {@code durationHours}
     * overlaps with any booking on the given date.
     */
    private boolean isHourBooked(Calendar date, int startHour, int durationHours) {
        if (blockedDates.isEmpty()) return false;
        String slotStart = toDateTimeStr(date, startHour);
        String slotEnd   = toDateTimeStr(date, startHour + durationHours);

        for (BlockedDateDto b : blockedDates) {
            String bStart = b.getStartDate();
            String bEnd   = b.getEndDate();
            if (bStart == null || bEnd == null) continue;

            // Use lexicographical comparison for ISO-8601 strings
            // A slot overlaps with a booking if (slotStart < bEnd) AND (slotEnd > bStart)
            if (slotStart.compareTo(bEnd) < 0 && slotEnd.compareTo(bStart) > 0) {
                return true;
            }
        }
        return false;
    }

    private void updateHoursDisplay() {
        tvHoursCount.setText(selectedHours + (selectedHours == 1 ? " hour" : " hours"));
    }

    // -------------------------------------------------------------------------
    // PRICE DISPLAY
    // -------------------------------------------------------------------------

    private void updatePriceDisplay() {
        if (isHourlyMode) {
            if (startDateCalendar == null) {
                tvTotalLabel.setText("Select a date to calculate price");
                tvTotalPrice.setText("₹0");
                tvBookingSummary.setText("");
                updateSmartBookingHints();
                return;
            }
            
            double total = equipment.getHourlyPrice() * selectedHours;
            tvTotalLabel.setText("₹" + String.format(Locale.getDefault(), "%.0f", equipment.getHourlyPrice()) + "/hr × " + selectedHours + " hours");
            
            tvTotalPrice.setText("₹" + String.format(Locale.getDefault(), "%.0f", total));
            tvBookingSummary.setText(DISPLAY_FMT.format(startDateCalendar.getTime()) + "  " + formatHour(selectedTimeHour) + " → " + formatHour(selectedTimeHour + selectedHours));
        } else {
            if (startDateCalendar == null) {
                tvTotalLabel.setText("Select dates to calculate price");
                tvTotalPrice.setText("₹0");
                tvBookingSummary.setText("");
                updateSmartBookingHints();
                return;
            }
            if (endDateCalendar == null) {
                tvTotalLabel.setText("Select end date to see total");
                tvTotalPrice.setText("₹0");
                tvBookingSummary.setText(DISPLAY_FMT.format(startDateCalendar.getTime()));
                updateSmartBookingHints();
                return;
            }

            Calendar sCal = (Calendar) startDateCalendar.clone();
            sCal.set(Calendar.HOUR_OF_DAY, selectedStartHour);
            sCal.set(Calendar.MINUTE, selectedStartMin);
            sCal.set(Calendar.SECOND, 0);

            Calendar eCal = (Calendar) endDateCalendar.clone();
            eCal.set(Calendar.HOUR_OF_DAY, selectedEndHour);
            eCal.set(Calendar.MINUTE, selectedEndMin);
            eCal.set(Calendar.SECOND, 0);

            long diffMs = eCal.getTimeInMillis() - sCal.getTimeInMillis();
            double totalHours = (double) diffMs / (1000 * 60 * 60);
            int days = (int) Math.ceil(totalHours / 24.0);
            if (days < 1) days = 1;
            double total = equipment.getDailyPrice() * days;
            tvTotalLabel.setText("₹" + String.format(Locale.getDefault(), "%.0f", equipment.getDailyPrice()) + " × " + days + (days == 1 ? " day" : " days"));

            tvTotalPrice.setText("₹" + String.format(Locale.getDefault(), "%.0f", total));
            
            int displayDays = (int) Math.ceil(totalHours / 24.0);
            if (displayDays < 1) displayDays = 1;
            tvBookingSummary.setText(displayDays + (displayDays == 1 ? " day" : " days"));
        }
        updateSmartBookingHints();
    }

    private String formatHour(int h) {
        if (h >= 24) h -= 24;
        if (h == 0) return "12 AM";
        if (h < 12) return h + " AM";
        if (h == 12) return "12 PM";
        return (h - 12) + " PM";
    }

    private void updateSmartBookingHints() {
        if (tvDateHint == null) return;

        String season = getSeasonLabel(currentMonthCalendar != null ? currentMonthCalendar.get(Calendar.MONTH) : Calendar.getInstance().get(Calendar.MONTH));
        String demand = isWeekendMode() ? "Weekend demand may run higher." : "Weekday rates are usually calmer.";

        if (isHourlyMode) {
            if (startDateCalendar == null) {
                tvDateHint.setText("Tap a date to book • " + season + " season.");
                return;
            }

            if (isHourBooked(startDateCalendar, selectedTimeHour, selectedHours)) {
                int nextOpen = findNextOpenHour(startDateCalendar, selectedHours);
                if (nextOpen != -1) {
                    tvDateHint.setText("Smart pick: " + formatHour(nextOpen) + " looks open • " + season + " season.");
                } else {
                    tvDateHint.setText("No open hour slot in the visible range • try another day.");
                }
            } else {
                tvDateHint.setText("Smart rate check • " + demand + " " + season + " season.");
            }
        } else {
            if (startDateCalendar == null) {
                tvDateHint.setText("Tap to select START date, then END date • " + season + " season.");
            } else if (endDateCalendar == null) {
                tvDateHint.setText("Now tap END date • " + demand);
            } else {
                tvDateHint.setText("Smart booking window ready • " + season + " season.");
            }
        }
    }

    private boolean isWeekendMode() {
        Calendar cal = startDateCalendar != null ? startDateCalendar : Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY;
    }

    private int findNextOpenHour(Calendar selectedDate, int durationHours) {
        for (int hour = 6; hour <= 20; hour++) {
            if (!isHourBooked(selectedDate, hour, durationHours)) {
                return hour;
            }
        }
        return -1;
    }

    private String getSeasonLabel(int monthIndex) {
        switch (monthIndex) {
            case Calendar.MARCH:
            case Calendar.APRIL:
            case Calendar.MAY:
                return "Summer";
            case Calendar.JUNE:
            case Calendar.JULY:
            case Calendar.AUGUST:
                return "Monsoon";
            case Calendar.SEPTEMBER:
            case Calendar.OCTOBER:
            case Calendar.NOVEMBER:
                return "Harvest";
            default:
                return "Winter";
        }
    }

    private void showTimePicker(boolean isStart) {
        int hour = isStart ? selectedStartHour : selectedEndHour;
        int min  = isStart ? selectedStartMin  : selectedEndMin;

        // Determine whether the date we're picking time for is today
        Calendar relevantDate = isStart ? startDateCalendar : endDateCalendar;
        Calendar now = Calendar.getInstance();
        boolean dateIsToday = relevantDate != null
                && relevantDate.get(Calendar.YEAR)  == now.get(Calendar.YEAR)
                && relevantDate.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                && relevantDate.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    // Reject if the chosen time is in the past (only when date is today)
                    if (dateIsToday) {
                        Calendar picked = Calendar.getInstance();
                        picked.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        picked.set(Calendar.MINUTE, minute);
                        picked.set(Calendar.SECOND, 0);
                        if (picked.getTimeInMillis() <= Calendar.getInstance().getTimeInMillis()) {
                            Toast.makeText(this, "Cannot select a past time. Please choose a future time.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    if (isStart) {
                        selectedStartHour = hourOfDay;
                        selectedStartMin  = minute;
                        tvStartTime.setText(formatTime(hourOfDay, minute));
                    } else {
                        selectedEndHour = hourOfDay;
                        selectedEndMin  = minute;
                        tvEndTime.setText(formatTime(hourOfDay, minute));
                    }
                    updatePriceDisplay();
                }, hour, min, false);
        timePickerDialog.show();
    }

    private String formatTime(int hourOfDay, int minute) {
        String amPm = hourOfDay >= 12 ? "PM" : "AM";
        int hour = hourOfDay % 12;
        if (hour == 0) hour = 12;
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour, minute, amPm);
    }

    // -------------------------------------------------------------------------
    // BLOCKED DATES
    // -------------------------------------------------------------------------

    private void fetchBlockedDates() {
        // Show overlay while loading
        // (Removed to prevent heavy loading screen feel)
        // if (layoutLoadingOverlay != null) layoutLoadingOverlay.setVisibility(View.VISIBLE);

        apiService.getBlockedDates(equipment.getEquipmentId()).enqueue(new Callback<List<BlockedDateDto>>() {
            @Override
            public void onResponse(Call<List<BlockedDateDto>> call, Response<List<BlockedDateDto>> response) {
                // Hide overlay — data is ready
                if (layoutLoadingOverlay != null) layoutLoadingOverlay.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    blockedDates.clear();
                    blockedDates.addAll(response.body());

                    // DEBUG: Shows how many restricted dates were loaded
                    android.util.Log.d("BlockedDates", "Loaded " + blockedDates.size() + " entries");
                    for (BlockedDateDto b : blockedDates) {
                        android.util.Log.d("BlockedDates", "  type=" + b.getType() + ", reason=" + b.getReason() + ", start=" + b.getStartDate() + ", end=" + b.getEndDate());
                    }

                    // Clear stale selections that fall inside a booked period
                    if (startDateCalendar != null && isDateBooked(startDateCalendar)) {
                        startDateCalendar = null;
                        endDateCalendar = null;
                        selectingStart = true;
                        if (!isHourlyMode) {
                            tvStartDate.setText("Not selected");
                            tvEndDate.setText("Not selected");
                            tvDateHint.setText("Tap to select START date, then END date");
                        }
                    } else if (endDateCalendar != null && isDateBooked(endDateCalendar)) {
                        endDateCalendar = null;
                        selectingStart = true;
                        if (!isHourlyMode) {
                            tvEndDate.setText("Not selected");
                            tvDateHint.setText("Now tap END date");
                        }
                    }

                    updateCalendarUI();
                    if (isHourlyMode && startDateCalendar != null) {
                        refreshTimeSlotsForDate(startDateCalendar);
                    } else {
                        updateSmartBookingHints();
                    }
                } else {
                    android.util.Log.w("BlockedDates", "HTTP " + response.code());
                    updateCalendarUI();
                    updateSmartBookingHints();
                }
            }
            @Override
            public void onFailure(Call<List<BlockedDateDto>> call, Throwable t) {
                // Hide overlay even on failure — let user book (server validates anyway)
                updateSmartBookingHints();
                if (layoutLoadingOverlay != null) layoutLoadingOverlay.setVisibility(View.GONE);
                android.util.Log.e("BlockedDates", "Network error: " + t.getMessage());
                updateCalendarUI(); // show calendar with no blocked info
            }
        });
    }

    // -----------------------------------------------------------------------
    // DATE AVAILABILITY HELPERS  (string-based, no timezone issues)
    // -----------------------------------------------------------------------

    /** yyyy-MM-dd string for the given Calendar (used for comparisons). */
    private String toDateStr(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    /** yyyy-MM-ddTHH:mm:ss string for comparison. */
    private String toDateTimeStr(Calendar c, int hour) {
        return String.format(Locale.US, "%04d-%02d-%02dT%02d:00:00",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH),
                hour);
    }

    /** First 10 chars of an ISO datetime string, e.g. "2026-03-07T00:00:00" -> "2026-03-07" */
    private String dateOnly(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return "";
        return isoDate.substring(0, 10);
    }

    /**
     * True if cal's calendar date falls inside ANY entry in blockedDates
     * where the entry is a CUSTOMER BOOKING (type="booked" OR reason="Booked").
     */
    private boolean isDateBooked(Calendar cal) {
        if (blockedDates.isEmpty()) return false;
        String today = toDateStr(cal);
        for (BlockedDateDto b : blockedDates) {
            if (!isBookedEntry(b)) continue;
            String s = dateOnly(b.getStartDate());
            String e = dateOnly(b.getEndDate());
            if (!s.isEmpty() && !e.isEmpty() && today.compareTo(s) >= 0 && today.compareTo(e) <= 0)
                return true;
        }
        return false;
    }

    /**
     * True if cal's calendar date falls inside an OWNER-BLOCKED entry
     * (i.e. NOT a booking).
     */
    private boolean isDateBlocked(Calendar cal) {
        if (blockedDates.isEmpty()) return false;
        String today = toDateStr(cal);
        for (BlockedDateDto b : blockedDates) {
            if (isBookedEntry(b)) continue; // skip customer bookings
            String s = dateOnly(b.getStartDate());
            String e = dateOnly(b.getEndDate());
            if (!s.isEmpty() && !e.isEmpty() && today.compareTo(s) >= 0 && today.compareTo(e) <= 0)
                return true;
        }
        return false;
    }

    /** Entry is a customer booking if type=="booked" OR reason=="Booked" (case-insensitive). */
    private boolean isBookedEntry(BlockedDateDto b) {
        return "booked".equalsIgnoreCase(b.getType())
            || "Booked".equalsIgnoreCase(b.getReason());
    }

    // -------------------------------------------------------------------------
    // SUBMIT BOOKING
    // -------------------------------------------------------------------------

    private void submitBooking() {
        if (isHourlyMode) {
            if (startDateCalendar == null) {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show(); return;
            }
            if (selectedHours < 2) {
                Toast.makeText(this, "Minimum 2 hours required", Toast.LENGTH_SHORT).show(); return;
            }
        } else {
            if (startDateCalendar == null) {
                Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show(); return;
            }
            if (endDateCalendar == null) {
                Toast.makeText(this, "Please select an end date", Toast.LENGTH_SHORT).show(); return;
            }
        }



        Calendar startCal = (Calendar) startDateCalendar.clone();
        Calendar endCal;

        if (isHourlyMode) {
            startCal.set(Calendar.HOUR_OF_DAY, selectedTimeHour);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            endCal = (Calendar) startCal.clone();
            endCal.add(Calendar.HOUR_OF_DAY, selectedHours);
        } else {
            startCal.set(Calendar.HOUR_OF_DAY, selectedStartHour);
            startCal.set(Calendar.MINUTE, selectedStartMin);
            startCal.set(Calendar.SECOND, 0);
            endCal = (Calendar) endDateCalendar.clone();
            endCal.set(Calendar.HOUR_OF_DAY, selectedEndHour);
            endCal.set(Calendar.MINUTE, selectedEndMin);
            endCal.set(Calendar.SECOND, 0);
        }

        String startStr = ISO_FMT.format(startCal.getTime());
        String endStr   = ISO_FMT.format(endCal.getTime());

        CreateBookingRequest req = new CreateBookingRequest(equipment.getEquipmentId(), startStr, endStr);

        apiService.createBooking(req).enqueue(new Callback<CreateBookingResponse>() {
            @Override
            public void onResponse(Call<CreateBookingResponse> call, Response<CreateBookingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String msg = response.body().getMessage();
                    Toast.makeText(ScheduleBookingActivity.this, msg != null ? msg : "Booking successful!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ScheduleBookingActivity.this, BookingsActivity.class);
                    // Clear the back stack so we don't go back into the booking flow
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String err = "Booking failed. Please try again.";
                    try {
                        if (response.errorBody() != null) {
                            err = response.errorBody().string().replace("\"", "");
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(ScheduleBookingActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<CreateBookingResponse> call, Throwable t) {
                Toast.makeText(ScheduleBookingActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // LOADING logic removed (using LoadingInterceptor instead)

    // -------------------------------------------------------------------------
    // CALENDAR ADAPTER
    // -------------------------------------------------------------------------

    private class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.VH> {
        private final List<Date> days;
        CalendarAdapter(List<Date> days) { this.days = days; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Date date = days.get(position);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            h.tvDay.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);

            boolean isPast      = cal.getTimeInMillis() < today.getTimeInMillis();
            boolean isBooked    = isDateBooked(cal);
            boolean isBlocked   = !isBooked && isDateBlocked(cal); // owner-blocked (non-booking)
            boolean isOtherMonth = cal.get(Calendar.MONTH) != currentMonthCalendar.get(Calendar.MONTH);

            boolean isStart   = isSameDay(cal, startDateCalendar);
            boolean isEnd     = isSameDay(cal, endDateCalendar);
            boolean isInRange = !isHourlyMode && isInRange(cal);

            // If the date belongs to another month, completely hide it
            if (isOtherMonth) {
                h.tvDay.setText("");
                h.tvDay.setBackgroundResource(android.R.color.transparent);
                h.itemView.setEnabled(false);
                h.itemView.setOnClickListener(null);
                return;
            }

            // Only show as "Full Day Booked" (Red) if we are in Daily Mode.
            // For Hourly mode, we keep the day clickable so user can pick other hours.
            if (isBooked && !isHourlyMode) {
                // 🔴 Already booked — red/pink background, completely unclickable
                h.tvDay.setBackgroundResource(R.drawable.bg_calendar_day_booked);
                h.tvDay.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                h.tvDay.setAlpha(1f);
                h.itemView.setEnabled(false);
                h.itemView.setOnClickListener(null);
            } else if (isPast || isBlocked) {
                // ⬜ Past or owner-blocked — greyed out
                h.tvDay.setBackgroundResource(R.drawable.bg_calendar_day);
                h.tvDay.setTextColor(getResources().getColor(R.color.text_secondary)); // Changed from border_color to be visible
                h.tvDay.setAlpha(0.4f);
                h.itemView.setEnabled(false);
                h.itemView.setOnClickListener(null);
            } else if (isStart || isEnd) {
                // Selected start/end
                h.tvDay.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                h.tvDay.setTextColor(getResources().getColor(android.R.color.white));
                h.tvDay.setAlpha(1f);
                h.itemView.setEnabled(true);
                h.itemView.setOnClickListener(v -> onDateTapped((Calendar) cal.clone()));
            } else if (isInRange) {
                // In range between start and end
                h.tvDay.setBackgroundColor(getResources().getColor(R.color.agri_light_green));
                h.tvDay.setTextColor(getResources().getColor(R.color.agri_dark_green));
                h.tvDay.setAlpha(1f);
                h.itemView.setEnabled(true);
                h.itemView.setOnClickListener(v -> onDateTapped((Calendar) cal.clone()));
            } else {
                // Normal available day
                h.tvDay.setBackgroundResource(R.drawable.bg_calendar_day);
                h.tvDay.setTextColor(getResources().getColor(R.color.text_primary));
                h.tvDay.setAlpha(1f);
                h.itemView.setEnabled(true);
                h.itemView.setOnClickListener(v -> onDateTapped((Calendar) cal.clone()));
            }
        }

        @Override public int getItemCount() { return days.size(); }

        private boolean isSameDay(Calendar a, Calendar b) {
            if (a == null || b == null) return false;
            return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                   a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
        }

        private boolean isInRange(Calendar cal) {
            if (startDateCalendar == null || endDateCalendar == null) return false;
            return cal.getTimeInMillis() > startDateCalendar.getTimeInMillis() &&
                   cal.getTimeInMillis() < endDateCalendar.getTimeInMillis();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvDay;
            VH(View v) { super(v); tvDay = v.findViewById(R.id.tvCalendarDay); }
        }
    }
}
