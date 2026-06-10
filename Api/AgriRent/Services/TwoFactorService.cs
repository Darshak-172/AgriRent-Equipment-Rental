using System.Text.Json;

namespace AgriRent.Services
{
    public class TwoFactorService
    {
        private readonly HttpClient _http;
        private readonly IConfiguration _config;

        public TwoFactorService(HttpClient http, IConfiguration config)
        {
            _http = http;
            _config = config;
        }

        private string ApiKey => _config["TwoFactor:ApiKey"] ?? throw new InvalidOperationException("TwoFactor:ApiKey not configured");
        private bool IsRealOtp => bool.TryParse(_config["TwoFactor:IsRealOtp"], out var val) ? val : true;

        // ✅ SEND OTP
        public async Task<string> SendOtp(string mobile)
        {
            if (!IsRealOtp)
            {
                // Fake mode
                return "FAKE_SESSION";
            }

            string url =
                $"https://2factor.in/API/V1/{ApiKey}/SMS/{mobile}/AUTOGEN";

            var response = await _http.GetStringAsync(url);

            var json = JsonDocument.Parse(response);

            if (json.RootElement.GetProperty("Status").GetString() != "Success")
                throw new Exception("2Factor OTP send failed");

            return json.RootElement.GetProperty("Details").GetString() ?? "UNKNOWN_SESSION";
        }

        // ✅ VERIFY OTP
        public async Task<bool> VerifyOtp(string sessionId, string otp)
        {
            if (!IsRealOtp)
            {
                return otp == "123456";
            }

            string url =
                $"https://2factor.in/API/V1/{ApiKey}/SMS/VERIFY/{sessionId}/{otp}";

            var response = await _http.GetAsync(url);
            var body = await response.Content.ReadAsStringAsync();

            Console.WriteLine("2FACTOR VERIFY RESPONSE: " + body);

            if (!response.IsSuccessStatusCode)
                return false; // ❗ DO NOT CRASH — JUST RETURN FALSE

            var json = JsonDocument.Parse(body);
            var status = json.RootElement.GetProperty("Status").GetString();

            return status == "Success";
        }
    }
}