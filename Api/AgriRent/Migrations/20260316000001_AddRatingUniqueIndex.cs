using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AgriRent.Migrations
{
    /// <inheritdoc />
    public partial class AddRatingUniqueIndex : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateIndex(
                name: "IX_Ratings_FromUserId_TargetId_RatingType",
                table: "Ratings",
                columns: new[] { "FromUserId", "TargetId", "RatingType" },
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_Ratings_FromUserId_TargetId_RatingType",
                table: "Ratings");
        }
    }
}
