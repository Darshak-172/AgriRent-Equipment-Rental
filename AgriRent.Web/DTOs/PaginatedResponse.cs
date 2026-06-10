namespace AgriRent.Web.DTOs
{
    public class PaginatedResponse<T>
    {
        public List<T> Data { get; set; } = new();
        public PaginationInfo Pagination { get; set; } = new();
    }

    public class PaginationInfo
    {
        public int Page { get; set; }
        public int PageSize { get; set; }
        public int TotalItems { get; set; }
        public int TotalPages { get; set; }
        public bool HasNextPage { get; set; }
        public bool HasPreviousPage { get; set; }
    }
}
