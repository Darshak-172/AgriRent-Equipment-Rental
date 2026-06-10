using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AgriRent.Data.Migrations
{
    /// <inheritdoc />
    public partial class AddPerformanceIndexes : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_EquipmentBookings_EquipmentId",
                table: "EquipmentBookings");

            migrationBuilder.AlterColumn<string>(
                name: "Status",
                table: "EquipmentBookings",
                type: "nvarchar(450)",
                nullable: false,
                oldClrType: typeof(string),
                oldType: "nvarchar(max)");

            migrationBuilder.CreateIndex(
                name: "IX_EquipmentBookings_EquipmentId_Status",
                table: "EquipmentBookings",
                columns: new[] { "EquipmentId", "Status" });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_EquipmentBookings_EquipmentId_Status",
                table: "EquipmentBookings");

            migrationBuilder.AlterColumn<string>(
                name: "Status",
                table: "EquipmentBookings",
                type: "nvarchar(max)",
                nullable: false,
                oldClrType: typeof(string),
                oldType: "nvarchar(450)");

            migrationBuilder.CreateIndex(
                name: "IX_EquipmentBookings_EquipmentId",
                table: "EquipmentBookings",
                column: "EquipmentId");
        }
    }
}
