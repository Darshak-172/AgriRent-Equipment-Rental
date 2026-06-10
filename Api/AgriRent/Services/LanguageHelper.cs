using Microsoft.AspNetCore.Mvc;

namespace AgriRent.Services
{
    public class LanguageHelper
    {
        private readonly IHttpContextAccessor _httpContextAccessor;

        public LanguageHelper(IHttpContextAccessor httpContextAccessor)
        {
            _httpContextAccessor = httpContextAccessor;
        }

        /// <summary>
        /// Gets the language from Accept-Language header
        /// Supports: en (English), gu (Gujarati), hi (Hindi)
        /// Defaults to "en" if not specified
        /// </summary>
        public string GetLanguage()
        {
            var context = _httpContextAccessor.HttpContext;
            if (context == null) return "en";

            // Read Accept-Language header
            var acceptLanguage = context.Request.Headers["Accept-Language"].ToString();
            
            if (string.IsNullOrWhiteSpace(acceptLanguage))
                return "en";

            // Parse language code (e.g., "gu", "hi", "en-US" -> "en")
            var language = acceptLanguage.Split(',')[0].Split('-')[0].Trim().ToLower();

            // Validate supported languages
            return language switch
            {
                "gu" => "gu",  // Gujarati
                "hi" => "hi",  // Hindi
                "en" => "en",  // English
                _ => "en"      // Default to English
            };
        }

        /// <summary>
        /// Checks if current language is English
        /// </summary>
        public bool IsEnglish() => GetLanguage() == "en";

        /// <summary>
        /// Checks if current language is Gujarati
        /// </summary>
        public bool IsGujarati() => GetLanguage() == "gu";

        /// <summary>
        /// Checks if current language is Hindi
        /// </summary>
        public bool IsHindi() => GetLanguage() == "hi";
    }
}
