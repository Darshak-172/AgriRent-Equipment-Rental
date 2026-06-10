using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AgriRent.Migrations
{
    /// <inheritdoc />
    public partial class UpdateComplaintAddTargetAndResolution : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<int>(
                name: "TargetId",
                table: "Complaints",
                type: "int",
                nullable: false,
                defaultValue: 0);

            migrationBuilder.AddColumn<string>(
                name: "TargetType",
                table: "Complaints",
                type: "nvarchar(20)",
                maxLength: 20,
                nullable: false,
                defaultValue: "Equipment");

            migrationBuilder.AddColumn<string>(
                name: "ResolutionNote",
                table: "Complaints",
                type: "nvarchar(1000)",
                maxLength: 1000,
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "ResolvedByUserId",
                table: "Complaints",
                type: "int",
                nullable: true);

            migrationBuilder.AddColumn<DateTime>(
                name: "ResolvedAt",
                table: "Complaints",
                type: "datetime2",
                nullable: true);

            migrationBuilder.AlterColumn<string>(
                name: "Description",
                table: "Complaints",
                type: "nvarchar(1000)",
                maxLength: 1000,
                nullable: false,
                oldClrType: typeof(string),
                oldType: "nvarchar(500)",
                oldMaxLength: 500);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(name: "TargetId",         table: "Complaints");
            migrationBuilder.DropColumn(name: "TargetType",       table: "Complaints");
            migrationBuilder.DropColumn(name: "ResolutionNote",   table: "Complaints");
            migrationBuilder.DropColumn(name: "ResolvedByUserId", table: "Complaints");
            migrationBuilder.DropColumn(name: "ResolvedAt",       table: "Complaints");

            migrationBuilder.AlterColumn<string>(
                name: "Description",
                table: "Complaints",
                type: "nvarchar(500)",
                maxLength: 500,
                nullable: false,
                oldClrType: typeof(string),
                oldType: "nvarchar(1000)",
                oldMaxLength: 1000);
        }
    }
}
