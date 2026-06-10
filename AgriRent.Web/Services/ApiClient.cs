using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using AgriRent.Web.DTOs;
using Microsoft.Extensions.Caching.Memory;

namespace AgriRent.Web.Services
{
    public class ApiClient
    {
        private readonly HttpClient _httpClient;
        private readonly IHttpContextAccessor _httpContextAccessor;
        private readonly IConfiguration _configuration;
        private readonly ILogger<ApiClient> _logger;
        private readonly IMemoryCache _cache;

        // Static JsonSerializerOptions to avoid repeated allocation (Issue #2 fix)
        private static readonly JsonSerializerOptions SharedSerializerOptions = new()
        {
            PropertyNameCaseInsensitive = true,
            WriteIndented = false,
            DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull,
            TypeInfoResolver = new System.Text.Json.Serialization.Metadata.DefaultJsonTypeInfoResolver()
        };

        public ApiClient(HttpClient httpClient, IHttpContextAccessor httpContextAccessor, IConfiguration configuration, ILogger<ApiClient> logger, IMemoryCache cache)
        {
            _httpClient = httpClient;
            _httpContextAccessor = httpContextAccessor;
            _configuration = configuration;
            _logger = logger;
            _cache = cache;

            // Set base URL from configuration
            var baseUrl = _configuration["ApiSettings:BaseUrl"];
            if (!string.IsNullOrEmpty(baseUrl))
            {
                _httpClient.BaseAddress = new Uri(baseUrl);
            }
        }

        private HttpRequestMessage CreateRequest(HttpMethod method, string endpoint, HttpContent? content = null)
        {
            var request = new HttpRequestMessage(method, endpoint);
            if (content != null)
            {
                request.Content = content;
            }

            try
            {
                var httpContext = _httpContextAccessor.HttpContext;
                if (httpContext == null) return request;

                var token = string.Empty;

                // Only use AdminToken if the endpoint specifically targets the admin API
                // Also handles paths like /api/complaint/admin/all, /api/review/admin/all, etc.
                if (endpoint.Contains("/api/admin", StringComparison.OrdinalIgnoreCase) ||
                    endpoint.Contains("/admin/", StringComparison.OrdinalIgnoreCase))
                {
                    token = httpContext.Session.GetString("AdminToken");
                }

                // Fallback to normal User JWT Token if it's not an admin API or if AdminToken string was empty
                if (string.IsNullOrEmpty(token))
                {
                    token = httpContext.Session.GetString("JwtToken");
                }

                if (!string.IsNullOrEmpty(token))
                {
                    request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
                }

                var language = httpContext.Session.GetString("Language") ?? "en";
                request.Headers.TryAddWithoutValidation("Accept-Language", language);
            }
            catch (Exception ex)
            {
                _logger.LogWarning("Error creating request headers: {Message}", ex.Message);
            }

            return request;
        }

