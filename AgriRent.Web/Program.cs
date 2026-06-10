using AgriRent.Web.Services;
using Microsoft.AspNetCore.ResponseCompression;
using System.IO.Compression;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllersWithViews(options =>
{
    options.Filters.Add<AgriRent.Web.Filters.SessionGuardFilter>();
});

// Enable Response Compression with Gzip (Issue #17 fix - improves payload size)
builder.Services.AddResponseCompression(options =>
{
    options.EnableForHttps = true;
    options.Providers.Add<GzipCompressionProvider>();
});

builder.Services.Configure<GzipCompressionProviderOptions>(options =>
{
    options.Level = CompressionLevel.Optimal;
});

// Add HttpContextAccessor for accessing session in services
builder.Services.AddHttpContextAccessor();

// Add Session support (Issue #12 fix - Reduced from 8 hours to 2 hours)
builder.Services.AddSession(options =>
{
    options.IdleTimeout = TimeSpan.FromHours(2);
    options.Cookie.HttpOnly = true;
    options.Cookie.IsEssential = true;
    // Suppress cookie decryption errors in development (old cookies from different keys)
    options.Cookie.SecurePolicy = CookieSecurePolicy.None;
});

// Register HttpClient and ApiClient service with connection pooling and resilience.
builder.Services.AddHttpClient<ApiClient>(client =>
{
    client.Timeout = TimeSpan.FromSeconds(60);
})
.ConfigurePrimaryHttpMessageHandler(() => new SocketsHttpHandler
{
    AutomaticDecompression = System.Net.DecompressionMethods.GZip | System.Net.DecompressionMethods.Deflate,
    PooledConnectionLifetime = TimeSpan.FromMinutes(10),
    PooledConnectionIdleTimeout = TimeSpan.FromMinutes(2)
});

// Add in-memory caching
builder.Services.AddMemoryCache();

// Register response caching services used by the middleware below.
builder.Services.AddResponseCaching();

// Register custom cache service
builder.Services.AddScoped<ICacheService, CacheService>();

var app = builder.Build();

// Enable Response Compression Middleware (Issue #17)
app.UseResponseCompression();

// Add response caching middleware (Issue #8 fix)
app.UseResponseCaching();

// Configure the HTTP request pipeline.
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
    // The default HSTS value is 30 days. You may want to change this for production scenarios, see https://aka.ms/aspnetcore-hsts.
    app.UseHsts();
}

// Disable HTTPS redirection in development
if (!app.Environment.IsDevelopment())
{
    app.UseHttpsRedirection();
}

app.UseStaticFiles(new StaticFileOptions
{
    OnPrepareResponse = ctx =>
    {
        // Cache static files for 30 days
        const int durationInSeconds = 60 * 60 * 24 * 30;
        ctx.Context.Response.Headers[Microsoft.Net.Http.Headers.HeaderNames.CacheControl] =
            "public,max-age=" + durationInSeconds;
    }
});

app.UseRouting();

// Enable session BEFORE authorization
app.UseSession();

// Set default language if not already set
app.Use(async (context, next) =>
{
    if (string.IsNullOrEmpty(context.Session.GetString("Language")))
    {
        context.Session.SetString("Language", "en"); // Default to English
    }
    await next();
});

app.UseAuthorization();

app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

app.Run();
