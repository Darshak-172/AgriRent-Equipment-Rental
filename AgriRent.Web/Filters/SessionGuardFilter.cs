using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;

namespace AgriRent.Web.Filters
{
    public class SessionGuardFilter : IActionFilter
    {
        public void OnActionExecuting(ActionExecutingContext context)
        {
            var userId = context.HttpContext.Session.GetString("UserId");
            var path = context.HttpContext.Request.Path.Value?.ToLower() ?? "";

            // If user is logged in and trying to access Login or Register pages
            if (!string.IsNullOrEmpty(userId))
            {
                if (path.Contains("/account/login") || 
                    path.Contains("/account/register") || 
                    path.Contains("/account/forgotpassword"))
                {
                    context.Result = new RedirectToActionResult("Index", "Home", null);
                }
            }
        }

        public void OnActionExecuted(ActionExecutedContext context)
        {
        }
    }
}
