namespace AgriRent.Extensions
{
    /// <summary>
    /// Helper to get current Indian Standard Time (IST = UTC+05:30).
    /// Use IstHelper.Now instead of DateTime.UtcNow wherever you want
    /// timestamps stored in the database to reflect IST.
    /// </summary>
    public static class IstHelper
    {
        private static readonly TimeZoneInfo Ist =
            TimeZoneInfo.FindSystemTimeZoneById("India Standard Time");

        /// <summary>Returns the current date and time in IST (UTC+05:30).</summary>
        public static DateTime Now => TimeZoneInfo.ConvertTimeFromUtc(DateTime.UtcNow, Ist);
    }
}
