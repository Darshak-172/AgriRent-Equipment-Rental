using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AgriRent.Migrations
{
    /// <inheritdoc />
    public partial class RemoveIconClassFromCategory : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "IconClass",
                table: "Categories");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "IconClass",
                table: "Categories",
                type: "nvarchar(50)",
                maxLength: 50,
                nullable: true);
        }
    }
}
