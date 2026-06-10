using AgriRent.Data;
using AgriRent.Models;
using AgriRent.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using System.Text;
using FirebaseAdmin;
using Google.Apis.Auth.OAuth2;

var builder = WebApplication.CreateBuilder(args);

// ==============================
// API Controllers Only
// ==============================
builder.Services.AddControllers();

// ==============================
// Rate Limiting
// ==============================
builder.Services.AddRateLimiter(options =>
{
    options.GlobalLimiter = System.Threading.RateLimiting.PartitionedRateLimiter.Create<HttpContext, string>(context =>
    {
        return System.Threading.RateLimiting.RateLimitPartition.GetFixedWindowLimiter(
            partitionKey: context.User.Identity?.Name ?? context.Request.Headers.Host.ToString(),
            factory: _ => new System.Threading.RateLimiting.FixedWindowRateLimiterOptions
            {
                PermitLimit = 1000,
                Window = TimeSpan.FromMinutes(1),
                QueueProcessingOrder = System.Threading.RateLimiting.QueueProcessingOrder.OldestFirst,
                QueueLimit = 50
            });
    });
});

// ==============================
// Health Checks & Compression
// ==============================
builder.Services.AddHealthChecks();
builder.Services.AddResponseCompression(options =>
{
    options.EnableForHttps = true;
    options.Providers.Add<Microsoft.AspNetCore.ResponseCompression.BrotliCompressionProvider>();
    options.Providers.Add<Microsoft.AspNetCore.ResponseCompression.GzipCompressionProvider>();
});

// SMS Service
builder.Services.AddHttpClient<TwoFactorService>();
builder.Services.AddScoped<TwoFactorService>();
//builder.Services.AddSingleton<TwoFactorService>();

// Translation Service
builder.Services.AddHttpContextAccessor(); // Required for LanguageHelper
builder.Services.AddHttpClient<TranslateService>();
builder.Services.AddScoped<LanguageHelper>();

// ==============================
// Database
// ==============================
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseSqlServer(
        builder.Configuration.GetConnectionString("DefaultConnection")
    )
);

// ==============================
// Session & Memory Caching Support
// ==============================
builder.Services.AddMemoryCache(); // 🚀 Enable In-Memory Caching
builder.Services.AddDistributedMemoryCache();
builder.Services.AddSession(options =>
{
    options.IdleTimeout = TimeSpan.FromHours(8);
    options.Cookie.HttpOnly = true;
    options.Cookie.IsEssential = true;
});

// ==============================
// JWT Authentication
// ==============================
var jwtKey = builder.Configuration["JwtSettings:Key"];
if (string.IsNullOrWhiteSpace(jwtKey))
    throw new Exception("JwtSettings:Key missing");

var keyBytes = Encoding.UTF8.GetBytes(jwtKey);

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
.AddJwtBearer(options =>
{
    options.RequireHttpsMetadata = false;
    options.SaveToken = true;

    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuer = true,
        ValidateAudience = true,
        ValidateLifetime = true,
        ValidateIssuerSigningKey = true,
        ValidIssuer = builder.Configuration["JwtSettings:Issuer"],
        ValidAudience = builder.Configuration["JwtSettings:Audience"],
        IssuerSigningKey = new SymmetricSecurityKey(keyBytes),
        ClockSkew = TimeSpan.Zero
    };

    // 🔴 ADD THIS PART
    options.Events = new JwtBearerEvents
    {
        OnChallenge = async context =>
        {
            context.HandleResponse();

            context.Response.StatusCode = StatusCodes.Status401Unauthorized;
            context.Response.ContentType = "application/json";

            await context.Response.WriteAsync(
                System.Text.Json.JsonSerializer.Serialize(new
                {
                    success = false,
                    statusCode = 401,
                    message = "Unauthorized access. Token is missing or invalid."
                })
            );
        },

        OnForbidden = async context =>
        {
            context.Response.StatusCode = StatusCodes.Status403Forbidden;
            context.Response.ContentType = "application/json";

            await context.Response.WriteAsync(
                System.Text.Json.JsonSerializer.Serialize(new
                {
                    success = false,
                    statusCode = 403,
                    message = "Access denied. You do not have the required role for this action."
                })
            );
        }
    };
});


builder.Services.AddAuthorization();

// ==============================
// Custom Services
// ==============================
// TranslateService is already registered via AddHttpClient above
builder.Services.AddScoped<SubscriptionExpiryService>();
builder.Services.AddScoped<JwtTokenService>();
// Notification Service
builder.Services.AddScoped<NotificationService>();

