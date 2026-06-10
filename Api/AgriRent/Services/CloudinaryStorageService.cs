using CloudinaryDotNet;
using CloudinaryDotNet.Actions;

namespace AgriRent.Services;

public class CloudinaryStorageService
{
    private readonly Cloudinary? _cloudinary;
    private readonly string? _configError;

    public CloudinaryStorageService(IConfiguration configuration)
    {
        var cloudName = configuration["Cloudinary:CloudName"] ?? configuration["Cloudinary__CloudName"];
        var apiKey = configuration["Cloudinary:ApiKey"] ?? configuration["Cloudinary__ApiKey"];
        var apiSecret = configuration["Cloudinary:ApiSecret"] ?? configuration["Cloudinary__ApiSecret"];

        if (string.IsNullOrEmpty(cloudName) || string.IsNullOrEmpty(apiKey) || string.IsNullOrEmpty(apiSecret))
        {
            _configError = "Cloudinary configuration is missing. Please set Cloudinary:CloudName, ApiKey, and ApiSecret in appsettings.json or Azure environment variables.";
            return;
        }

        var account = new Account(cloudName, apiKey, apiSecret);
        _cloudinary = new Cloudinary(account);
        _cloudinary.Api.Secure = true;
    }

    /// <summary>
    /// Upload an image file to Cloudinary
    /// </summary>
    /// <param name="file">The image file to upload</param>
    /// <param name="folder">Folder name (e.g., "equipment", "product")</param>
    /// <returns>The public URL of the uploaded image</returns>
    public async Task<string> UploadImageAsync(IFormFile file, string folder)
    {
        if (_cloudinary == null)
            throw new InvalidOperationException(_configError ?? "Cloudinary is not initialized.");

        // Validate file

        // Validate file type
        var allowedExtensions = new[] { ".jpg", ".jpeg", ".png", ".webp" };
        var fileExtension = Path.GetExtension(file.FileName).ToLower();
        if (!allowedExtensions.Contains(fileExtension))
            throw new ArgumentException($"File type not allowed. Supported types: {string.Join(", ", allowedExtensions)}");

        // Validate file size (max 5MB)
        const long maxFileSize = 5 * 1024 * 1024; // 5MB
        if (file.Length > maxFileSize)
            throw new ArgumentException("File size exceeds 5MB limit");

        var timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        var randomString = Guid.NewGuid().ToString("N").Substring(0, 8);
        var publicId = $"{folder}/{timestamp}_{randomString}";

        using var stream = file.OpenReadStream();
        var uploadParams = new ImageUploadParams()
        {
            File = new FileDescription(file.FileName, stream),
            PublicId = publicId,
            AssetFolder = folder,
            UseFilename = false,
            UniqueFilename = false,
            Overwrite = true
        };

        var uploadResult = await _cloudinary.UploadAsync(uploadParams);

        if (uploadResult.Error != null)
        {
            throw new Exception($"Cloudinary upload failed: {uploadResult.Error.Message}");
        }

        return uploadResult.SecureUrl.ToString();
    }

    /// <summary>
    /// Delete an image from Cloudinary
    /// </summary>
    /// <param name="imageUrl">The full URL of the image</param>
    public async Task<bool> DeleteImageAsync(string imageUrl)
    {
        try
        {
            if (_cloudinary == null)
                return false;

            if (string.IsNullOrWhiteSpace(imageUrl))
                return false;

            // Extract public ID from Cloudinary URL
            // Format: https://res.cloudinary.com/<cloud>/image/upload/v<version>/<folder>/<file>.<ext>
            var uri = new Uri(imageUrl);
            var pathSegments = uri.AbsolutePath.Split('/', StringSplitOptions.RemoveEmptyEntries);

            // Find "upload" segment index
            var uploadIndex = Array.IndexOf(pathSegments, "upload");
            if (uploadIndex == -1 || uploadIndex + 1 >= pathSegments.Length)
                return false;

            // Skip optional version segment ("v1234567890")
            int startIndex = uploadIndex + 1;
            if (startIndex < pathSegments.Length
                && pathSegments[startIndex].StartsWith("v", StringComparison.OrdinalIgnoreCase)
                && pathSegments[startIndex].Length > 1
                && char.IsDigit(pathSegments[startIndex][1]))
            {
                startIndex++;
            }

            if (startIndex >= pathSegments.Length)
                return false;

            // Join remaining segments as the public ID (excluding file extension)
            var publicIdWithExt = string.Join("/", pathSegments.Skip(startIndex));

            // Strip the file extension using string operations (Path.ChangeExtension is OS-path-aware and unreliable with forward slashes)
            var lastDotIndex = publicIdWithExt.LastIndexOf('.');
            var lastSlashIndex = publicIdWithExt.LastIndexOf('/');
            string publicId;
            if (lastDotIndex > lastSlashIndex && lastDotIndex >= 0)
            {
                publicId = publicIdWithExt[..lastDotIndex];
            }
            else
            {
                publicId = publicIdWithExt;
            }

            var deletionParams = new DeletionParams(publicId)
            {
                ResourceType = ResourceType.Image
            };

            var deletionResult = await _cloudinary.DestroyAsync(deletionParams);

            return deletionResult.Result == "ok";
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    /// Validate if file is a valid image
    /// </summary>
    public bool IsValidImage(IFormFile file)
    {
        if (file == null || file.Length == 0)
            return false;

        var allowedExtensions = new[] { ".jpg", ".jpeg", ".png", ".webp" };
        var fileExtension = Path.GetExtension(file.FileName).ToLower();
        
        if (!allowedExtensions.Contains(fileExtension))
            return false;

        const long maxFileSize = 5 * 1024 * 1024; // 5MB
        if (file.Length > maxFileSize)
            return false;

        return true;
    }
}
