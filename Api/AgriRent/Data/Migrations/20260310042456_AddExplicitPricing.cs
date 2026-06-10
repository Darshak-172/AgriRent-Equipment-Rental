using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AgriRent.Data.Migrations
{
    /// <inheritdoc />
    public partial class AddExplicitPricing : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "PriceType",
                table: "Equipments");

            migrationBuilder.RenameColumn(
                name: "Price",
                table: "Equipments",
                newName: "HourlyPrice");

            migrationBuilder.AddColumn<decimal>(
                name: "DailyPrice",
                table: "Equipments",
                type: "decimal(18,2)",
                nullable: false,
                defaultValue: 0m);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "DailyPrice",
                table: "Equipments");

            migrationBuilder.RenameColumn(
                name: "HourlyPrice",
                table: "Equipments",
                newName: "Price");

            migrationBuilder.AddColumn<string>(
                name: "PriceType",
                table: "Equipments",
                type: "nvarchar(20)",
                maxLength: 20,
                nullable: false,
                defaultValue: "");
        }
    }
}