// ==============================
// Firebase Configuration
// ==============================
// Priority 1: Environment variable (Azure / production)
// Priority 2: Local JSON file (development)
var startupLogger = LoggerFactory.Create(b => b.AddConsole()).CreateLogger("Startup");
if (FirebaseApp.DefaultInstance == null)
{
    var firebaseJson = Environment.GetEnvironmentVariable("FIREBASE_CREDENTIALS_JSON");
    if (!string.IsNullOrEmpty(firebaseJson))
    {
        FirebaseApp.Create(new AppOptions()
        {
            Credential = CredentialFactory.FromJson<ServiceAccountCredential>(firebaseJson).ToGoogleCredential()
        });
        startupLogger.LogInformation("[FCM] Firebase initialized from environment variable.");
    }
    else
    {
        var firebaseKeyPath = Path.Combine(Directory.GetCurrentDirectory(), "agrirentproject-firebase-adminsdk.json");
        if (File.Exists(firebaseKeyPath))
        {
            FirebaseApp.Create(new AppOptions()
            {
                Credential = CredentialFactory.FromFile<ServiceAccountCredential>(firebaseKeyPath).ToGoogleCredential()
            });
            startupLogger.LogInformation("[FCM] Firebase initialized from local JSON file.");
        }
        else
        {
            startupLogger.LogWarning("[WARN] Firebase credentials not found. Notifications will not work.");
        }
    }
}
// Cloudinary Storage Service
builder.Services.AddScoped<CloudinaryStorageService>();

// Background Services
builder.Services.AddHostedService<SubscriptionExpiryBackgroundService>();



// ==============================
// CORS
// ==============================
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader());
});

var app = builder.Build();

// ==============================
// Seed Roles
// ==============================
try
{
    using var scope = app.Services.CreateScope();
    var context = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    var roles = new[] { "Farmer", "EquipmentOwner", "Seller", "Owner" };
    foreach (var roleName in roles)
    {
        if (!context.Roles.Any(r => r.RoleName == roleName))
            context.Roles.Add(new Role { RoleName = roleName });
    }
    context.SaveChanges();

    // ── One-time data migration: rename legacy "Approved" status → "Active" ──
    context.Database.ExecuteSqlRaw(
        "UPDATE Equipments SET Status = 'Active' WHERE Status = 'Approved'");
    context.Database.ExecuteSqlRaw(
        "UPDATE Products SET Status = 'Active' WHERE Status = 'Approved'");

    // ── Ensure unique index on Ratings (one review per user per item) ──
    context.Database.ExecuteSqlRaw(@"
        IF NOT EXISTS (
            SELECT 1 FROM sys.indexes
            WHERE name = 'IX_Ratings_FromUserId_TargetId_RatingType'
              AND object_id = OBJECT_ID('Ratings')
        )
        BEGIN
            CREATE UNIQUE INDEX IX_Ratings_FromUserId_TargetId_RatingType
            ON Ratings (FromUserId, TargetId, RatingType)
        END");

    // ── Complaint table: add new columns if missing ──
    context.Database.ExecuteSqlRaw(@"
        IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Complaints') AND name = 'TargetId')
            ALTER TABLE Complaints ADD TargetId int NOT NULL DEFAULT 0");
    context.Database.ExecuteSqlRaw(@"
        IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Complaints') AND name = 'TargetType')
            ALTER TABLE Complaints ADD TargetType nvarchar(20) NOT NULL DEFAULT 'Equipment'");
    context.Database.ExecuteSqlRaw(@"
        IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Complaints') AND name = 'ResolutionNote')
            ALTER TABLE Complaints ADD ResolutionNote nvarchar(1000) NULL");
    context.Database.ExecuteSqlRaw(@"
        IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Complaints') AND name = 'ResolvedByUserId')
            ALTER TABLE Complaints ADD ResolvedByUserId int NULL");
    context.Database.ExecuteSqlRaw(@"
        IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Complaints') AND name = 'ResolvedAt')
            ALTER TABLE Complaints ADD ResolvedAt datetime2 NULL");
}
catch (Exception ex)
{
    Console.WriteLine($"[WARN] Role seed skipped (DB may not be ready): {ex.Message}");
}


// ==============================
// Middleware Pipeline (ORDER MATTERS)
// ==============================

// Global Exception Handler
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Error");
    app.UseHsts();
}

app.UseResponseCompression();

app.UseHttpsRedirection();

app.UseRouting();

app.UseCors("AllowAll");

app.UseRateLimiter(); // ✅ Rate limiting

app.UseSession();          // ✅ MUST be before auth
app.UseAuthentication();
app.UseAuthorization();

// ==============================
// Routing
// ==============================
app.MapControllers();
app.MapHealthChecks("/health");

// Firebase status diagnostic endpoint (no auth required)
app.MapGet("/firebase-status", () =>
{
    var isInitialized = FirebaseApp.DefaultInstance != null;
    var hasEnvVar = !string.IsNullOrEmpty(Environment.GetEnvironmentVariable("FIREBASE_CREDENTIALS_JSON"));
    var hasLocalFile = File.Exists(Path.Combine(Directory.GetCurrentDirectory(), "agrirentproject-firebase-adminsdk.json"));
    return Results.Ok(new
    {
        firebaseInitialized = isInitialized,
        sourceEnvVariable = hasEnvVar,
        sourceLocalFile = hasLocalFile
    });
});

app.Run();