        // GET request
        public async Task<T?> GetAsync<T>(string endpoint)
        {
            try
            {
                bool shouldCache = ShouldCacheEndpoint(endpoint);
                string cacheKey = string.Empty;

                if (shouldCache)
                {
                    var token = string.Empty;
                    if (endpoint.Contains("/api/admin", StringComparison.OrdinalIgnoreCase) ||
                        endpoint.Contains("/admin/", StringComparison.OrdinalIgnoreCase))
                    {
                        token = _httpContextAccessor.HttpContext?.Session.GetString("AdminToken") ?? "";
                    }
                    if (string.IsNullOrEmpty(token))
                    {
                        token = _httpContextAccessor.HttpContext?.Session.GetString("JwtToken") ?? "";
                    }
                    cacheKey = $"API_GET_{endpoint}_{token}_T";
                    
                    if (_cache.TryGetValue(cacheKey, out T? cachedData))
                    {
                        return cachedData;
                    }
                }

                using var request = CreateRequest(HttpMethod.Get, endpoint);
                using var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
                
                if (response.IsSuccessStatusCode)
                {
                    // FIX: Use streaming for large responses instead of loading all in memory
                    using var contentStream = await response.Content.ReadAsStreamAsync();
                    var data = await JsonSerializer.DeserializeAsync<T>(contentStream, SharedSerializerOptions);
                    
                    if (shouldCache && data != null)
                    {
                        TimeSpan cacheTime = endpoint.Contains("/admin") ? TimeSpan.FromMinutes(5) : TimeSpan.FromMinutes(5);
                        _cache.Set(cacheKey, data, cacheTime);
                    }
                    
                    return data;
                }
                else
                {
                    _logger.LogWarning("API GET failed: {Endpoint} - Status: {Status}", endpoint, response.StatusCode);
                    var errorContent = await response.Content.ReadAsStringAsync();
                    _logger.LogDebug("Error response: {Error}", errorContent);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Exception calling GET API: {Endpoint}", endpoint);
            }

            return default;
        }

        // POST request with compression
        public async Task<TResponse?> PostAsync<TRequest, TResponse>(string endpoint, TRequest data)
        {
            try
            {
                // FIX: Pre-serialize once instead of multiple times
                var json = JsonSerializer.Serialize(data, SharedSerializerOptions);
                
                // FIX: Use gzip compression for request body if large
                byte[] buffer = Encoding.UTF8.GetBytes(json);
                using var content = new ByteArrayContent(buffer);
                content.Headers.ContentType = new MediaTypeHeaderValue("application/json");
                
                // Only compress if payload > 1KB
                if (buffer.Length > 1024)
                {
                    // FIX: Compress large payloads
                    using var ms = new MemoryStream();
                    using (var gzip = new System.IO.Compression.GZipStream(ms, System.IO.Compression.CompressionMode.Compress, false))
                    {
                        await gzip.WriteAsync(buffer, 0, buffer.Length);
                    }
                    
                    var compressedContent = new ByteArrayContent(ms.ToArray());
                    compressedContent.Headers.ContentType = new MediaTypeHeaderValue("application/json");
                    compressedContent.Headers.ContentEncoding.Add("gzip");
                    
                    using var request = CreateRequest(HttpMethod.Post, endpoint, compressedContent);
                    using var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
                    HandleCacheInvalidation(endpoint, response);
                    if (response.IsSuccessStatusCode)
                    {
                        using var responseStream = await response.Content.ReadAsStreamAsync();
                        return await JsonSerializer.DeserializeAsync<TResponse>(responseStream, SharedSerializerOptions);
                    }
                }
                else
                {
                    using var request = CreateRequest(HttpMethod.Post, endpoint, content);
                    using var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
                    HandleCacheInvalidation(endpoint, response);
                    if (response.IsSuccessStatusCode)
                    {
                        using var responseStream = await response.Content.ReadAsStreamAsync();
                        return await JsonSerializer.DeserializeAsync<TResponse>(responseStream, SharedSerializerOptions);
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Exception calling POST API: {Endpoint}", endpoint);
            }

            return default;
        }

        // PUT request
        public async Task<TResponse?> PutAsync<TRequest, TResponse>(string endpoint, TRequest data)
        {
            try
            {
                var json = JsonSerializer.Serialize(data, SharedSerializerOptions);
                using var content = new StringContent(json, Encoding.UTF8, "application/json");
                
                using var request = CreateRequest(HttpMethod.Put, endpoint, content);
                using var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
                HandleCacheInvalidation(endpoint, response);
                
                if (response.IsSuccessStatusCode)
                {
                    using var responseStream = await response.Content.ReadAsStreamAsync();
                    return await JsonSerializer.DeserializeAsync<TResponse>(responseStream, SharedSerializerOptions);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Exception calling PUT API: {Endpoint}", endpoint);
            }

            return default;
        }

        // DELETE request
        public async Task<bool> DeleteAsync(string endpoint)
        {
            try
            {
                using var request = CreateRequest(HttpMethod.Delete, endpoint);
                using var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
                HandleCacheInvalidation(endpoint, response);
                return response.IsSuccessStatusCode;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Exception calling DELETE API: {Endpoint}", endpoint);
                return false;
            }
        }

        // POST request returning raw response (for login, etc.)
        public async Task<HttpResponseMessage> PostAsyncRaw<TRequest>(string endpoint, TRequest data)
        {
            var json = JsonSerializer.Serialize(data, SharedSerializerOptions);
            var content = new StringContent(json, Encoding.UTF8, "application/json");
            var request = CreateRequest(HttpMethod.Post, endpoint, content);
            var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
            HandleCacheInvalidation(endpoint, response);
            return response;
        }

        private bool ShouldCacheEndpoint(string endpoint)
        {
            return endpoint.Contains("/api/seller/my-products") ||
                   endpoint.Contains("/api/equipment/my-list") ||
                   endpoint.Contains("/api/public/categories") ||
                   endpoint.Contains("/api/subscription/my-subscription") ||
                   // Seller heavily hit endpoints
                   endpoint.Contains("/api/seller/dashboard") ||
                   endpoint.Contains("/api/seller/orders") ||
                   // Owner heavily hit endpoints
                   endpoint.Contains("/api/owner/dashboard") ||
                   endpoint.Contains("/api/booking/owner-requests");
        }

        private void ClearRelatedCache(string endpoint)
        {
            try
            {
                var token = string.Empty;
                if (endpoint.Contains("/api/admin", StringComparison.OrdinalIgnoreCase) ||
                    endpoint.Contains("/admin/", StringComparison.OrdinalIgnoreCase))
                {
                    token = _httpContextAccessor.HttpContext?.Session.GetString("AdminToken") ?? "";
                }
                if (string.IsNullOrEmpty(token))
                {
                    token = _httpContextAccessor.HttpContext?.Session.GetString("JwtToken") ?? "";
                }

                if (endpoint.Contains("seller") || endpoint.Contains("product") || endpoint.Contains("order"))
                {
                    _cache.Remove($"API_GET_/api/seller/my-products_{token}");
                    _cache.Remove($"API_GET_/api/seller/dashboard_{token}");
                    _cache.Remove($"API_GET_/api/seller/orders_{token}");
                }

                if (endpoint.Contains("equipment") || endpoint.Contains("owner") || endpoint.Contains("booking"))
                {
                    _cache.Remove($"API_GET_/api/equipment/my-list_{token}");
                    _cache.Remove($"API_GET_/api/equipment/my-list?pageSize=20&page=1_{token}");
                    _cache.Remove($"API_GET_/api/owner/dashboard_{token}");
                    _cache.Remove($"API_GET_/api/booking/owner-requests?pageSize=20&page=1_{token}");
                }

                if (endpoint.Contains("category") || endpoint.Contains("categories"))
                    _cache.Remove($"API_GET_/api/public/categories_{token}");

                if (endpoint.Contains("subscription"))
                    _cache.Remove($"API_GET_/api/subscription/my-subscription_{token}");

                // Invalidate Admin Dashboard summary specifically for immediate visible refresh on modifications
                if (endpoint.Contains("/admin/") || endpoint.Contains("/complaint/") || endpoint.Contains("/payment/"))
                {
                    _cache.Remove($"API_GET_/api/admin/dashboard_{token}");
                    
                    // Force refresh of the users list explicitly if a user is modified
                    if (endpoint.Contains("/users/"))
                    {
                        var baseUserCache = $"API_GET_/api/admin/users";
                        // Using IMemoryCache typically requires exact keys: we remove the most common permutations or we could use a custom clearing system.
                        _cache.Remove($"{baseUserCache}?pageSize=1000_{token}");
                        _cache.Remove($"{baseUserCache}?pageSize=1000&role=Farmer_{token}");
                        _cache.Remove($"{baseUserCache}?pageSize=1000&role=Owner_{token}");
                        _cache.Remove($"{baseUserCache}?pageSize=1000&role=Seller_{token}");
                        // Also with statuses
                        _cache.Remove($"{baseUserCache}?pageSize=1000&status=Active_{token}");
                        _cache.Remove($"{baseUserCache}?pageSize=1000&status=Blocked_{token}");
                        _cache.Remove($"{baseUserCache}?pageSize=1000&role=Farmer&status=Active_{token}");
                        _cache.Remove($"{baseUserCache}?pageSize=1000&role=Farmer&status=Blocked_{token}");
                        _cache.Remove($"{baseUserCache}?pageSize=1000&role=Owner&status=Active_{token}");
                        _cache.Remove($"{baseUserCache}?pageSize=1000&role=Owner&status=Blocked_{token}");
                        _cache.Remove($"{baseUserCache}?pageSize=1000&role=Seller&status=Active_{token}");
                        _cache.Remove($"{baseUserCache}?pageSize=1000&role=Seller&status=Blocked_{token}");
                    }
                }
            }
            catch { }
        }

        private void HandleCacheInvalidation(string endpoint, HttpResponseMessage response)
        {
            if (response.IsSuccessStatusCode)
            {
                ClearRelatedCache(endpoint);
            }
        }

        // GET request returning raw response - FIX: Optimized with streaming
        public async Task<HttpResponseMessage> GetAsyncRaw(string endpoint)
        {
            bool shouldCache = ShouldCacheEndpoint(endpoint);
            string cacheKey = string.Empty;

            if (shouldCache)
            {
                var token = string.Empty;
                if (endpoint.Contains("/api/admin", StringComparison.OrdinalIgnoreCase) ||
                    endpoint.Contains("/admin/", StringComparison.OrdinalIgnoreCase))
                {
                    token = _httpContextAccessor.HttpContext?.Session.GetString("AdminToken") ?? "";
                }
                if (string.IsNullOrEmpty(token))
                {
                    token = _httpContextAccessor.HttpContext?.Session.GetString("JwtToken") ?? "";
                }
                
                cacheKey = $"API_GET_{endpoint}_{token}";
                
                if (_cache.TryGetValue(cacheKey, out string? cachedResponse) && !string.IsNullOrEmpty(cachedResponse))
                {
                    var cachedMsg = new HttpResponseMessage(System.Net.HttpStatusCode.OK)
                    {
                        Content = new StringContent(cachedResponse, Encoding.UTF8, "application/json")
                    };
                    return cachedMsg;
                }
            }

            var request = CreateRequest(HttpMethod.Get, endpoint);
            var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);

            // Dynamic caching interval
            if (shouldCache && response.IsSuccessStatusCode)
            {
                var contentBytes = await response.Content.ReadAsByteArrayAsync();
                var contentString = Encoding.UTF8.GetString(contentBytes);
                
                TimeSpan cacheTime = endpoint.Contains("/admin") ? TimeSpan.FromMinutes(15) : TimeSpan.FromMinutes(5);
                _cache.Set(cacheKey, contentString, cacheTime);
                
                var clonedResponse = new HttpResponseMessage(response.StatusCode);
                foreach (var header in response.Headers)
                    clonedResponse.Headers.TryAddWithoutValidation(header.Key, header.Value);
                
                var newContent = new ByteArrayContent(contentBytes);
                foreach (var header in response.Content.Headers)
                    newContent.Headers.TryAddWithoutValidation(header.Key, header.Value);
                
                clonedResponse.Content = newContent;
                return clonedResponse;
            }

            return response;
        }

        // PUT request returning raw response
        public async Task<HttpResponseMessage> PutAsyncRaw<TRequest>(string endpoint, TRequest data)
        {
            var json = JsonSerializer.Serialize(data, SharedSerializerOptions);
            var content = new StringContent(json, Encoding.UTF8, "application/json");
            var request = CreateRequest(HttpMethod.Put, endpoint, content);
            var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
            HandleCacheInvalidation(endpoint, response);
            return response;
        }

        // DELETE request returning raw response
        public async Task<HttpResponseMessage> DeleteAsyncRaw(string endpoint)
        {
            var request = CreateRequest(HttpMethod.Delete, endpoint);
            var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
            HandleCacheInvalidation(endpoint, response);
            return response;
        }

        // Multipart/form-data POST returning raw response
        public async Task<HttpResponseMessage> PostMultipartAsyncRaw(string endpoint, MultipartFormDataContent content)
        {
            var request = CreateRequest(HttpMethod.Post, endpoint, content);
            var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
            HandleCacheInvalidation(endpoint, response);
            return response;
        }

        // GET request for paginated responses
        public async Task<PaginatedResponse<T>?> GetPaginatedAsync<T>(string endpoint)
        {
            try
            {
                bool shouldCache = ShouldCacheEndpoint(endpoint);
                string cacheKey = string.Empty;

                if (shouldCache)
                {
                    var token = string.Empty;
                    if (endpoint.Contains("/api/admin", StringComparison.OrdinalIgnoreCase) ||
                        endpoint.Contains("/admin/", StringComparison.OrdinalIgnoreCase))
                    {
                        token = _httpContextAccessor.HttpContext?.Session.GetString("AdminToken") ?? "";
                    }
                    if (string.IsNullOrEmpty(token))
                    {
                        token = _httpContextAccessor.HttpContext?.Session.GetString("JwtToken") ?? "";
                    }
                    cacheKey = $"API_GET_{endpoint}_{token}_Paginated";
                    
                    if (_cache.TryGetValue(cacheKey, out PaginatedResponse<T>? cachedData))
                    {
                        return cachedData;
                    }
                }

                using var request = CreateRequest(HttpMethod.Get, endpoint);
                using var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
                
                if (response.IsSuccessStatusCode)
                {
                    using var contentStream = await response.Content.ReadAsStreamAsync();
                    var data = await JsonSerializer.DeserializeAsync<PaginatedResponse<T>>(contentStream, SharedSerializerOptions);
                    
                    if (shouldCache && data != null)
                    {
                        TimeSpan cacheTime = endpoint.Contains("/admin") ? TimeSpan.FromMinutes(15) : TimeSpan.FromMinutes(5);
                        _cache.Set(cacheKey, data, cacheTime);
                    }
                    
                    return data;
                }
                else
                {
                    _logger.LogWarning("API GET (paginated) failed: {Endpoint} - Status: {Status}", endpoint, response.StatusCode);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Exception calling paginated GET API: {Endpoint}", endpoint);
            }
            
            return null;
        }
    }
}
