using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AgriRent.Migrations
{
    /// <inheritdoc />
    public partial class RenameApprovedStatusToActive : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // Rename "Approved" → "Active" in Equipment and Product tables
            // to support the new unified status model:
            // Pending | Active (admin-approved, publicly visible) | Paused (auto-hidden) | Rejected
            migrationBuilder.Sql("UPDATE Equipments SET Status = 'Active' WHERE Status = 'Approved'");
            migrationBuilder.Sql("UPDATE Products   SET Status = 'Active' WHERE Status = 'Approved'");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("UPDATE Equipments SET Status = 'Approved' WHERE Status = 'Active'");
            migrationBuilder.Sql("UPDATE Products   SET Status = 'Approved' WHERE Status = 'Active'");
        }
    }
}
