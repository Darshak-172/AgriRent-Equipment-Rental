using Microsoft.Extensions.Caching.Memory;

namespace AgriRent.Web.Services
{
    public interface ICacheService
    {
        T? Get<T>(string key);
        void Set<T>(string key, T value, TimeSpan? duration = null);
        void Remove(string key);
    }

    public class CacheService : ICacheService
    {
        private readonly IMemoryCache _cache;
        private static readonly TimeSpan DefaultDuration = TimeSpan.FromMinutes(30);

        public CacheService(IMemoryCache cache)
        {
            _cache = cache;
        }

        public T? Get<T>(string key)
        {
            return _cache.TryGetValue(key, out T? value) ? value : default;
        }

        public void Set<T>(string key, T value, TimeSpan? duration = null)
        {
            _cache.Set(key, value, duration ?? DefaultDuration);
        }

        public void Remove(string key)
        {
            _cache.Remove(key);
        }
    }
}
