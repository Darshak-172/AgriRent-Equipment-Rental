using AgriRent.DTOs;
using AgriRent.Services;

namespace AgriRent.Extensions
{
    public static class TranslationExtensions
    {
        /// <summary>
        /// Translates a SearchResultDto to the target language
        /// </summary>
        public static async Task<SearchResultDto> TranslateAsync(this SearchResultDto dto, TranslateService translator, string language)
        {
            if (language == "en") return dto; // Already in English

            return new SearchResultDto
            {
                EquipmentId = dto.EquipmentId,
                Name = await translator.Translate(dto.Name, language),
                Description = await translator.Translate(dto.Description, language),
                Price = dto.Price,
                HourlyPrice = dto.HourlyPrice,
                DailyPrice = dto.DailyPrice,
                PriceType = dto.PriceType,
                Location = dto.Location, // Don't translate location names
                ImageUrl = dto.ImageUrl,
                Type = await translator.Translate(dto.Type, language),
                CategoryName = await translator.Translate(dto.CategoryName, language),
                Status = dto.Status
            };
        }

        /// <summary>
        /// Translates a list of SearchResultDto to the target language
        /// </summary>
        public static async Task<List<SearchResultDto>> TranslateAsync(this List<SearchResultDto> dtos, TranslateService translator, string language)
        {
            if (language == "en") return dtos; // Already in English

            var translatedList = new List<SearchResultDto>();
            foreach (var dto in dtos)
            {
                translatedList.Add(await dto.TranslateAsync(translator, language));
            }
            return translatedList;
        }

        /// <summary>
        /// Translates equipment/product name and description
        /// </summary>
        public static async Task<T> TranslateFieldsAsync<T>(this T obj, TranslateService translator, string language, params string[] fields) where T : class
        {
            if (language == "en") return obj;

            var type = typeof(T);
            foreach (var fieldName in fields)
            {
                var property = type.GetProperty(fieldName);
                if (property != null && property.PropertyType == typeof(string))
                {
                    var value = property.GetValue(obj) as string;
                    if (!string.IsNullOrEmpty(value))
                    {
                        var translated = await translator.Translate(value, language);
                        property.SetValue(obj, translated);
                    }
                }
            }
            return obj;
        }
    }
}
