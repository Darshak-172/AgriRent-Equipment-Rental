package com.example.agrirent.network;

import com.example.agrirent.models.ApiCategory;
import com.example.agrirent.models.ChangePasswordRequest;
import com.example.agrirent.models.Equipment;
import com.example.agrirent.models.ForgotPasswordRequest;
import com.example.agrirent.models.GenericResponse;
import com.example.agrirent.models.LoginRequest;
import com.example.agrirent.models.LoginResponse;
import com.example.agrirent.models.MySubscriptionResponse;
import com.example.agrirent.models.AddProductRequest;
import com.example.agrirent.models.AddProductResponse;
import com.example.agrirent.models.ProductItem;
import com.example.agrirent.models.RegisterRequest;
import com.example.agrirent.models.SendOtpRequest;
import com.example.agrirent.models.SendOtpResponse;
import com.example.agrirent.models.OtpLoginRequest;
import com.example.agrirent.models.PaginatedResponse;
import com.example.agrirent.models.RefreshRequest;
import com.example.agrirent.models.RefreshResponse;
import com.example.agrirent.models.SubscriptionOrder;
import com.example.agrirent.models.SubscriptionPlansResponse;
import com.example.agrirent.models.VerifyPaymentRequest;
import com.example.agrirent.models.VerifyPaymentResponse;
import com.example.agrirent.models.CreateBookingRequest;
import com.example.agrirent.models.CreateBookingResponse;
import com.example.agrirent.models.BlockedDateDto;
import com.example.agrirent.models.BookingResponseDto;
import com.example.agrirent.models.MessageResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ========== AUTH ==========

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/update-fcm-token")
    Call<Void> updateFcmToken(@Body com.example.agrirent.models.FcmTokenRequest request);

    @POST("api/auth/otp-register")
    Call<LoginResponse> otpRegister(@Body RegisterRequest request);

    @POST("api/auth/send-otp")
    Call<SendOtpResponse> sendOtp(@Body SendOtpRequest request);

    @POST("api/auth/otp-login")
    Call<LoginResponse> otpLogin(@Body OtpLoginRequest request);

    @POST("api/auth/otp-forgot")
    Call<ResponseBody> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/auth/otp-reset")
    Call<String> changePassword(@Body ChangePasswordRequest request);

    @POST("api/auth/refresh")
    Call<RefreshResponse> refresh(@Body RefreshRequest request);

    // ========== HOME PAGE DATA ==========

    @GET("api/public/categories")
    Call<List<ApiCategory>> getCategories(@Query("type") String type);

    @retrofit2.http.Headers("No-Loading: true")
    @GET("api/public/equipment/available")
    Call<PaginatedResponse<Equipment>> getAvailableEquipment(
            @Query("page") int page,
            @Query("pageSize") int pageSize
    );

    @GET("api/equipment/my-list")
    Call<List<Equipment>> getMyEquipments();

    @POST("api/equipment/add")
    Call<com.example.agrirent.models.AddEquipmentResponse> addEquipment(@Body com.example.agrirent.models.AddEquipmentRequest request);

    @retrofit2.http.Multipart
    @POST("api/equipment/upload-image")
    Call<com.example.agrirent.models.ImageUploadResponse> uploadEquipmentImage(
            @retrofit2.http.Part("equipmentId") okhttp3.RequestBody equipmentId,
            @retrofit2.http.Part okhttp3.MultipartBody.Part image
    );

    @retrofit2.http.Headers("No-Loading: true")
    @GET("api/products/list")
    Call<PaginatedResponse<ProductItem>> getProducts(
            @Query("page") int page,
            @Query("pageSize") int pageSize
    );

    @POST("api/seller/add-product")
    Call<AddProductResponse> addProduct(@Body AddProductRequest request);

    @retrofit2.http.Multipart
    @POST("api/seller/upload-product-image")
    Call<com.example.agrirent.models.ImageUploadResponse> uploadProductImage(
            @retrofit2.http.Part("productId") okhttp3.RequestBody productId,
            @retrofit2.http.Part okhttp3.MultipartBody.Part image
    );

    // ========== SEARCH ==========

    @GET("api/Search")
    Call<com.example.agrirent.models.SearchResponse> searchApp(
            @retrofit2.http.Query("keyword") String keyword,
            @retrofit2.http.Query("page") int page,
            @retrofit2.http.Query("pageSize") int pageSize
    );

    // ========== SUBSCRIPTION — USER ENDPOINTS ==========

    /** GET /api/subscription-plans — Public, no auth needed */
    @GET("api/subscription-plans")
    Call<SubscriptionPlansResponse> getSubscriptionPlans();

    /** POST /api/subscription/create-order/{planId} — Auth required */
    @POST("api/subscription/create-order/{planId}")
    Call<SubscriptionOrder> createSubscriptionOrder(@Path("planId") int planId);

    /** POST /api/subscription/verify-payment — Auth required */
    @POST("api/subscription/verify-payment")
    Call<VerifyPaymentResponse> verifyPayment(@Body VerifyPaymentRequest request);

    /** GET /api/subscription/my-subscription — Auth required; returns active subscriptions */
    @GET("api/subscription/my-subscription")
    Call<MySubscriptionResponse> getMySubscriptions();

    /** GET /api/payment/history — Auth required; returns paginated payment history */
    @GET("api/payment/history")
    Call<com.example.agrirent.models.PaymentHistoryResponse> getPaymentHistory(
            @retrofit2.http.Query("pageSize") int pageSize,
            @retrofit2.http.Query("pageNumber") int pageNumber
    );

    // ========== RATINGS ==========
    @POST("api/review")
    Call<okhttp3.ResponseBody> submitRating(@Body com.example.agrirent.models.CreateRatingRequest request);

    @GET("api/review/{type}/{targetId}")
    Call<com.example.agrirent.models.IncomingReviewsResponse> getRatings(
            @retrofit2.http.Path("type") String type, 
            @retrofit2.http.Path("targetId") int targetId
    );

    // ========== BOOKING ==========
    @POST("api/booking/create")
    Call<CreateBookingResponse> createBooking(@Body CreateBookingRequest request);

    @GET("api/booking/my-bookings")
    Call<List<BookingResponseDto>> getMyBookings();

    @GET("api/booking/owner-requests")
    Call<List<BookingResponseDto>> getOwnerRequests();

    @POST("api/booking/accept/{bookingId}")
    Call<String> acceptBooking(@Path("bookingId") int bookingId);

    @POST("api/booking/reject/{bookingId}")
    Call<String> rejectBooking(@Path("bookingId") int bookingId);

    // ========== PRODUCT ORDERS ==========
    @POST("api/products/place-order")
    Call<com.example.agrirent.models.GenericResponse> placeProductOrder(@Body com.example.agrirent.models.CreateOrderRequest request);

    @GET("api/products/my-orders")
    Call<List<com.example.agrirent.models.OrderResponseDto>> getMyProductOrders();

    @PUT("api/products/cancel-order/{orderId}")
    Call<String> cancelProductOrder(@Path("orderId") int orderId);

    @GET("api/seller/orders")
    Call<List<com.example.agrirent.models.OrderResponseDto>> getSellerOrders();

    // --- Complaints (Incoming for Owners/Sellers) ---
    @GET("api/complaint/incoming")
    Call<List<com.example.agrirent.models.ComplaintResponse>> getIncomingComplaints();

    @PUT("api/complaint/{id}/resolve")
    Call<com.example.agrirent.models.MessageResponse> resolveComplaint(@Path("id") int complaintId, @Body com.example.agrirent.models.ResolutionRequest req);

    // --- Reviews (Incoming for Owners/Sellers) ---
    @GET("api/review/incoming")
    Call<com.example.agrirent.models.IncomingReviewsResponse> getIncomingReviews();

    @PUT("api/seller/update-order-status/{orderId}")
    Call<okhttp3.ResponseBody> updateSellerOrderStatus(@Path("orderId") int orderId, @Body okhttp3.RequestBody statusJson);

    // ========== EQUIPMENT AVAILABILITY ==========
    @GET("api/seller/my-products")
    Call<List<ProductItem>> getMyProducts();

    @retrofit2.http.DELETE("api/seller/delete-product/{id}")
    Call<String> deleteProduct(@Path("id") int productId);

    @PUT("api/seller/toggle-product-status/{id}")
    Call<String> toggleProductStatus(@Path("id") int productId);

    @PUT("api/seller/update-product/{id}")
    Call<String> updateProduct(@Path("id") int productId, @Body AddProductRequest request);

    @GET("api/equipment/blocked-dates/{equipmentId}")
    Call<List<BlockedDateDto>> getBlockedDates(@Path("equipmentId") int equipmentId);

    @retrofit2.http.DELETE("api/equipment/delete/{id}")
    Call<String> deleteEquipment(@Path("id") int equipmentId);

    @PUT("api/equipment/toggle-status/{id}")
    Call<String> toggleEquipmentStatus(@Path("id") int equipmentId);

    @PUT("api/equipment/update/{id}")
    Call<String> updateEquipment(@Path("id") int equipmentId, @Body com.example.agrirent.models.AddEquipmentRequest request);

    // ========== COMPLAINTS ==========
    @POST("api/complaint")
    Call<okhttp3.ResponseBody> submitComplaint(@Body com.example.agrirent.models.CreateComplaintRequest request);

    @GET("api/complaint/my")
    Call<List<com.example.agrirent.models.ComplaintResponse>> getMyComplaints();

    @GET("api/review/my")
    Call<List<com.example.agrirent.models.RatingResponse>> getMyRatings();

    @GET("api/public/equipment/{id}")
    Call<com.example.agrirent.models.EquipmentNameResponse> getEquipmentById(@Path("id") int id);

    @GET("api/products/{id}")
    Call<com.example.agrirent.models.ProductNameResponse> getProductById(@Path("id") int id);
}
