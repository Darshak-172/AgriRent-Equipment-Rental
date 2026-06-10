using AgriRent.Data;

namespace AgriRent.Services
{
    public class SubscriptionExpiryBackgroundService : BackgroundService
    {
        private readonly IServiceProvider _serviceProvider;
        private readonly ILogger<SubscriptionExpiryBackgroundService> _logger;

        public SubscriptionExpiryBackgroundService(
            IServiceProvider serviceProvider,
            ILogger<SubscriptionExpiryBackgroundService> logger)
        {
            _serviceProvider = serviceProvider;
            _logger = logger;
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Subscription Expiry Background Service started.");

            while (!stoppingToken.IsCancellationRequested)
            {
                try
                {
                    using (var scope = _serviceProvider.CreateScope())
                    {
                        var expiryService = scope.ServiceProvider.GetRequiredService<SubscriptionExpiryService>();
                        
                        var expiredCount = await expiryService.DeactivateExpiredSubscriptionsAsync();
                        
                        if (expiredCount > 0)
                        {
                            _logger.LogInformation($"Deactivated {expiredCount} expired subscriptions at {DateTime.UtcNow}");
                        }
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Error occurred while checking expired subscriptions.");
                    System.IO.File.WriteAllText("expiry_error_log.txt", ex.ToString());
                }

                // Run every hour
                await Task.Delay(TimeSpan.FromHours(1), stoppingToken);
            }

            _logger.LogInformation("Subscription Expiry Background Service stopped.");
        }
    }
}
