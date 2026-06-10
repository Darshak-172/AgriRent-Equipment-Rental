using System.Text;
using System.Text.Json;

namespace AgriRent.Services
{
    public class TranslateService
    {
        private readonly HttpClient _httpClient;
        private readonly string _endpoint;
        private readonly string _key;
        private readonly string _region;

        public TranslateService(HttpClient httpClient, IConfiguration config)
        {
            _httpClient = httpClient;
            _endpoint = config["AzureTranslate:Endpoint"] ?? throw new ArgumentNullException("AzureTranslate:Endpoint");
            _key = config["AzureTranslate:Key"] ?? throw new ArgumentNullException("AzureTranslate:Key");
            _region = config["AzureTranslate:Region"] ?? throw new ArgumentNullException("AzureTranslate:Region");
            
            _httpClient.DefaultRequestHeaders.Add("Ocp-Apim-Subscription-Key", _key);
            _httpClient.DefaultRequestHeaders.Add("Ocp-Apim-Subscription-Region", _region);
        }

        // 📌 MAIN METHOD USED IN CONTROLLER
        public async Task<string> Translate(string? text, string toLanguage)
        {
            if (string.IsNullOrWhiteSpace(text))
                return ""; // Return empty string instead of null

            // ⭐ For saving to DB (Gujarati/Hindi → English transliteration)
            if (toLanguage == "en")
            {
                var fromGu = await Transliterate(text, "gu", "Gujr", "Latn");
                if (fromGu != text) return fromGu;

                var fromHi = await Transliterate(text, "hi", "Deva", "Latn");
                if (fromHi != text) return fromHi;

                return text; // already English
            }

            // ⭐ For UI translation (English → Other languages)
            return await TranslateForDisplay(text, toLanguage);
        }

        // ⭐ Convert Gujarati/Hindi → English
        public async Task<string> ToEnglish(string? text, string fromLanguage = "")
        {
            if (string.IsNullOrWhiteSpace(text)) return "";
            if (fromLanguage == "en") return text; // ⚡ Optimization: Skip API if input is already English

            return await Translate(text, "en");
        }

        // 🔥 1) Transliteration (GU/HINDI → ENGLISH FOR DATABASE)
        private async Task<string> Transliterate(string? text, string language, string fromScript, string toScript)
        {
            if (string.IsNullOrWhiteSpace(text)) return "";
            try
            {
                string url = $"{_endpoint}/transliterate?api-version=3.0&language={language}&fromScript={fromScript}&toScript={toScript}";
                var requestBody = JsonSerializer.Serialize(new[] { new { Text = text } });

                var response = await _httpClient.PostAsync(url,
                    new StringContent(requestBody, Encoding.UTF8, "application/json"));

                var json = await response.Content.ReadAsStringAsync();
                using var result = JsonDocument.Parse(json);

                return result.RootElement[0].GetProperty("text").GetString()!;
            }
            catch
            {
                return text; // fallback if API fails
            }
        }

        // 🔥 2) Translation for display (English → Gujarati/Hindi for USER UI)
        private async Task<string> TranslateForDisplay(string? text, string toLanguage)
        {
            if (string.IsNullOrWhiteSpace(text)) return "";

            var url = $"{_endpoint}/translate?api-version=3.0&to={toLanguage}";
            var body = JsonSerializer.Serialize(new[] { new { Text = text } });

            var response = await _httpClient.PostAsync(
                url,
                new StringContent(body, Encoding.UTF8, "application/json"));

            var json = await response.Content.ReadAsStringAsync();
            using var doc = JsonDocument.Parse(json);

            // 🟢 CASE 1: Normal ARRAY response
            if (doc.RootElement.ValueKind == JsonValueKind.Array)
            {
                return doc.RootElement[0]
                    .GetProperty("translations")[0]
                    .GetProperty("text")
                    .GetString()!;
            }

            // 🟡 CASE 2: OBJECT response
            // 🔴 Fallback safety
            return TranslateObject(doc.RootElement, text);
        }

        private string TranslateObject(JsonElement rootElement, string? text)
        {
            // Case 2: OBJECT response
            if (rootElement.ValueKind == JsonValueKind.Object &&
                rootElement.TryGetProperty("translations", out var translations))
            {
                return translations[0]
                    .GetProperty("text")
                    .GetString()!;
            }

            return text ?? "";
        }

        public async Task<string> TransliterateToNative(string? text, string toLanguage)
        {
            if (string.IsNullOrWhiteSpace(text) || toLanguage == "en")
                return text ?? "";

            // Map language code to script
            string? toScript = toLanguage switch
            {
                "gu" => "Gujr", // Gujarati
                "hi" => "Deva", // Devanagari (Hindi)
                _ => null
            };

            if (toScript == null)
                return text; // Fallback for unsupported scripts

            // Transliterate from Latin (English) to Native Script
            // We tell the API: "This text is in {toLanguage} but written in Latin script"
            return await Transliterate(text, toLanguage, "Latn", toScript);
        }

        // 🔥 3) REVERSE Transliteration (Native → English for Database)
        public async Task<string> TransliterateToEnglish(string? text, string fromLanguage)
        {
            if (string.IsNullOrWhiteSpace(text) || fromLanguage == "en")
                return text ?? "";

            string? fromScript = fromLanguage switch
            {
                "gu" => "Gujr",
                "hi" => "Deva",
                _ => null
            };

            if (fromScript == null)
                return await Translate(text, "en"); // Fallback to translation if script unknown

            // Convert Native Script → Latin (English)
            return await Transliterate(text, fromLanguage, fromScript, "Latn");
        }
    }
}