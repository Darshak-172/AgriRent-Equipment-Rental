package com.example.agrirent.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agrirent.R;
import com.example.agrirent.adapters.SearchResultAdapter;
import com.example.agrirent.models.SearchResponse;
import com.example.agrirent.network.ApiClient;
import com.example.agrirent.network.ApiService;
import com.example.agrirent.utils.LocaleHelper;
import com.example.agrirent.utils.SessionManager;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchResultsActivity extends BaseActivity {

    private SessionManager session;
    private ApiService apiService;

    // View References
    private EditText etSearchQuery;
    private ImageButton btnBack, btnClearSearch, btnVoiceSearch;
    private RecyclerView rvSearchResults;
    private ProgressBar pbSearchLoading;
    private LinearLayout llEmptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;

    private SearchResultAdapter adapter;

    // Debounce mechanics
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private Call<SearchResponse> currentApiCall;
    private static final long SEARCH_DELAY_MS = 500;
    private final ActivityResultLauncher<Intent> voiceSearchLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenQuery = matches.get(0).trim();
                        etSearchQuery.setText(spokenQuery);
                        etSearchQuery.setSelection(spokenQuery.length());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        session = new SessionManager(this);
        LocaleHelper.applyLanguage(this, session.getLanguage());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        initViews();
        setupRecyclerView();
        setupListeners();
        handleIntent();
    }

    private void initViews() {
        etSearchQuery = findViewById(R.id.etSearchQuery);
        btnBack = findViewById(R.id.btnBack);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        pbSearchLoading = findViewById(R.id.pbSearchLoading);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
    }

    private void setupRecyclerView() {
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchResultAdapter(this, new ArrayList<>());
        rvSearchResults.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnClearSearch.setOnClickListener(v -> {
            etSearchQuery.setText("");
            showEmptyState("Search inventory", "Find equipment, products, and sellers quickly.");
            btnClearSearch.setVisibility(View.GONE);
            adapter.updateData(new ArrayList<>());
        });

        if (btnVoiceSearch != null) {
            btnVoiceSearch.setOnClickListener(v -> launchVoiceSearch());
        }

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Remove previous callbacks to cancel old searches
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString().trim();

                if (query.isEmpty()) {
                    btnClearSearch.setVisibility(View.GONE);
                    adapter.updateData(new ArrayList<>());
                    showEmptyState("Search inventory", "Find equipment, products, and sellers quickly.");
                } else {
                    btnClearSearch.setVisibility(View.VISIBLE);
                    // Schedule a new search after user stops typing
                    searchRunnable = () -> performSearch(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void handleIntent() {
        String initialQuery = getIntent().getStringExtra("search_keyword");
        if (initialQuery != null && !initialQuery.isEmpty()) {
            etSearchQuery.setText(initialQuery);
            etSearchQuery.setSelection(initialQuery.length()); // Move cursor to end
            // TextWatcher will automatically pick this up and perform the search
        } else {
            // Request focus and show keyboard
            etSearchQuery.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT);
            }
            showEmptyState("Search inventory", "Find equipment, products, and sellers quickly.");
        }
    }

    private void performSearch(String keyword) {
        // Cancel ongoing calls safely
        if (currentApiCall != null && !currentApiCall.isCanceled()) {
            currentApiCall.cancel();
        }

        showLoading();

        currentApiCall = apiService.searchApp(keyword, 1, 50); // Fetch top 50 results
        currentApiCall.enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                hideLoading();
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getData() != null && !response.body().getData().isEmpty()) {
                        showResults();
                        adapter.updateData(response.body().getData());
                    } else {
                        adapter.updateData(new ArrayList<>());
                        showEmptyState("No results found", "We couldn't find anything matching '" + keyword + "'. Try different keywords.");
                    }
                } else {
                    adapter.updateData(new ArrayList<>());
                    showEmptyState("Search Failed", "An error occurred while searching. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                if (!call.isCanceled()) {
                    hideLoading();
                    adapter.updateData(new ArrayList<>());
                    showEmptyState("Network Error", "Please check your internet connection.");
                }
            }
        });
    }

    private void showLoading() {
        pbSearchLoading.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        pbSearchLoading.setVisibility(View.GONE);
    }

    private void showResults() {
        rvSearchResults.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
    }

    private void showEmptyState(String title, String subtitle) {
        llEmptyState.setVisibility(View.VISIBLE);
        rvSearchResults.setVisibility(View.GONE);
        tvEmptyTitle.setText(title);
        tvEmptySubtitle.setText(subtitle);
    }

    private void launchVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toString());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak what you want to find");
        try {
            voiceSearchLauncher.launch(intent);
        } catch (Exception e) {
            showEmptyState("Voice search unavailable", "Please type your query instead.");
        }
    }
}
