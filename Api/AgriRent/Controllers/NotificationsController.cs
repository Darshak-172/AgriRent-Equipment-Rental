using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using AgriRent.Data;
using System.Security.Claims;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/notifications")]
    [Authorize]
    public class NotificationsController : ControllerBase
    {
        private readonly AppDbContext _context;

        public NotificationsController(AppDbContext context)
        {
            _context = context;
        }

        private int GetUserId() =>
            int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

        // ──────────────────────────────────────────────
        // GET /api/notifications
        // Returns paginated notifications for logged-in user
        // ──────────────────────────────────────────────
        [HttpGet]
        public async Task<IActionResult> GetNotifications(
            [FromQuery] int page = 1,
            [FromQuery] int pageSize = 20)
        {
            if (page < 1) page = 1;
            if (pageSize < 1 || pageSize > 100) pageSize = 20;

            var userId = GetUserId();

            var query = _context.Notifications
                .Where(n => n.UserId == userId)
                .OrderByDescending(n => n.CreatedAt);

            var total = await query.CountAsync();

            var items = await query
                .Skip((page - 1) * pageSize)
                .Take(pageSize)
                .Select(n => new
                {
                    n.Id,
                    n.Title,
                    n.Message,
                    n.Type,
                    n.IsRead,
                    n.CreatedAt
                })
                .ToListAsync();

            return Ok(new
            {
                data = items,
                pagination = new
                {
                    page,
                    pageSize,
                    totalItems = total,
                    totalPages = (int)Math.Ceiling(total / (double)pageSize)
                },
                unreadCount = await _context.Notifications
                    .CountAsync(n => n.UserId == userId && !n.IsRead)
            });
        }

        // ──────────────────────────────────────────────
        // GET /api/notifications/unread-count
        // Returns unread notification count for badge
        // ──────────────────────────────────────────────
        [HttpGet("unread-count")]
        public async Task<IActionResult> GetUnreadCount()
        {
            var userId = GetUserId();
            var count = await _context.Notifications
                .CountAsync(n => n.UserId == userId && !n.IsRead);
            return Ok(new { unreadCount = count });
        }

        // ──────────────────────────────────────────────
        // PUT /api/notifications/{id}/read
        // Mark a single notification as read
        // ──────────────────────────────────────────────
        [HttpPut("{id}/read")]
        public async Task<IActionResult> MarkAsRead(int id)
        {
            var userId = GetUserId();
            var notification = await _context.Notifications
                .FirstOrDefaultAsync(n => n.Id == id && n.UserId == userId);

            if (notification == null)
                return NotFound(new { message = "Notification not found." });

            notification.IsRead = true;
            await _context.SaveChangesAsync();
            return Ok(new { message = "Marked as read." });
        }

        // ──────────────────────────────────────────────
        // PUT /api/notifications/read-all
        // Mark all notifications as read
        // ──────────────────────────────────────────────
        [HttpPut("read-all")]
        public async Task<IActionResult> MarkAllAsRead()
        {
            var userId = GetUserId();
            await _context.Notifications
                .Where(n => n.UserId == userId && !n.IsRead)
                .ExecuteUpdateAsync(s => s.SetProperty(n => n.IsRead, true));

            return Ok(new { message = "All notifications marked as read." });
        }

        // ──────────────────────────────────────────────
        // DELETE /api/notifications/{id}
        // Delete a single notification
        // ──────────────────────────────────────────────
        [HttpDelete("{id}")]
        public async Task<IActionResult> DeleteNotification(int id)
        {
            var userId = GetUserId();
            var notification = await _context.Notifications
                .FirstOrDefaultAsync(n => n.Id == id && n.UserId == userId);

            if (notification == null)
                return NotFound(new { message = "Notification not found." });

            _context.Notifications.Remove(notification);
            await _context.SaveChangesAsync();
            return Ok(new { message = "Notification deleted." });
        }
    }
}
