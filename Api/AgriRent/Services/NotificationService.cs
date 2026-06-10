using FirebaseAdmin;
using FirebaseAdmin.Messaging;
using Microsoft.EntityFrameworkCore;
using AgriRent.Data;

namespace AgriRent.Services
{
    public class NotificationService
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translator;

        public NotificationService(AppDbContext context, TranslateService translator)
        {
            _context = context;
            _translator = translator;
        }

        /// <summary>
        /// Sends FCM push in the user's preferred language.
        /// </summary>
        public async Task SendNotificationAsync(int userId, string title, string body, string type = "General")
        {
            // Look up user's preferred language
            var user = await _context.Users.AsNoTracking()
                .FirstOrDefaultAsync(u => u.UserId == userId);

            if (user == null) return;

            string lang = user.PreferredLanguage ?? "en";

            // Translate title and body if not English
            string translatedTitle = lang == "en" ? title : await _translator.Translate(title, lang);
            string translatedBody  = lang == "en" ? body  : await _translator.Translate(body, lang);

            await SendPushNotificationAsync(user, translatedTitle, translatedBody);
        }

        private async Task SendPushNotificationAsync(AgriRent.Models.User user, string title, string body)
        {
            try
            {
                if (FirebaseApp.DefaultInstance == null)
                {
                    Console.WriteLine("[FCM] FirebaseApp not initialized.");
                    return;
                }

                if (string.IsNullOrEmpty(user.FcmToken))
                {
                    Console.WriteLine($"[FCM] No FCM token for User {user.UserId}.");
                    return;
                }

                var message = new Message()
                {
                    Token = user.FcmToken,
                    Notification = new FirebaseAdmin.Messaging.Notification()
                    {
                        Title = title,
                        Body = body
                    },
                    Android = new AndroidConfig
                    {
                        Priority = Priority.High,
                        Notification = new AndroidNotification
                        {
                            Sound = "default",
                            ChannelId = "agrirent_notifications"
                        }
                    }
                };

                string response = await FirebaseMessaging.DefaultInstance.SendAsync(message);
                Console.WriteLine($"[FCM] ✅ Sent to User {user.UserId}: {response}");
            }
            catch (FirebaseMessagingException fex) when (fex.MessagingErrorCode == MessagingErrorCode.Unregistered
                                                       || fex.MessagingErrorCode == MessagingErrorCode.InvalidArgument)
            {
                Console.WriteLine($"[FCM] Stale token for User {user.UserId}. Clearing.");
                var u = await _context.Users.FirstOrDefaultAsync(x => x.UserId == user.UserId);
                if (u != null) { u.FcmToken = null; await _context.SaveChangesAsync(); }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[FCM] Error sending to User {user.UserId}: {ex.Message}");
            }
        }
    }
}
